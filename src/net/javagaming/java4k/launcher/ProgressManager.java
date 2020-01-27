/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to &lt;http://unlicense.org/
 */
package net.javagaming.java4k.launcher;

import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.cache.ResourceListener;
import net.javagaming.java4k.launcher.progress.ProgressController;
import net.javagaming.java4k.launcher.progress.ProgressState;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Central location for managing the workers.
 *
 * @author Groboclown
 */
public class ProgressManager {
    private final ProgressPanel main;
    private final LauncherManager launcherManager;
    private final ExecutorService downloadService =
            Executors.newSingleThreadExecutor();
    private volatile int pendingDownload = 0;


    public ProgressManager(ProgressPanel progressPanel,
            LauncherManager launcherManager) {
        this.main = progressPanel;
        this.launcherManager = launcherManager;
    }

    public void shutdownManager() {
        waitForWorkers();
        downloadService.shutdownNow();
        try {
            downloadService.awaitTermination(5L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.err.println("Did not stop download in time");
        }
    }


    public int getPendingDownloads() {
        return pendingDownload;
    }


    /**
     * Request a download of the given resource.  The listener will have
     * {@link net.javagaming.java4k.launcher.cache.ResourceListener#resourceLoaded(net.javagaming.java4k.launcher.cache.Resource)}
     * or {@link net.javagaming.java4k.launcher.cache.ResourceListener#resourceDownloadError(net.javagaming.java4k.launcher.cache.Resource, java.io.IOException)}
     * called (unless the program is terminated before the download can begin).
     * The {@link net.javagaming.java4k.launcher.cache.ResourceListener#resourceDownloadStarted(net.javagaming.java4k.launcher.cache.Resource)}
     * will only be called if the resource requires a download.
     * <p/>
     * Because the {@link net.javagaming.java4k.launcher.cache.Resource#isAvailable()}
     * method can potentially be an expensive operation, the listener will
     * always be called in a separate (non-EDT) thread.
     *
     * @param resource resource to download
     * @param listener callback on resource loading events
     * @param flush true if the resource should be flushed before starting
     */
    public void download(Resource resource, ResourceListener listener,
            boolean flush) {
        if (resource == null || listener == null) {
            throw new NullPointerException();
        }
        downloadService.submit(new Downloader(resource, listener, flush));
    }


    public <V> void startWorker(final String name, final ActionSource action,
            boolean canCancel, ProgressWorker<V> worker) {
        ProgressController controller = main.createWorkerController(
                name, canCancel);
        ProgressSwingWorker<V> swingWorker = new ProgressSwingWorker<V>(
                name, action, worker, controller);

        // FIXME cache worker for waitForWorkers call.

        swingWorker.execute();
    }


    public void startEDT(final String name, final ActionSource action,
            final Runnable r) {
        if (SwingUtilities.isEventDispatchThread()) {
            runEDT(name, action, r);
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    runEDT(name, action, r);
                }
            });
        }

    }





    public void waitForWorkers() {
        // FIXME
    }



    private void runEDT(String name, ActionSource action, Runnable r) {
        try {
            r.run();
        } catch (VirtualMachineError e) {
            throw e;
        } catch (ThreadDeath e) {
            throw e;
        } catch (Throwable e) {
            launcherManager.getErrorMessageManager().gameError(name, e, action);
        }
    }



    class ProgressSwingWorker<V> extends SwingWorker<Void, V> {
        private final String name;
        private final ActionSource action;
        private final ProgressWorker<V> worker;
        private final ProxyProgressController<V> proxyController;

        ProgressSwingWorker(String name, ActionSource action,
                ProgressWorker<V> worker,
                ProgressController workerController) {
            this.name = name;
            this.action = action;
            this.worker = worker;
            this.proxyController = new ProxyProgressController<V>(
                    this, workerController);
        }


        void publishValues(V... values) {
            publish(values);
        }


        @Override
        protected Void doInBackground() {
            try {
                worker.doInBackground(proxyController);
            } catch (Exception e) {
                launcherManager.getErrorMessageManager().gameError(
                    name, e, action);
            }
            return null;
        }


        @Override
        protected void process(List<V> chunks) {
            try {
                worker.process(chunks);
            } catch (Exception e) {
                launcherManager.getErrorMessageManager().gameError(
                    name, e, action);
            }
        }


        @Override
        public void done() {
            try {
                worker.done(proxyController);
            } catch (Exception e) {
                launcherManager.getErrorMessageManager().gameError(
                    name, e, action);
            }
            proxyController.completed();
        }
    }



    class ProxyProgressController<V> implements ProgressWorker.Publisher<V> {
        private final ProgressSwingWorker<V> worker;
        private final ProgressController proxy;


        ProxyProgressController(ProgressSwingWorker<V> worker,
                ProgressController proxy) {
            this.worker = worker;
            this.proxy = proxy;
        }

        @Override
        public void advance(String labelMsg, Object... params) {
            proxy.advance(labelMsg, params);
        }

        @Override
        public void advance(int min, int max, int value) {
            proxy.advance(min, max, value);
        }

        @Override
        public void advanceBy(int valueIncr) {
            proxy.advanceBy(valueIncr);
        }

        @Override
        public void setProgressState(ProgressState state) {
            proxy.setProgressState(state);
        }

        @Override
        public void advanceTo(int value) {
            proxy.advanceTo(value);
        }

        @Override
        public void completed() {
            proxy.completed();
        }

        @Override
        public boolean isCancelled() {
            return proxy.isCancelled();
        }

        @Override
        public ProgressController createChild(int parentRange) {
            return proxy.createChild(parentRange);
        }

        @Override
        public void publish(V... values) {
            worker.publishValues(values);
        }
    }



    class Downloader implements Runnable {
        private final Resource resource;
        private final ResourceListener listener;
        private final boolean flush;

        Downloader(Resource resource, ResourceListener listener, boolean flush) {
            this.resource = resource;
            this.listener = listener;
            this.flush = flush;
        }


        @Override
        public void run() {
            if (flush) {
                resource.flush();
            }
            try {
                listener.resourceDownloadStarted(resource);
                if (! resource.isAvailable()) {
                    try {
                        resource.read().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                        listener.resourceDownloadError(resource, e);
                        return;
                    }
                }
                listener.resourceLoaded(resource);
            } catch (ThreadDeath e) {
                throw e;
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable e) {
                launcherManager.getErrorMessageManager().gameError(
                        resource.toString(), e, ActionSource.GAME_DOWNLOAD);
            }
        }
    }
}

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
package net.javagaming.java4k.launcher.ui;

import net.javagaming.java4k.launcher.ActionSource;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.cache.ResourceListener;
import net.javagaming.java4k.launcher.progress.ProgressController;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.LayoutManager;
import java.io.IOException;
import java.net.URL;
import java.util.List;

/**
 * A panel that processes resources in the correct threads.
 *
 * @param <V> the resource processing publish type.
 * @author Groboclown
 */
public abstract class AbstractResourcePanel<V> extends JPanel {
    private static final int ICON_SIZE = 16;

    private Icon previousIcon;

    private final LauncherManager launcherManager;
    private Resource resource;
    private JLabel status;
    private final Icon loadingIcon;
    private final Icon notAvailableIcon;
    private final Icon usingCachedIcon;
    private final Icon currentIcon;
    private volatile ResourceProcessWorker activeWorker;

    public AbstractResourcePanel(LauncherManager launcherManager) {
        this(launcherManager, new BorderLayout());
    }

    public AbstractResourcePanel(LauncherManager launcherManager,
            LayoutManager lm) {
        super(lm);
        this.launcherManager = launcherManager;
        //setPreferredSize(new Dimension(48, 48));
        status = new JLabel();
        add(status);

        notAvailableIcon = loadIcon("not-available.png");
        usingCachedIcon = loadIcon("cached-2.png");
        currentIcon = loadIcon("current.png");
        loadingIcon = new ProcessingIcon(status, ICON_SIZE);
        setNotAvailableIcon("not loaded");
    }


    public LauncherManager getLauncherManager() {
        return launcherManager;
    }


    /**
     *
     * @return the resource currently being used to refresh the data.  Do not
     *      use this to read the data!
     */
    public Resource getResource() {
        return resource;
    }


    /**
     * Forces a flush of the resource before loading.
     *
     * @param source type of action
     * @param r resource
     */
    public void reloadResource(ActionSource source, Resource r) {
        loadResource(source, r, false);
    }


    /**
     * Trigger the logic to load the resource.  It will update the status
     * icon based on the state, and trigger the action methods.
     *
     * @param source type of action
     * @param r resource
     */
    public void loadResource(ActionSource source, Resource r) {
        loadResource(source, r, true);
    }


    public void loadResource(ActionSource source, Resource r, boolean flash) {
        this.resource = r;

        setLoadingIcon();

        activeWorker = new ResourceProcessWorker(r, source);
        launcherManager.getWorkerManager().download(r, activeWorker, flash);
    }


    /**
     *
     * @return the component that displays the current status
     */
    public JComponent getStatusIconComponent() {
        return status;
    }


    private void setLoadingIcon() {
        setIcon(loadingIcon, LauncherBundle.message("download.loading",
                getResource().getURI().toString()));
    }


    private void setNotAvailableIcon(String problem) {
        setIcon(notAvailableIcon,
                LauncherBundle.message("download.not-available",
                        problem));
    }


    private void setUsingCacheIcon() {
        setIcon(usingCachedIcon, LauncherBundle.message("download.cached"));
    }


    private void setCurrentIcon() {
        setIcon(currentIcon, LauncherBundle.message("download.current"));
    }


    private void setIcon(final Icon icon, final String toolTip) {
        launcherManager.getWorkerManager().startEDT("set icon",
                ActionSource.GUI_UPDATE, new Runnable() {
            @Override
            public void run() {
                status.setIcon(icon);
                status.setToolTipText(toolTip);

                if (previousIcon != null &&
                        previousIcon instanceof AbstractAnimatedIcon) {
                    ((AbstractAnimatedIcon) previousIcon).stop();
                }
                if (icon != null &&
                        icon instanceof AbstractAnimatedIcon) {
                    ((AbstractAnimatedIcon) icon).start();
                }

                previousIcon = icon;
            }
        });
    }



    protected Image loadImage(String id, boolean scale) {
        URL url = AbstractResourcePanel.class.getResource(id);
        Image img = null;
        if (url != null) {
            try {
                img = ImageIO.read(url);
                if (scale) {
                    img = img.getScaledInstance(
                            (img.getWidth(null) * ICON_SIZE / img.getHeight(null)),
                            ICON_SIZE,
                            Image.SCALE_SMOOTH);
                }
            } catch (IOException e) {
                e.printStackTrace();
                img = null;
            }
        }
        return img;
    }


    protected Icon loadIcon(String id) {
        Image img = loadImage(id, true);
        if (img == null) {
            return null;
        } else {
            return new ImageIcon(img);
        }
    }



    /**
     * Called in a worker thread to process the data in the reader.
     *
     * @param r resource that was loaded
     * @param controller the controller to publish values to.
     * @throws Exception
     */
    protected abstract void processLoadedResource(Resource r,
            ProgressWorker.Publisher<V> controller) throws Exception;


    /**
     * Process the published values that were processed.  This is performed
     * in the EDT.
     *
     * @param values a (possible subset) of published values
     * @throws Exception
     */
    protected abstract void processPublished(List<V> values) throws Exception;

    /**
     * Called in the EDT when all the values published by
     * {@link #processLoadedResource(net.javagaming.java4k.launcher.cache.Resource, net.javagaming.java4k.launcher.progress.ProgressWorker.Publisher)}
     * have finished processing in
     * {@link #processPublished(java.util.List)}
     *
     * @throws Exception
     */
    protected abstract void onResourceProcessed() throws Exception;


    /**
     * Called in the EDT when the resource could not be loaded.  If this is
     * called, then none of the process methods are called.
     *
     * @param r resource
     * @param e source of the error
     * @throws Exception
     */
    protected abstract void onResourceLoadFailure(Resource r, IOException e)
            throws Exception;


    /**
     * Called when the resource load begins.  This will be invoked in a worker
     * thread.
     *
     * @param r resource
     */
    protected abstract void onResourceLoadStarted(Resource r);



    class ResourceProcessWorker extends ProgressWorker<V>
            implements ResourceListener, Runnable {
        private final Resource r;
        private int state;
        private IOException failure;
        private final ActionSource source;

        public ResourceProcessWorker(Resource r, ActionSource source) {
            this.r = r;
            this.source = source;
        }

        @Override
        public void doInBackground(Publisher<V> controller) throws Exception {
            // only operate if we're still the active worker.
            // Note that this is only ever called if the resource
            // is available to download.
            if (activeWorker == this && (state == 1 || state == 2)) {
                try {
                    processLoadedResource(r, controller);
                } catch (ThreadDeath t) {
                    throw t;
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable t) {
                    state = 3;
                    if (t instanceof IOException) {
                        failure = (IOException) t;
                    } else {
                        failure = new IOException(t);
                    }
                    setNotAvailableIcon("");
                    launcherManager.getErrorMessageManager().gameError(
                            resource.toString(), t, ActionSource.GUI_UPDATE);
                }
            }
        }


        @Override
        public void process(List<V> obj) throws Exception {
            // Note that this is only ever called if the resource
            // is available to download.
            if (activeWorker == this) {
                try {
                    processPublished(obj);
                } catch (ThreadDeath t) {
                    throw t;
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable t) {
                    state = 3;
                    if (t instanceof IOException) {
                        failure = (IOException) t;
                    } else {
                        failure = new IOException(t);
                    }
                    setNotAvailableIcon("");
                    launcherManager.getErrorMessageManager().gameError(
                            resource.toString(), t, ActionSource.GUI_UPDATE);
                }
            }
        }

        @Override
        public void done(ProgressController controller) throws Exception {
            // Note that this is only ever called if the resource
            // is available to download.
            if (activeWorker == this) {
                try {
                    onResourceProcessed();
                } catch (ThreadDeath t) {
                    throw t;
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable t) {
                    state = 3;
                    if (t instanceof IOException) {
                        failure = (IOException) t;
                    } else {
                        failure = new IOException(t);
                    }
                    launcherManager.getErrorMessageManager().gameError(
                            resource.toString(), t, ActionSource.GUI_UPDATE);
                } finally {
                    activeWorker = null;
                    if (state == 1) {
                        setCurrentIcon();
                    } else if (state == 2) {
                        setUsingCacheIcon();
                    } else {
                        setNotAvailableIcon("");
                    }
                }
            }
        }

        @Override
        public void resourceLoaded(Resource resource) {
            // keep the loading icon
            if (resource.isUpToDate()) {
                state = 1;
            } else {
                state = 2;
            }
            launcherManager.getWorkerManager().startWorker(
                    resource.toString(), source, false, this);
        }


        @Override
        public void resourceDownloadError(Resource resource,
                IOException reason) {
            state = 3;
            failure = reason;
            launcherManager.getWorkerManager().startEDT(
                    resource.toString(), source, this);
        }

        @Override
        public void run() {
            // called from the download error
            setNotAvailableIcon(failure.getMessage());
            try {
                onResourceLoadFailure(r, failure);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                activeWorker = null;
            }
        }

        @Override
        public void resourceDownloadStarted(Resource resource) {
            setLoadingIcon();
            onResourceLoadStarted(resource);
        }
    }

}

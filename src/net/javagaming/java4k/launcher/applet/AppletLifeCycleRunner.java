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
package net.javagaming.java4k.launcher.applet;

import net.javagaming.java4k.launcher.AbstractLifeCycleRunner;
import net.javagaming.java4k.launcher.ActionSource;
import net.javagaming.java4k.launcher.ChildProgressController;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.progress.ProgressController;

import javax.swing.JFrame;
import javax.swing.JPanel;
import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Manages the life cycle for an Applet.
 *
 * TODO perform correct cleanup and garbage collection of the applet object,
 * class, and classloader.
 *
 * @author Groboclown
 */
public class AppletLifeCycleRunner extends AbstractLifeCycleRunner<AppletConfiguration, Applet> {
    private JFrame frame = null;
    private AppletPanel appletPanel = null;
    private Stub stub;

    public AppletLifeCycleRunner(GameDetail description,
            AppletConfiguration appletConfig, LauncherManager launcherManager) {
        super(description, appletConfig, launcherManager);
    }



    @Override
    protected synchronized void setup(final Container parentContainer)
            throws IOException {
        final Applet applet = getConfig().loadApplet();
        setGameObj(applet);

        stub = new Stub();
        applet.setStub(stub);

        getLauncherManager().getWorkerManager().startEDT(
                getDetail().getName(),
                ActionSource.GAME_START, new Runnable() {
            @Override
            public void run() {
                applet.setPreferredSize(getConfig().getSize());
                appletPanel = new AppletPanel(applet);
                appletPanel.setPreferredSize(getConfig().getSize());

                if (parentContainer != null) {
                    parentContainer.add(appletPanel, BorderLayout.CENTER);
                    appletPanel.setVisible(true);
                    parentContainer.validate();

                    // This shouldn't be necessary
                    parentContainer.setVisible(true);
                } else {
                    frame = new JFrame(getDetail().getName());

                    // In case the user wants to resize the frame, default to
                    // the color of the Java4k site.
                    frame.setBackground(Color.BLACK);

                    frame.getContentPane().add(appletPanel);
                    if (getDetail().getIcon() != null) {
                        frame.setIconImage(getDetail().getIcon());
                    } else {
                        frame.setIconImage(
                                getLauncherManager().getMainFrame().
                                        getIconImage());
                    }
                    frame.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            destroyFromEvent();
                        }
                    });
                    frame.setLocationRelativeTo(getLauncherManager().getMainFrame());
                    frame.pack();
                    frame.setVisible(true);
                    frame.requestFocus();
                    applet.requestFocusInWindow();
                }
                appletPanel.initialize();
            }
        });
    }


    void activate(boolean wait) throws InterruptedException {
        if (appletPanel == null) {
            throw new IllegalStateException("wrong");
        }
        stub.isActive = true;
        runNewThread("init/start", new AppletInitStart(getGameObj()),
                getWaitTime(wait));
    }


    /**
     * Sends the stop event to the applet.
     *
     * @throws InterruptedException
     */
    @Override
    public synchronized void startEvent(boolean wait)
            throws InterruptedException {
        validateStarted();
        stub.isActive = false;
        runNewThread("start", new AppletStart(getGameObj()), getWaitTime(wait));
    }


    /**
     * Sends the stop event to the applet.
     *
     * @throws InterruptedException
     */
    @Override
    public synchronized void stopEvent(boolean wait)
            throws InterruptedException {
        validateStarted();
        stub.isActive = false;
        runNewThread("stop", new AppletStop(getGameObj()), getWaitTime(wait));
    }


    @Override
    public synchronized boolean destroy(ProgressController progress) {
        progress.advance(0, 20, 0);

        boolean destroyed = true;
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
        if (stub != null) {
            try {
                runNewThread("destroy", new AppletDestroy(getGameObj()),
                        3000L);
            } catch (InterruptedException e) {
                // We really don't care at this point if we're interrupted; we're
                // trying to kill the thread.
            }
            progress.advanceBy(1);
            if (! stopThreads(progress)) {
                destroyed = false;
            }
        }

        if (destroyed) {
            setGameObj(null);
            stub = null;
        }
        return destroyed;
    }


    private class Stub implements AppletStub {
        private final AppletContext context = new DefaultAppletContext();
        boolean isActive = false;

        @Override
        public boolean isActive() {
            return isActive;
        }

        @Override
        public URL getDocumentBase() {
            try {
                return getConfig().getDocumentBase().getURI().toURL();
            } catch (MalformedURLException e) {
                return getConfig().getBaseUrl();
            }
        }

        @Override
        public URL getCodeBase() {
            return getConfig().getBaseUrl();
        }

        @Override
        public String getParameter(String name) {
            return getConfig().getParameters().get(name);
        }

        @Override
        public AppletContext getAppletContext() {
            return context;
        }

        @Override
        public void appletResize(int width, int height) {
            if (width < 0) {
                width = getConfig().getWidth();
            }
            if (height < 0) {
                height = getConfig().getHeight();
            }
            appletPanel.setPreferredSize(new Dimension(width, height));
            appletPanel.validate();
        }
    }


    static class AppletInitStart implements Runnable {
        private final Applet applet;

        AppletInitStart(Applet applet) {
            this.applet = applet;
        }

        @Override
        public void run() {
            debug(applet, "init starting");
            applet.init();
            debug(applet, "init ended, start starting");
            applet.start();
            debug(applet, "start ended");
        }
    }


    static class AppletStart implements Runnable {
        private final Applet applet;

        AppletStart(Applet applet) {
            this.applet = applet;
        }

        @Override
        public void run() {
            debug(applet, "start starting");
            applet.start();
            debug(applet, "start ended");
        }
    }


    static class AppletStop implements Runnable {
        private final Applet applet;

        AppletStop(Applet applet) {
            this.applet = applet;
        }

        @Override
        public void run() {
            debug(applet, "stop starting");
            applet.stop();
            debug(applet, "stop ended");
        }
    }


    static class AppletDestroy implements Runnable {
        private final Applet applet;

        AppletDestroy(Applet applet) {
            this.applet = applet;
        }

        @Override
        public void run() {
            debug(applet, "destroy starting");
            applet.destroy();
            debug(applet, "destroy ended");
        }
    }


    /**
     * Viewer for the applets. This does not setup the SecurityManager.
     * <p/>
     * Original post:
     * http://www.java-gaming.org/topics/discuss-the-future-of-4k-contest
     * /30600/msg/284588/view.html#msg284588
     *
     * @author Sunsword
     * @author Groboclown
     */
    public class AppletPanel extends JPanel {
        private boolean isReady;

        public AppletPanel(Applet applet) {
            super(new BorderLayout());

            // useful to see if the applet has actually loaded, and it also adds
            // a bit of a background flair to match the java4k site
            setBackground(Color.BLACK);

            add(applet, BorderLayout.NORTH);
        }

        public void initialize() {
            this.isReady = true;
            repaint();
        }

        @Override
        public void paint(Graphics g) {
            // We put the start execution inside the paint method to ensure
            // that we actually have a graphics context in which we can run.
            synchronized (this) {
                if (isReady) {
                    isReady = false;
                    getLauncherManager().getWorkerManager().startEDT(
                            getDetail().getName(),
                            ActionSource.GAME_START, new Runnable() {
                        @Override
                        public void run() {
                            try {
                                activate(false);
                            } catch (InterruptedException e) {
                                // FIXME report took too long
                                // quick stop
                            }
                        }
                    });
                }
            }
        }





        @Override
        protected void finalize() throws Throwable {
            // We know that this runner isn't being referenced by the
            // launcher manager, therefore this should never need to be called.
            // However, in case this is ever used in the future by something
            // else, we'll just add this for completion.
            destroy(new ChildProgressController(null, 1));

            System.out.println("** Garbage collected applet runner " + getName());


            super.finalize();
        }


        @Override
        public void processEvent(AWTEvent e) {
            switch (e.getID()) {
                case WindowEvent.WINDOW_ICONIFIED:
                    try {
                        stopEvent(false);
                    } catch (InterruptedException e2) {
                        // ignore - we're not waiting
                    }
                    break;
                case WindowEvent.WINDOW_DEICONIFIED:
                    try {
                        startEvent(false);
                    } catch (InterruptedException e1) {
                        // ignore - we're not waiting
                    }
                    break;
                // If window closes, exit the app
                case WindowEvent.WINDOW_CLOSING:
                    destroyFromEvent();
                    break;
                // Other events can be handled here
            }

        }
    }
}

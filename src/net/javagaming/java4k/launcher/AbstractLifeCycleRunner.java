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

import net.javagaming.java4k.launcher.progress.ProgressController;

import java.applet.Applet;
import java.awt.Container;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Groboclown
 */
public abstract class AbstractLifeCycleRunner<T extends GameConfiguration, O>
        implements GameLifeCycleRunner {
    private final LauncherManager launcherManager;
    private final T config;
    private final List<Throwable> uncaughtExceptions =
            Collections.synchronizedList(new ArrayList<Throwable>());
    private final UncaughtExceptionHandlerImpl exceptionHandler =
            new UncaughtExceptionHandlerImpl();
    private final GameDetail detail;
    private O gameObj;

    public AbstractLifeCycleRunner(GameDetail detail, T config, LauncherManager launcherManager) {
        this.detail = detail;
        this.config = config;
        this.launcherManager = launcherManager;
    }


    protected T getConfig() {
        return config;
    }

    protected O getGameObj() {
        return gameObj;
    }

    protected void setGameObj(O obj) {
        this.gameObj = obj;
    }


    public GameDetail getDetail() {
        return detail;
    }

    @Override
    public final boolean isActive() {
        return gameObj != null;
    }


    @Override
    public final List<Throwable> getExceptions() {
        return new ArrayList<Throwable>(uncaughtExceptions);
    }





    @Override
    public void start(final Container parentContainer,
            final boolean wait)
            throws InterruptedException, IOException {
        validateNotStarted();

        runNewThread("setup", new Runnable() {
            @Override
            public void run() {
                try {
                    setup(parentContainer);
                } catch (ThreadDeath t) {
                    throw t;
                } catch (HardDeathException e) {
                    // ignore
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (Throwable t) {
                    launcherManager.getErrorMessageManager().gameError(
                            getDetail().getName(),
                            t, ActionSource.GAME_START
                    );
                    // Mark this game as not started
                    launcherManager.getGameManager().setActiveGame(null);
                }
            }
        },
                // Note that we must wait for this to complete, so that we
                // can throw the exception.  However, that's only done at
                // the start of the setup, while the rest is performed in
                // the EDT.
                getWaitTime(true));
    }


    protected abstract void setup(Container parentContainer)
            throws Exception;


    /**
     * We don't call the {@link #destroy(net.javagaming.java4k.launcher.progress.ProgressController)} method ourselves -
     * it's only ever called by the
     * {@link net.javagaming.java4k.launcher.GameManager}.  Instead, call this
     * method.
     */
    protected void destroyFromEvent() {
        launcherManager.getGameManager().setActiveGame(null);
    }


    protected void validateStarted() {
        if (! isActive()) {
            throw new IllegalStateException("game not started");
        }
    }


    protected void validateNotStarted() {
        if (isActive()) {
            throw new IllegalStateException("game already started");
        }
    }


    protected final boolean stopThreads(ProgressController progress) {
        ProgressController child = progress.createChild(10);
        boolean stillAlive = stopThreadsOnce(500L, false, child);
        child.completed();

        int retryCount = 10;
        while (stillAlive && retryCount > 0) {
            child = progress.createChild(10);
            stillAlive = stopThreadsOnce(100L, true, child);
            child.completed();

            --retryCount;
        }

        return !stillAlive;
    }


    @SuppressWarnings("deprecation")
    protected final boolean stopThreadsOnce(long waitTimeMillis,
            boolean forceStop, ProgressController progress) {
        boolean stillAlive = false;
        Thread[] active = new Thread[2000];
        if (config.getThreadGroup().enumerate(active, true) >= active.length) {
            System.err.println(
                    "Game started too many threads - will not stop them all");
            stillAlive = true;
        }
        List<Thread> hardDeath = new ArrayList<Thread>();


        progress.advance(0, active.length, 0);
        for (Thread t : active) {
            if (t != null && t.isAlive()) {
                t.interrupt();
                try {
                    t.join(waitTimeMillis);
                } catch (InterruptedException e) {
                    // ignore - it just stopped us early.
                }
                if (t.isAlive()) {
                    if (forceStop) {
                        t.stop(new HardDeathException(
                                getDetail().getName()));

                        // assume it's dead - don't change the still alive
                        // state.

                        // Hard death threads seem to only stop once this
                        // worker thread stops.  We need to go back and check
                        // its status, but for now mark it as a dead thread.
                        hardDeath.add(t);
                    } else {
                        stillAlive = true;
                        //System.err.println("Could not stop thread " +
                        //        t.getName());
                    }
                }
            }
            progress.advanceBy(1);
        }

        // FIXME add a worker process to check the status of all the
        // hard death threads at a future time.



        return stillAlive;
    }


    protected final void runNewThread(String name, Runnable action,
            long waitTime) throws InterruptedException {
        Thread thread = createThread(config.getThreadGroup(),
                action, detail.getName() + " - " + name,
                config, exceptionHandler);
        thread.start();
        if (waitTime >= 0) {
            thread.join(waitTime);
            if (thread.isAlive()) {
                System.err.println("Timeout occurred before action '" + name +
                        "' completed");
            }
        }
    }


    private Thread createThread(final ThreadGroup threadGroup,
                                      final Runnable runner, final String name,
                                      final GameConfiguration config,
                                      final Thread.UncaughtExceptionHandler ueh) {
        // NOTE: this sets the context class loader to the object we want
        // to have tight control over.  This means that this thread needs to
        // be tightly controlled.

        Thread t = new Thread(threadGroup, runner, name);
        // TODO set this correctly, but note that this could lead to yet another
        // leak of the classloader.
        //t.setContextClassLoader(config.classLoader);
        t.setContextClassLoader(null);
        t.setUncaughtExceptionHandler(ueh);
        t.setPriority(Thread.NORM_PRIORITY - 1);
        t.setDaemon(true);
        return t;
    }



    protected final long getWaitTime(boolean wait) {
        if (wait) {
            return 1000000L;
        }
        return -1;
    }


    private final class UncaughtExceptionHandlerImpl implements
            Thread.UncaughtExceptionHandler {
        @Override
        public void uncaughtException(Thread t, Throwable e) {
            if (! (e instanceof HardDeathException) &&
                    ! (e instanceof ThreadDeath)) {
                System.err.println("Exception in Game " +
                        getDetail().getName() +
                        " in thread " + t.getName());
                e.printStackTrace();
            }
            uncaughtExceptions.add(e);
        }
    }



    protected LauncherManager getLauncherManager() {
        return launcherManager;
    }

    protected static void debug(Applet applet, String msg) {
        //System.err.println(applet.getClass().getSimpleName() + ": " + msg);
    }
}

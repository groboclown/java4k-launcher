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
package net.javagaming.java4k.launcher.webstart;

import net.javagaming.java4k.launcher.AbstractLifeCycleRunner;
import net.javagaming.java4k.launcher.ActionSource;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.HardDeathException;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.progress.ProgressController;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import java.awt.Container;
import java.awt.Window;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Manages the execution of WebStart applications.
 *
 * @author Groboclown
 */
public class WebStartRunner
        extends AbstractLifeCycleRunner<WebStartConfiguration, Method> {

    public WebStartRunner(GameDetail description,
            WebStartConfiguration webStartConfiguration,
            LauncherManager launcherManager) {
        super(description, webStartConfiguration, launcherManager);
    }

    /**
     * This runs inside the game's thread group.
     *
     * @param parentContainer container to put it in, or null
     * @throws IOException
     */
    @Override
    protected synchronized void setup(Container parentContainer)
            throws Exception {
        final Method main = getConfig().loadMainMethod();
        setGameObj(main);

        // Most WebStart apps incorrectly start their jframe right in the
        // main thread, when it should be done in the EDT.  However, some
        // of them actually do the right thing, in which case helping here
        // will cause the game to fail.  Most of them loop forever in the main
        // method, which means that this setup method will not exit, which in
        // turn means the stop button is never enabled.  So, we kick this off
        // in a separate thread to run on its own.
        runNewThread(getDetail().getName(), new Runnable() {
            @Override
            public void run() {
                try {
                    main.invoke(null, (Object) new String[0]);
                } catch (InvocationTargetException e) {
                    getLauncherManager().getErrorMessageManager().gameError(
                            getDetail().getName(), e.getCause(),
                            ActionSource.GAME_START);
                } catch (ThreadDeath t) {
                    throw t;
                } catch (VirtualMachineError e) {
                    throw e;
                } catch (HardDeathException e) {
                    // ignore
                } catch (Throwable t) {
                    getLauncherManager().getErrorMessageManager().gameError(
                            getDetail().getName(), t.getCause(),
                            ActionSource.GAME_START);
                }
            }
        }, getWaitTime(false));
    }

    @Override
    public void startEvent(boolean wait) throws InterruptedException {
        validateStarted();
    }

    @Override
    public void stopEvent(boolean wait) throws InterruptedException {
        validateStarted();
    }

    @Override
    public boolean destroy(ProgressController progress) {
        boolean stillAlive = false;
        if (getGameObj() != null) {
            stillAlive = stopThreads(progress);
        }

        // Try killing the frames
        for (Window w: getLauncherManager().getAllUnmanagedWindows()) {
            w.dispose();
        }

        if (stillAlive) {
            // Assume it's dead?  How can we be sure?  How else would we be
            // able to kill it?  It could still have registered listeners
            // throughout the UI.
            stopThreads(progress);
        }

        setGameObj(null);
        return true;
    }
}

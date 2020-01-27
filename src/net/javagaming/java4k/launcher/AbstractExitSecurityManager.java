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

import javax.swing.JFrame;
import java.security.AccessController;
import java.security.Permission;
import java.security.PrivilegedAction;

/**
 * A security manager that has explicit logic for managing System.exit calls.
 *
 * @author Groboclown
 */
public class AbstractExitSecurityManager extends SecurityManager {
    private boolean allowSystemExit = false;
    private final LauncherManager launcherManager;

    public AbstractExitSecurityManager(LauncherManager launcherManager) {
        this.launcherManager = launcherManager;
    }


    void setAllowSystemExit(boolean allowSystemExit) {
        this.allowSystemExit = allowSystemExit;
    }


    @Override
    public void checkExit(int status) {
        // This is really tricky.  Some WebStart apps register the default
        // frame close operation as exit, which causes a call to this method.
        // We want to allow that registration to happen, but we don't want
        // a system.exit to happen.

        if (allowSystemExit) {
            // Back door to allow the main program to exit.
            return;
        }

        // We want the full stack trace, not just the class stack.  If
        // JFrame.setDefaultCloseOperation is in the stack, then we allow
        // this call to happen.
        Throwable t = new Throwable();
        t.fillInStackTrace();
        for (StackTraceElement el: t.getStackTrace()) {
            if (el.getClassName().equals(JFrame.class.getName()) &&
                    el.getMethodName().equals("setDefaultCloseOperation")) {
                // allow it
                return;
            }
            // If JFrame is in the stack, then we know that the JFrame close
            // button was clicked.
            //if (el.getClassName().equals(JFrame.class.getName())) {
        }

        checkSystemExit(status);
        throw new SecurityException("Only the main class can exit");
    }

    protected void checkSystemExit(int status) {
        // Kill the active game
        launcherManager.getGameManager().setActiveGame(null);
    }

    protected GameConfiguration getNullableGameConfiguration() {
        ClassLoader loader = AccessController.doPrivileged(
                new PrivilegedAction<ClassLoader>() {
                    @SuppressWarnings("deprecation")
                    @Override
                    public ClassLoader run() {
                        return currentClassLoader();
                    }
                });

        return launcherManager.getGameManager().
                getActiveGameFor(loader);
    }
}

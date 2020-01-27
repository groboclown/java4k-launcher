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

import java.security.Permission;

/**
 * This will need some major changes.  Specifically, change the class loader
 * references to the thread group.
 * <p>
 * Applets need to have restricted access to which thread groups they can
 * manage.  The default security manager is very loose on creation on thread
 * groups.
 * </p>
 *
 * @author Groboclown
 */
public class AppletPolicySecurityManager extends AbstractExitSecurityManager {

    public AppletPolicySecurityManager(LauncherManager launcherManager) {
        super(launcherManager);
    }

    // For debugging security issues
    @Override
    public void checkPermission(Permission perm) {
        try {
            super.checkPermission(perm);
        } catch (SecurityException e) {
            System.out.println("** Prevented checkPermission(perm=" + perm +
                    ")");
            e.printStackTrace();
            throw e;
        }
    }
    @Override
    public void checkPermission(Permission perm, Object context) {
        try {
            super.checkPermission(perm, context);
        } catch (SecurityException e) {
            System.out.println("** Prevented checkPermission(perm=" + perm +
                    ",cxt=" + context +")");
            e.printStackTrace();
            throw e;
        }

    }


    boolean isInCheckConnect = false;

    @Override
    public synchronized void checkConnect(String host, int port) {
        // Because the check for isHost can cause recursion, we need to
        // protect this against infinite recursion.
        if (!isInCheckConnect) {
            try {
                isInCheckConnect = true;

                GameConfiguration config = getNullableGameConfiguration();
                if (config != null && config.isHost(host, port)) {
                    return;
                }

                super.checkConnect(host, port);
            } finally {
                isInCheckConnect = false;
            }
        }
    }


    /* This ends up doing the wrong thing in most cases, which ends up
       causing more issues than I think this prevents.

    private boolean inThreadGroupCheck = false;
    @Override
    public synchronized void checkAccess(ThreadGroup g) {
        if (inThreadGroupCheck) {
            // prevent infinite recursion
            checkPermission(new RuntimePermission("modifyThreadGroup"));
        } else {
System.out.println("checkAccess(ThreadGroup)");
            try {
                inThreadGroupCheck = true;
                if (!isValidThreadGroupAccess(g)) {
                    checkPermission(new RuntimePermission("modifyThreadGroup"));
                }
            } finally {
                inThreadGroupCheck = false;
            }
        }
    }

    @Override
    public synchronized void checkAccess(Thread t) {
        if ((t.getState() != Thread.State.TERMINATED)
                && !isValidThreadGroupAccess(t.getThreadGroup())) {
            checkPermission(new RuntimePermission("modifyThread"));
        }
    }


    private boolean inThreadGroup(GameConfiguration config, ThreadGroup g) {
        if (config != null) {
            ThreadGroup tg = getThreadGroup();
            if (tg.parentOf(g)) {
                return true;
            }
        }
        return false;
        // return getNullableGameConfiguration() != null && getThreadGroup().parentOf(g);
    }

    private boolean isValidThreadGroupAccess(ThreadGroup g) {
        GameConfiguration config = getNullableGameConfiguration();
System.out.println("isValidThreadGroupAccess: has config? " + (config != null));

        if (inThreadGroup(config, g)) {
System.out.println(" - it's in the correct thread group");
            return true;
        }

        // WebStart apps have a looser policy regarding thread groups
        if (config != null && config.isWebApp()) {
System.out.println(" - it's a web app");
            return true;
        }

System.out.println(" - game is null, or it's accessing the wrong group and it's not a web app");

        // the given thread group argument is not in the applet thread group
        return false;
    }

    */

    @Override
    public ThreadGroup getThreadGroup() {
        GameConfiguration config = getNullableGameConfiguration();
        if (config == null) {
            return super.getThreadGroup();
        }
        return config.getThreadGroup();
    }

}

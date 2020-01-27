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

import javax.swing.JOptionPane;
import java.io.File;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.Policy;
import java.util.Arrays;
import java.util.List;

/**
 * @author Groboclown
 */
public class Security {

    private static enum SecurityMode {
        PUBLIC,
        DEVELOPER,
        DEBUG
    }


    private static File rootDir;
    private static File cacheDir;
    private static SecurityMode securityMode = SecurityMode.PUBLIC;

    private Security() {
        // Singleton
    }


    /**
     * Load the specific security settings based on the arguments passed in
     * by the user.
     *
     * @param args command-line arguments
     */
    public static void setupOptions(String[] args) {
        for (String arg: args) {
            if ("-d".equals(arg)) {
                System.out.println("Running in Developer Mode");
                securityMode = SecurityMode.DEVELOPER;
            }
            if ("-g".equals(arg)) {
                System.out.println("Running in Debug Mode");
                securityMode = SecurityMode.DEBUG;
            }
        }
    }


    public static boolean showLocalGames() {
        return (securityMode != SecurityMode.PUBLIC);
    }



    public static File getRootDir() {
        if (rootDir == null) {
            try {
                rootDir = new File(System.getProperty("launcher.dir"));
            } catch (SecurityException e) {
                // We don't have access to the error dialogs.
                JOptionPane.showMessageDialog(null,
                        "Could not access install directory.",
                        "Setup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
            }
        }
        return rootDir;
    }

    public static File getCacheDir() {
        if (cacheDir == null) {
            cacheDir = new File(getRootDir(), "downloads");
        }
        return cacheDir;
    }


    public static AbstractExitSecurityManager setSecurityManager(
            LauncherManager launcherManager) {
        AbstractExitSecurityManager ret;
        if (securityMode == SecurityMode.DEBUG) {
            // no security manager
            System.out.println("*** WARNING: running without a security manager! ***");
            ret = new ExitSecurityManager(launcherManager);
        } else {
            // switch the security policy based on the security mode.
            String policyName = "public.policy";
            if (securityMode == SecurityMode.DEVELOPER) {
                policyName = "developer.policy";
            }

            URL policyResource = Security.class.getResource(policyName);
            if (policyResource == null) {
                throw new IllegalStateException("Could not find security policy file");
            }

            System.setProperty("java.security.policy", policyResource.toString());
            Policy.getPolicy().refresh();
            ret = new AppletPolicySecurityManager(launcherManager);
        }
        System.setSecurityManager(ret);

        return ret;
    }


    public static boolean isHost(URI document, String hostname, int port) {
        String documentHost = document.getHost();
        if (!hostname.equals(documentHost)) {
            // Oh boy.

            try {
                boolean foundOne = false;
                List<InetAddress> archiveAddr = Arrays.asList(InetAddress
                        .getAllByName(documentHost));
                for (InetAddress hostnameAddr : InetAddress
                        .getAllByName(hostname)) {
                    if (archiveAddr.contains(hostnameAddr)) {
                        foundOne = true;
                        break;
                    }
                }
                if (!foundOne) {
                    return false;
                }

            } catch (UnknownHostException e) {
                return false;
            }
        }

        int archivePort = document.getPort();

        // FIXME because this is a URI, not a URL, we can't get the default
        // port.  Assume it's 80.
        return (port == archivePort
                || (archivePort < 0 && port == 80));
    }
}

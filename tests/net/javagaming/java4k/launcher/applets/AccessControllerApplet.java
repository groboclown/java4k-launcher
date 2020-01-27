package net.javagaming.java4k.launcher.applets;

import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Attempts to run the {@link AccessController} from within the applet.
 */
public class AccessControllerApplet extends Applet {
    @Override
    public void init() {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            @Override
            public Object run() {
                try {
                    File f = File.createTempFile("abc", ".tmp");
                    System.err.println("error: created temp file " + f);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
    }
}


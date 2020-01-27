package net.javagaming.java4k.launcher.applets;

import javax.swing.SwingUtilities;
import java.applet.Applet;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * Test applet for performing reflection. This should be prohibited.
 */
public class SwingUtilitiesInvokeApplet extends Applet {
	private static final long serialVersionUID = 1L;

	public SwingUtilitiesInvokeApplet() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    File f;
                    try {
                        f = File.createTempFile("myTempFile", ".tmp");
                        f.delete();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

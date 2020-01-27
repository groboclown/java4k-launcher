package net.javagaming.java4k.launcher.applets;

import java.applet.Applet;

/**
 * Test applet for performing reflection. This should be prohibited.
 */
public class ReflectionApplet extends Applet {
	private static final long serialVersionUID = 1L;

    @Override
	public void start() {
        System.err.println("Begin ReflectionApplet.start()");
		ClassLoader.class.getDeclaredFields();
	}
}

package net.javagaming.java4k.launcher.applets;

import java.applet.Applet;

/**
 * Test applet for performing reflection. This should be prohibited.
 */
public class ConstructorBadApplet extends Applet {
	private static final long serialVersionUID = 1L;

	public ConstructorBadApplet() {
        System.err.println("Begin ConstructorBadApplet()");
		ClassLoader.class.getDeclaredFields();
	}
}

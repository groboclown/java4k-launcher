package net.javagaming.java4k.launcher.applets;

import java.applet.Applet;

public class SecurityManagerReplacementApplet extends Applet {

	public SecurityManagerReplacementApplet() {
		System.setSecurityManager(null);
	}

}

package net.javagaming.java4k.launcher.applet;

import net.javagaming.java4k.launcher.AbstractGameDetail;
import net.javagaming.java4k.launcher.GameConfiguration;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.cache.Resource;

import java.io.IOException;

/**
 * @author Groboclown
 */
public class AppletGameDetail extends AbstractGameDetail {

    public AppletGameDetail(GameDescription source, Resource detailsSource) {
        super(source, detailsSource);
    }

    @Override
    public GameConfiguration createGameConfiguration(
            ThreadGroup parentAppletThreadGroup) throws IOException {
        AppletConfiguration ret = new AppletConfiguration(getClassName(),
                getJar(), getDocumentBase(),
                getSize().width, getSize().height,
                parentAppletThreadGroup, this);
        ret.setParameters(getParameters());
        return ret;
    }
}

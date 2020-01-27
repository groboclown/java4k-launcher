package net.javagaming.java4k.launcher.webstart;

import net.javagaming.java4k.launcher.AbstractGameDetail;
import net.javagaming.java4k.launcher.GameConfiguration;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.cache.Resource;

import java.io.IOException;

/**
 * @author Groboclown
 */
public class WebStartGameDetail extends AbstractGameDetail {

    public WebStartGameDetail(GameDescription source, Resource detailsSource) {
        super(source, detailsSource);
    }

    @Override
    public GameConfiguration createGameConfiguration(
            ThreadGroup parentAppletThreadGroup) throws IOException {
        WebStartConfiguration ret = new WebStartConfiguration(getClassName(),
                getJar(), getDocumentBase(),
                parentAppletThreadGroup, this);
        return ret;
    }
}

package net.javagaming.java4k.launcher;

import net.javagaming.java4k.launcher.cache.Resource;

import java.awt.Dimension;
import java.awt.Image;
import java.io.IOException;
import java.util.Map;

/**
 * Represents an abstract view of details that are loaded after the more
 * general {@link net.javagaming.java4k.launcher.GameDescription} is
 * loaded.  This is for things like loading in the icon, the jar file,
 * and processing extra files of meta-information.  When this object exists
 * in the owning object, it's assumed that all of the "big" data (icons,
 * jar files, etc) have already been downloaded locally.
 *
 * @author Groboclown
 */
public interface GameDetail {

    /**
     *
     * @return the precise name for this game.  May be different than the
     *      description.
     */
    public String getName();


    /**
     *
     * @return the unique identifier used by the host site, so that extra
     *      communication (e.g. submitting comments) can be made easier.
     */
    public String getId();


    /**
     *
     * @return the builder associated with this detail.  This must be constant.
     */
    public GameDescription getSource();


    public Image getIcon();

    public Resource getJar();

    public Dimension getSize();

    public Resource getDocumentBase();

    /**
     * For an Applet, this is the page hosting the applet tag (or, for a JNLP
     * defined applet, the JNLP file).  For a WebStart app, this is the JNLP
     * file.
     *
     * @return source that defines these details.
     */
    public Resource getDetailsSource();


    public String getClassName();


    public Map<String, String> getParameters();


    /**
     * Creates the applet configuration.
     *
     * @param parentAppletThreadGroup the parent thread group for the game,
     *     for use in creating the configuration object.
     * @return the configuration for the game
     * @throws java.io.IOException
     */
    public GameConfiguration createGameConfiguration(
            ThreadGroup parentAppletThreadGroup)
            throws IOException;
}

package net.javagaming.java4k.launcher;

import java.io.IOException;

/**
 * @author Groboclown
 */
public interface GameConfiguration {

    /**
     * Does this game have the given host / port as its home?
     *
     * @param hostname
     * @param port
     * @return true if the given host and port match this game's setup,
     *      otherwise false.
     */
    boolean isHost(String hostname, int port);


    /**
     *
     * @return the runner to manage this game's life cycle.
     */
    GameLifeCycleRunner createGameLifeCycleRunner(
            LauncherManager launcherManager);

    /**
     * FIXME we need tight control over who has the class loader.  If it dangles
     * around after the game has closed, then the class will remain loaded.
     * Perhaps wrap it in a reference?
     *
     * @return the classloader for this configuration.
     */
    boolean isClassLoader(ClassLoader cl);

    /**
     *
     * @return the thread group containing this game's activity
     */
    ThreadGroup getThreadGroup();


    /**
     * Prepopulate the cached game jar.
     *
     * @throws IOException
     */
    void loadCache() throws IOException;


    boolean isWebApp();
}

package net.javagaming.java4k.launcher;

/**
 * Listens for changes to the running game's state.  These are guaranteed to
 * fire in the event dispatch thread.
 *
 * @author Groboclown
 */
public interface GameStateListener {
    public void onGameStarted(GameDetail detail);


    public void onGameStopped(GameDetail detail);
}

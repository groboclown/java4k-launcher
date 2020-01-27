package net.javagaming.java4k.launcher;

import net.javagaming.java4k.launcher.progress.HostedResourceConsumer;
import net.javagaming.java4k.launcher.progress.NamedResourceConsumer;

import java.util.Collection;

/**
 * Finishes reading in the details for a game.  This is a strange entity -
 * This fills in the blanks for a
 *
 * Reads a resource that contains details for a specific
 * {@link net.javagaming.java4k.launcher.GameDescription}.  The processor
 * for the generated results must know how to map the keys to objects.
 *
 * @author Groboclown
 */
public interface GameDetailListResourceReader
        extends HostedResourceConsumer<GameDetail>,
        NamedResourceConsumer<GameDetail> {

    /**
     * Mark a source as pending for processing.  This reader will match it
     * up against the {@link DefaultGameDescription#getGameDetailSource()} reference.
     * Once the source is processed, it will be removed from interal
     * storage.
     *
     * @param source
     */
    public void addPendingSource(GameDescription source);


    /**
     *
     * @return all sources that haven't been processed.
     */
    public Collection<GameDescription> getPendingSources();
}

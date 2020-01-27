package net.javagaming.java4k.launcher.ui;

import net.javagaming.java4k.launcher.ActionSource;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.progress.HostedResourceConsumer;
import net.javagaming.java4k.launcher.progress.ProgressWorker;
import net.javagaming.java4k.launcher.progress.ResourceConsumer;

/**
 * @author Groboclown
 */
public abstract class ResourceReaderPanel<V>
        extends AbstractResourcePanel<V> {
    private ResourceConsumer<V> consumer;

    public ResourceReaderPanel(LauncherManager launcherManager) {
        super(launcherManager);
    }

    public <T extends ResourceConsumer<V>> void setConsumer(T consumer) {
        if (consumer == null) {
            throw new NullPointerException();
        }
        this.consumer = consumer;
    }

    /**
     * Performs a load resource with the resource that's provided by the
     * consumer.  This should only be called if the consumer is a
     * {@link net.javagaming.java4k.launcher.progress.HostedResourceConsumer}.
     *
     * @param source action source
     */
    public void loadResource(ActionSource source) {
        loadResource(source, false);
    }


    /**
     * Performs a load resource with the resource that's provided by the
     * consumer.  This should only be called if the consumer is a
     * {@link net.javagaming.java4k.launcher.progress.HostedResourceConsumer}.
     *
     * @param source action source
     */
    public void reloadResource(ActionSource source) {
        loadResource(source, true);
    }


    /**
     * Performs a load resource with the resource that's provided by the
     * consumer.  This should only be called if the consumer is a
     * {@link net.javagaming.java4k.launcher.progress.HostedResourceConsumer}.
     *
     * @param source action source
     * @param flash should the resource be flashed first?
     */
    public void loadResource(ActionSource source, boolean flash) {
        if (consumer == null || !(consumer instanceof HostedResourceConsumer)) {
            throw new IllegalStateException();
        }
        Resource r = ((HostedResourceConsumer<V>) consumer).getResource();
        if (r == null) {
            throw new NullPointerException("null resource");
        }
        loadResource(source, r, flash);
    }


    public void loadResource(ActionSource source, Resource r, boolean flash) {
        if (consumer == null) {
            throw new IllegalStateException();
        }
        super.loadResource(source, r, flash);
    }


    @Override
    protected void processLoadedResource(Resource r,
            ProgressWorker.Publisher<V> controller) throws Exception {
        consumer.processLoadedResource(r, controller);
    }
}

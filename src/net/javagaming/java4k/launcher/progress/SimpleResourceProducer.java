package net.javagaming.java4k.launcher.progress;

import net.javagaming.java4k.launcher.ChildProgressController;
import net.javagaming.java4k.launcher.cache.Resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple producer for a
 * {@link net.javagaming.java4k.launcher.progress.ResourceConsumer}
 *
 * @author Groboclown
 */
public class SimpleResourceProducer {

    public static <V> List<V> load(HostedResourceConsumer<V> rc)
            throws Exception {
        return load(rc.getResource(), rc);
    }

    public static <V> List<V> load(Resource r, ResourceConsumer<V> rc)
            throws Exception {
        SimplePublisher<V> p = new SimplePublisher<V>();
        rc.processLoadedResource(r, p);
        return p.queue;
    }

    static class SimplePublisher<V> extends ChildProgressController
            implements ProgressWorker.Publisher<V> {
        private final List<V> queue = new ArrayList<V>();

        public SimplePublisher() {
            super(null, 1);
        }

        @Override
        public void publish(V... values) {
            queue.addAll(Arrays.asList(values));
        }

        public List<V> getQueue() {
            return queue;
        }
    }

}

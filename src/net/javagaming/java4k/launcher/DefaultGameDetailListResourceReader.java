/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to &lt;http://unlicense.org/
 */
package net.javagaming.java4k.launcher;

import net.javagaming.java4k.launcher.applet.HtmlAppletDetailResourceReader;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.progress.AbstractHostedResourceConsumer;
import net.javagaming.java4k.launcher.progress.ProgressWorker;
import net.javagaming.java4k.launcher.webstart.JnlpDetailResourceReader;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Groboclown
 */
public class DefaultGameDetailListResourceReader
        extends AbstractHostedResourceConsumer<GameDetail>
        implements GameDetailListResourceReader {
    private static final Map<GameDescription.SourceType, SourceTypeResourceReader> TYPE_READERS;
    static {
        Map<GameDescription.SourceType, SourceTypeResourceReader> m =
            new HashMap<GameDescription.SourceType, SourceTypeResourceReader>();
        m.put(GameDescription.SourceType.HTML, HtmlAppletDetailResourceReader.READER);
        m.put(GameDescription.SourceType.JNLP, JnlpDetailResourceReader.READER);
        TYPE_READERS = Collections.unmodifiableMap(m);
    }




    public static interface SourceTypeResourceReader {
        void processLoadedResource(Resource r,
            ProgressWorker.Publisher<GameDetail> controller,
            GameDescription source) throws Exception;
    }


    private final Map<Resource, GameDescription> pending =
            Collections.synchronizedMap(
                    new HashMap<Resource, GameDescription>());


    public DefaultGameDetailListResourceReader() {
        super("Game details");
    }


    @Override
    public void addPendingSource(GameDescription source) {
        if (source == null) {
            throw new NullPointerException();
        }
        if (source.getGameDetailSource() == null) {
            throw new IllegalArgumentException("description " + source +
                    " has null GameDetailSource");
        }
        pending.put(source.getGameDetailSource(), source);
    }

    @Override
    public Collection<GameDescription> getPendingSources() {
        return pending.values();
    }


    @Override
    public void processLoadedResource(Resource r,
            ProgressWorker.Publisher<GameDetail> controller) throws Exception {
        controller.advance(0, 100, 0);

        GameDescription source = pending.remove(r);
        if (source == null) {
            throw new IllegalArgumentException("resource " + r +
                " has already been consumed, or was never queued for consumption");
        }
        if (source.getSourceType() == null) {
            throw new IllegalArgumentException("resource " + r +
                    " has no source type assigned");
        }

        controller.advanceTo(10);
        SourceTypeResourceReader reader = TYPE_READERS.get(source.getSourceType());
        controller.advanceTo(50);
        if (reader == null) {
            throw new IllegalStateException("unknown source type " +
                    source.getSourceType());
        }
        reader.processLoadedResource(r, controller, source);
    }
}

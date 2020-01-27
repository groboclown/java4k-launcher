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
package net.javagaming.java4k.launcher.applet;

import net.javagaming.java4k.launcher.DefaultGameDetailListResourceReader;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.WebUtil;
import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import javax.imageio.ImageIO;
import javax.swing.text.ElementIterator;
import javax.swing.text.html.HTMLDocument;
import java.io.InputStream;

/**
 * Processes a GameDescription resource to produce GameDetail instances
 * for JNLP files.  JNLP files can define either applets or WebStart apps.
 *
 * @author Groboclown
 */
public class HtmlAppletDetailResourceReader implements
        DefaultGameDetailListResourceReader.SourceTypeResourceReader {

    public static final HtmlAppletDetailResourceReader READER = new HtmlAppletDetailResourceReader();

    @Override
    public void processLoadedResource(Resource html,
            ProgressWorker.Publisher<GameDetail> controller,
            GameDescription source) throws Exception {
        String docText = html.readAsString(Resource.DEFAULT_ENCODING);
        controller.advanceBy(20);
        HTMLDocument doc = WebUtil.toWebPage(docText);
        controller.advanceBy(5);
        ElementIterator it = new ElementIterator(doc);
        javax.swing.text.Element elem;
        while ((elem = it.next()) != null) {
            if (WebUtil.isAppletTag(elem)) {
                AppletGameDetail gd = new AppletGameDetail(source, html);
                WebUtil.loadAppletTag(gd, html, elem);

                // use the server info jar over the applet tag
                if (source.getServerInfo().containsKey("jar-uri")) {
                    gd.setJar(source.getGameDetailSource().getChildResource(
                            Cache.uri(source.getServerInfo().get("jar-uri"))));
                }

                // download the icon
                Resource iconResource = source.getIconResource();
                if (iconResource != null) {
                    InputStream in = iconResource.read();
                    try {
                        gd.setIcon(ImageIO.read(in));
                    } finally {
                        in.close();
                    }
                }
                controller.advanceBy(10);

                // Download the jar
                if (gd.getJar() != null) {
                    gd.getJar().read().close();
                    controller.advanceBy(10);
                }

                controller.publish(gd);
            }
        }
        controller.advanceTo(100);
    }

}

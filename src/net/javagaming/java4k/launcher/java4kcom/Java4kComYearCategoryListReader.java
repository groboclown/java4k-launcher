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
package net.javagaming.java4k.launcher.java4kcom;

import net.javagaming.java4k.launcher.CategoryListResourceReader;
import net.javagaming.java4k.launcher.CommentSubmission;
import net.javagaming.java4k.launcher.GameDescriptionListResourceReader;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.json.JSONArray;
import net.javagaming.java4k.launcher.json.JSONTokener;
import net.javagaming.java4k.launcher.progress.AbstractHostedResourceConsumer;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import java.io.IOException;
import java.net.URI;

/**
 * Loads the games by the contest.
 *
 * @author Groboclown
 */
public class Java4kComYearCategoryListReader
        extends AbstractHostedResourceConsumer<GameDescriptionListResourceReader>
        implements CategoryListResourceReader {
    private final Java4kComCommentSubmission commentSubmission;

    public Java4kComYearCategoryListReader(LauncherManager launcherManager)
            throws IOException {
        super(LauncherBundle.getString("java4k.site.dir"),
                getCategoryListResource());
        commentSubmission = new Java4kComCommentSubmission(launcherManager);
    }

    @Override
    public void processLoadedResource(Resource r,
            ProgressWorker.Publisher<GameDescriptionListResourceReader> controller)
            throws IOException {
        controller.advance(0, 100, 0);

        // This method just doesn't load the <a> tags right
        //HTMLDocument page = WebUtil.loadWebPage(url);
        String page = r.readAsString(Resource.DEFAULT_ENCODING);

        controller.advanceTo(50);

        Object obj = new JSONTokener(page).nextValue();
        if (! (obj instanceof JSONArray)) {
            throw new IOException(
                    LauncherBundle.getString("contest-decode-error"));
        }

        JSONArray list = (JSONArray) obj;

        //System.out.println("loaded page with " + list.length() + " items");

        controller.advance(0, list.length() + 51, 51);

        // For this site, the very last contest entry is the current
        // contest.
        for (int i = 0; i < list.length(); ++i) {
            Contest contest = new Contest(r, list.getJSONObject(i));

            controller.publish(new Java4kComGameDescriptionListResourceReader(
                    contest, i >= list.length() - 1));
            controller.advanceBy(1);
        }
    }


    private static Resource getCategoryListResource() throws IOException {
        String strurl = LauncherBundle.getString("url.game.category.list.contest");
        //System.out.println("Loading year categories from " + strurl);
        URI url = Cache.uri(strurl);

        // We don't expect the contents of the category to change much
        // (about once a year), so we mark it as static
        return Cache.getInstance().getTopResource(url, true);
    }

    @Override
    public CommentSubmission getCommentSubmission() {
        return commentSubmission;
    }
}

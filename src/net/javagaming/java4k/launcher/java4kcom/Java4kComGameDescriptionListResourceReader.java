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

import net.javagaming.java4k.launcher.DefaultGameDescription;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.GameDescriptionListResourceReader;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.UserComment;
import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.json.JSONArray;
import net.javagaming.java4k.launcher.json.JSONObject;
import net.javagaming.java4k.launcher.json.JSONString;
import net.javagaming.java4k.launcher.json.JSONTokener;
import net.javagaming.java4k.launcher.progress.AbstractHostedResourceConsumer;
import net.javagaming.java4k.launcher.progress.ProgressWorker;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * java4k.com applet list source.  It requires the current competition
 * list main page URL.
 *
 * @author Groboclown
 * @author Sunsword
 */
public class Java4kComGameDescriptionListResourceReader
        extends AbstractHostedResourceConsumer<GameDescription>
        implements GameDescriptionListResourceReader {
    private final static SimpleDateFormat DATE_FORMATTER =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public Java4kComGameDescriptionListResourceReader(Contest contest, boolean isStatic) {
        super(contest.getName(), contest.getSource().getChildResource(
                contest.getGamesList(), isStatic));
    }


    @Override
    public void processLoadedResource(Resource resource,
            ProgressWorker.Publisher<GameDescription> controller) throws Exception {
        controller.advance("description.progress.download-entries",
                getName());
        String data = resource.readAsString(Resource.DEFAULT_ENCODING);

        controller.advance("description.progress.process-entries",
                getName());
        JSONArray list = (JSONArray) new JSONTokener(data).nextValue();
        controller.advance(0, list.length(), 0);
        //System.out.println("- read " + list.length() + " entries");



        // FIXME comments are probably included in that, as well.


        for (int i = 0; i < list.length(); ++i) {
            try {
                JSONObject obj = list.getJSONObject(i);

                int id = obj.getInt("id");
                if (LauncherBundle.isFiltered("game", id)) {
                    System.err.println("** SKIPPING " + obj.getString("title") +
                            " (" + id + ") - MARKED AS SPAM **");
                    continue;
                }

                DefaultGameDescription desc = parseBuilder(resource, obj);

                controller.publish(desc);
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new IOException(e);
            } catch (ParseException e) {
                e.printStackTrace();
                throw new IOException(e);
            }
            controller.advanceBy(1);
        }
    }



    // Applet:
    // {"id":459,"type":"applet","title":"4096 A.D.",
    // "submitted":"2013-02-12 09:15:15","downloads":2140,
    // "description":"In the year 4096 A.D. your planet is ...",
    // "instructions":"Pick red power-up to slow down meteorites, ...",
    // "author":"Mojo","screenshot":"http:\/\/java4k.com\/screenshots\/5c0d42d226a9b643e6a0b0e32da3438d.png",
    // "url":"http:\/\/java4k.com\/index.php?action=games&method=view&gid=459",
    // "external_url":"http:\/\/games.frikulin.sk\/4096AD\/",
    // "jar":"http:\/\/java4k.com\/applet.php?gid=459",
    // "comments":[{"comment_id":492,"comment":"Catherby. It ...",
    // "created":"2013-10-20 02:21:45","author":"wowgold5"}]}

    // JNLP:
    // {"id":141,"type":"jnlp","title":"0H MUMMY!!!",
    // "submitted":"2007-12-01 00:00:00","downloads":4327,
    // "description":"OH MUMMY, a classical ...",
    // "instructions":"Control:\r\nDirection arrows.",
    // "author":"Luis Javier L\u00f3pez Arredondo",
    // "screenshot":"http:\/\/java4k.com\/screenshots\/142.png",
    // "url":"http:\/\/java4k.com\/index.php?action=games&method=view&gid=141",
    // "external_url":"http:\/\/es.geocities.com\/luisja80\/eng\/ohmummy.htm",
    // "jnlp":"http:\/\/java4k.com\/file.php?gid=141&type=jnlp",
    // "comments":[{"comment_id":492,"comment":"Catherby. It ...",
    // "created":"2013-10-20 02:21:45","author":"wowgold5"}]}

    private DefaultGameDescription parseBuilder(Resource resource,
            JSONObject obj) throws ParseException, IOException {
        DefaultGameDescription builder = new DefaultGameDescription();

        builder.setName(decodeString(obj, "title"));
        builder.setId(Integer.toString(obj.getInt("id")));
        //System.out.println("- Found " + id + ": " + builder.getName());
        builder.setSubmissionDate(DATE_FORMATTER.parse(obj.getString("submitted")));
        builder.setDescription(decodeString(obj, "description"));
        builder.setInstructions(decodeString(obj, "instructions"));
        builder.setAuthor(decodeString(obj, "author"));
        builder.setIconResource(resource.getChildResource(
                Cache.uri(obj.getString("screenshot"))));
        Object externalUrl = obj.get("external_url");
        if (externalUrl != null && externalUrl instanceof JSONString) {
            builder.setAuthorContact(externalUrl.toString());
        }
        builder.addServerInfo("downloads",
                Integer.toString(obj.getInt("downloads")));
        Object comments = obj.get("comments");
        if (comments != null && comments instanceof JSONArray) {
            builder.addUserComments(parseUserComments((JSONArray) comments));
        }

        // per type parsing
        String type = obj.getString("type");
        if ("applet".equals(type)) {
            builder.setSourceType(GameDescription.SourceType.HTML);
            builder.setDetailSource(resource.getChildResource(
                    Cache.uri(obj.getString("url"))));
            builder.addServerInfo("jar-uri", obj.getString("jar"));
        } else if ("jnlp".equals(type)) {
            builder.setSourceType(GameDescription.SourceType.JNLP);
            builder.setDetailSource(resource.getChildResource(
                    Cache.uri(obj.getString("jnlp"))));
        }
        return builder;
    }

    private List<UserComment> parseUserComments(JSONArray comments)
            throws ParseException {
        List<UserComment> ret = new ArrayList<UserComment>(comments.length());

        for (int i = 0; i < comments.length(); ++i) {
            JSONObject obj = comments.getJSONObject(i);
            int id = obj.getInt("comment_id");

            if (LauncherBundle.isFiltered("comment", id)) {
                System.err.println("** SKIPPING COMMENT " +
                        " (" + id + ") - MARKED AS SPAM **");
                continue;
            }

            UserComment comment = new UserComment(
                    Integer.toString(id), decodeString(obj, "author"),
                    DATE_FORMATTER.parse(obj.getString("created")),
                    decodeString(obj, "comment"));
            ret.add(comment);
        }

        return ret;
    }


    @Override
    public String toString() {
        return getName();
    }



    private String decodeString(JSONObject obj, String key) {
        if (! obj.has(key)) {
            return null;
        }
        Object o = obj.get(key);
        if (o instanceof String || o instanceof JSONString) {
            String text = o.toString();
            return text.replace("\\\"", "\"").replace("\\'", "'").
                    replace("\\\\", "\\");
        }
        return null;
    }
}

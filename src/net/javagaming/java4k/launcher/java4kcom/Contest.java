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

import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.json.JSONObject;

import java.io.IOException;
import java.net.URI;

/**
 * @author Groboclown
 */
public class Contest implements Comparable<Contest> {
    private final int id;
    private final String name;
    private final String year;
    private final URI gamesList;
    private final Resource source;

    public Contest(Resource source, JSONObject obj) throws IOException {
        this.id = obj.getInt("id");
        this.name = obj.getString("name");
        this.year = obj.getString("year");
        this.gamesList = Cache.uri(obj.getString("games_url"));
        this.source = source;
    }

    public String getName() {
        return name;
    }

    public String getYear() {
        return year;
    }

    public URI getGamesList() {
        return gamesList;
    }

    @Override
    public int compareTo(Contest o) {
        return this.id < o.id ? -1 : this.id > o.id ? 1 : 0;
    }

    public Resource getSource() {
        return source;
    }
}

package net.javagaming.java4k.launcher.cache;

import net.javagaming.java4k.launcher.json.JSONArray;
import net.javagaming.java4k.launcher.json.JSONObject;
import net.javagaming.java4k.launcher.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Maps between a requested URL and a real URL.  This is because in some
 * circumstances, the requested URL is no longer available, or has moved to
 * a different location, which may cause 302 redirection problems (Java does
 * not automatically redirect from http to https, or vice versa).
 *
 * @author Groboclown
 */
public class UrlMap {
    private static final String MAPPING_FILE = "fixed-urls.json";
    private final Map<String, URI> MAP;


    public UrlMap(Map<String, String> properties) {
        MAP = Collections.unmodifiableMap(convertFromJSon(properties));
    }


    public URI getURI(String src) throws IOException {
        URI uri = MAP.get(src);
        if (uri == null) {
            try {
                uri = new URI(src);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        }
        return uri;
    }

    public URI getURI(String scheme, String host, int port, String path)
            throws IOException {
        String userAuth = null;
        String query = null;
        String fragment = null;
        if (path != null) {
            int pos = path.indexOf('#');
            if (pos >= 0) {
                fragment = path.substring(pos + 1);
                path = path.substring(0, pos);
            }
            pos = path.indexOf('?');
            if (pos >= 0) {
                query = path.substring(pos + 1);
                path = path.substring(0, pos);
            }
        }
        try {
            URI uri = new URI(scheme, userAuth, host, port, path, query, fragment);
            return getURI(uri.toString());
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }

    public Collection<String> getRedirectedUris() {
        return Collections.unmodifiableCollection(MAP.keySet());
    }




    private static Map<String, URI> convertFromJSon(
            Map<String, String> properties) {
        final Map<String, URI> ret = new HashMap<String, URI>();

        final JSONArray maps;
        try {
            maps = loadMap();
        } catch (IOException e) {
            System.err.println("[UrlMap] WARNING " + e.getMessage());
            return ret;
        }
        for (int i = 0; i < maps.length(); ++i) {
            final JSONArray map = maps.getJSONArray(i);
            if (map.length() < 2) {
                System.err.println(
                        "[UrlMap] WARNING invalid mapping in map element " + i);
            } else {
                String src = convertFromJSon(map.getString(0), properties);
                String tgt = convertFromJSon(map.getString(1), properties);
                try {
                    URI tgtUri = new URI(tgt);
                    ret.put(src, tgtUri);
                } catch (URISyntaxException e) {
                    System.err.println("[UrlMap] WARNING mapping element " + i +
                        " has invalid URI syntax (" + map.getString(1) +
                        " -> " + tgt + ")");
                }
            }
        }

        return ret;
    }


    private static String convertFromJSon(String s,
            final Map<String, String> properties) {
        for (Map.Entry<String, String> e: properties.entrySet()) {
            String k = "${" + e.getKey() + "}";
            int pos;
            while ((pos = s.indexOf(k)) >= 0) {
                s = s.substring(0, pos) + e.getValue() +
                        s.substring(pos + k.length());
            }
        }
        return s;
    }


    private static JSONArray loadMap() throws IOException {
        InputStream in = UrlMap.class.getResourceAsStream(MAPPING_FILE);
        if (in == null) {
            throw new IOException("could not find map " + MAPPING_FILE);
        }
        try {
            JSONObject obj = (JSONObject) new JSONTokener(in).nextValue();
            JSONArray map = obj.getJSONArray("map");
            if (map == null) {
                throw new IOException("no map definition in " + MAPPING_FILE);
            }
            return map;
        } finally {
            in.close();
        }
    }
}

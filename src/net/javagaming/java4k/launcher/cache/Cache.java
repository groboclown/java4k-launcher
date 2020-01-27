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
package net.javagaming.java4k.launcher.cache;

import net.javagaming.java4k.launcher.ActionSource;
import net.javagaming.java4k.launcher.ErrorMessageManager;
import net.javagaming.java4k.launcher.Java4kException;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.Security;
import net.javagaming.java4k.launcher.json.JSONArray;
import net.javagaming.java4k.launcher.json.JSONObject;
import net.javagaming.java4k.launcher.json.JSONTokener;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The main repository for the data cache.  All downloads are passed through
 * here, which determines if it needs to be refreshed from the network or
 * pulled from the already downloaded cache.  It also handles storing the
 * downloads.
 *
 * Instead of storing URLs, which are slow and perform network lookups on
 * .equals, we store URIs, and only pull out the URL when necessary.
 *
 * @author Groboclown
 */
public class Cache {
    private static final Cache INSTANCE = new Cache();
    private static final String CURRENT_VERSION = "1";
    private static final String CLASS_READABLE_DIR = "resource";
    private final File cacheDir;
    private final UrlMap urlMap;
    private final List<ResourceListener> listeners =
            new ArrayList<ResourceListener>();
    private final Object sync = new Object();
    private final Map<URI, DefaultResource> cache;
    private final File cacheIndexFile;
    private LauncherManager launcherManager;
    private int lastFileIndex = 0;

    public static Cache getInstance() {
        return INSTANCE;
    }

    public static URI uri(String uri) throws IOException {
        return getInstance().urlMap.getURI(uri);
    }

    public static URI uri(String scheme, String host, int port, String path)
            throws IOException {
        return getInstance().urlMap.getURI(scheme, host, port, path);
    }


    private Cache() {
        // singleton

        try {
            // Load the map before the cache directory.

            // TODO allow setting user-defined properties
            Map<String, String> mapProperties =
                    LauncherBundle.getStringMapping();
            this.urlMap = new UrlMap(mapProperties);



            cacheDir = Security.getCacheDir();
            if (! cacheDir.exists()) {
                if (! cacheDir.mkdirs()) {
                    throw new IllegalStateException("could not create cache dir " +
                        cacheDir);
                }
            }
            cacheIndexFile = new File(Security.getCacheDir(), "index.json");

            JSONObject obj = null;
            if (cacheIndexFile.exists()) {
                obj = readCacheFile(cacheIndexFile);
            }
            if (obj == null) {
                cache = new HashMap<URI, DefaultResource>();
            } else {
                validateCache(obj);
                cache = loadResourceCache(obj);
                lastFileIndex = loadLastFileIndex(obj);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }

    }


    UrlMap getUrlMap() {
        return urlMap;
    }

    public void setLauncherManager(LauncherManager launcherManager) {
        if (this.launcherManager != null) {
            this.launcherManager.getErrorMessageManager().gameError(
                    "initialization", new Throwable(), ActionSource.GUI_UPDATE);
            return;
        }
        this.launcherManager = launcherManager;
    }


    /**
     * Add a listener to this resource that is called when the status of the
     * resource is updated.
     *
     * @param listener the listener class.
     */
    public void addResourceListener(ResourceListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }


    public void removeResourceListener(ResourceListener listener) {
        listeners.remove(listener);
    }


    /**
     * Used for unpacking a zip file from a resource stream.
     *
     * @param r
     * @return the temp jar file.
     */
    public File getJarOutputFileFor(Resource r) {
        File f;
        synchronized (sync) {
            int id = 0;
            File root = ((DefaultResource) r).getLocal();
            do {
                f = new File(root.getParentFile(), root.getName() + ".tmp." + id);
                ++id;
            } while (f.exists());
        }
        f.deleteOnExit();
        return f;
    }


    /**
     * Gets a file that's readable by the security-restricted class
     * that's loaded from its class loader as a resource.
     *
     * @param r parent resource containing the file
     * @return the file that's in a location that's readable by the class.
     */
    public File getClassLoaderResourceFileFor(Resource r) {
        File f;
        synchronized (sync) {
            int id = 0;
            File root = ((DefaultResource) r).getLocal();
            File readDir = new File(root.getParentFile(), CLASS_READABLE_DIR);
            if (! readDir.exists()) {
                if (! readDir.mkdirs()) {
                    throw new IllegalStateException("could not create " +
                        readDir);
                }
            }
            String baseName = root.getName() + ".tmp.";
            do {
                f = new File(readDir, baseName + id);
                ++id;
            } while (f.exists());
        }
        f.deleteOnExit();
        return f;
    }



    public Resource getTopResource(URI url,
            boolean isStatic) {
        return getResource(null, url, isStatic);
    }



    protected void validateCache(JSONObject obj) {
        if (obj.has("version")) {
            Object versionObj = obj.get("version");
            if (versionObj != null) {
                String version = versionObj.toString();
                if (CURRENT_VERSION.equals(version)) {
                    // The cache is valid.
                    return;
                }
                System.err.println("cache has version [" + versionObj + "] (" +
                        versionObj.getClass() + "), expected " + CURRENT_VERSION);
            }
        //} else {
            //System.out.println("cache does not have 'version' field");
        }

        // should tell the user that their cache
        // directory is being cleared.  We need a more user-friendly
        // method for this.

        launcherManager.getErrorMessageManager().addOptionPane(
                "cache.cleared.title", JOptionPane.ERROR_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION, new ErrorMessageManager.MessageHandler() {
            @Override
            public void handleOptionSelection(Object option) {
                if (option != null && option instanceof Integer &&
                        ((Integer) option) == JOptionPane.OK_OPTION) {
                    clearCache();
                }
                // FIXME we just quit right now.
                System.exit(0);
            }
        }, "cache.cleared");
    }


    public void clearCache() {
        // Needs a progress bar.
        File[] entries = cacheDir.listFiles();
        if (entries != null) {
            for (File entry: entries) {
                if (entry.isFile()) {
                    if (! entry.delete()) {
                        warn("Could not delete " + entry);
                    }
                }
            }
        }
    }



    Resource getResource(Resource parent, URI url,
            boolean isStatic) {

        if (url == null) {
            throw new NullPointerException("null url");
        }
        boolean updated = false;
        DefaultResource res;
        synchronized (sync) {
            res = cache.get(url);
            if (res == null) {
                updated = true;
                File f = new File(cacheDir,
                        Integer.toHexString(lastFileIndex++));
                if (isStatic) {
                    res = new DefaultResource(url, f, "static");
                } else if (url.getScheme().equals("http")) {
                    res = new DefaultResource(url, f, "http");
                } else if (url.getScheme().equals("file")) {
                    // a non-static local file.  Yes, it can happen.
                    res = new DefaultResource(url, f, "static-volatile");
                } else {
                    //System.out.println("schema: " + url.getScheme());
                    res = new DefaultResource(url, f, "volatile");
                }
                cache.put(url, res);
            }
            if (parent != null && parent instanceof DefaultResource) {
                if (((DefaultResource) parent).addChild(res)) {
                    updated = true;
                }
            }
        }
        if (updated) {
            cacheUpdated();
        }
        return res;
    }


    Resource loadCachedResource(URI url) {
        Resource ret;
        synchronized (sync) {
            ret = cache.get(url);
        }
        return ret;
    }


    void resourceLoaded(final Resource res) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                for (ResourceListener listener : listeners) {
                    listener.resourceLoaded(res);
                }
            }
        };
        if (launcherManager == null) {
            // just run it here.
            r.run();
        } else {
            launcherManager.getWorkerManager().startEDT(
                "reload " + res, ActionSource.DETAILS_DOWNLOAD, r);
        }
    }



    private static JSONObject readCacheFile(File cacheIndexFile) {
        JSONObject obj;
        try {
            Reader r = new InputStreamReader(new FileInputStream(cacheIndexFile),
                    Resource.DEFAULT_ENCODING);
            try {
                obj = new JSONObject(new JSONTokener(r));
            } finally {
                r.close();
            }
        } catch (IOException e) {
            // This is before we have error dialogs.  We will ignore this error
            // for now.
            //error(e);
            e.printStackTrace();

            // Invalid cache
            // FIXME check the delete return code
            if (! cacheIndexFile.delete()) {
                warn("could not remove out-of-date cache file");
            }

            obj = null;
        }

        return obj;
    }



    private static int loadLastFileIndex(JSONObject obj) {
        return obj.getInt("lastFileIndex");
    }

    private Map<URI, DefaultResource> loadResourceCache(JSONObject obj) {
        JSONArray rc = obj.getJSONArray("resources");

        Map<URI, DefaultResource> ret = new HashMap<URI, DefaultResource>();

        for (int i = 0; i < rc.length(); ++i) {
            JSONObject resObj = rc.getJSONObject(i);
            DefaultResource res = createResourceFromJSON(resObj);
            if (res != null) {
                ret.put(res.getURI(), res);
            }
        }

        return ret;
    }

    private DefaultResource createResourceFromJSON(JSONObject resObj) {
        try {
            return new DefaultResource(resObj);
        } catch (IOException e) {
            // FIXME what to do?
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void finalize() throws Throwable {
        saveCacheIndex();
        super.finalize();
    }


    // FIXME remove this later, maybe?  Need to test performance to see if it's much of a hog.
    void cacheUpdated() {
        saveCacheIndex();
    }



    /**
     * Write the cache to disk
     */
    protected void saveCacheIndex() {
        JSONObject obj = new JSONObject();
        obj.put("version", CURRENT_VERSION);
        synchronized (sync) {
            //System.out.println("cache updated");
            obj.put("lastFileIndex", lastFileIndex);
            JSONArray resArray = new JSONArray();
            for (DefaultResource ar: cache.values()) {
                JSONObject res = ar.toJSon();
                resArray.put(res);
            }
            obj.put("resources", resArray);

            try {
                Writer out = new OutputStreamWriter(
                    new FileOutputStream(cacheIndexFile), Resource.DEFAULT_ENCODING);
                try {
                    out.write(obj.toString());
                } finally {
                    out.close();
                }
            } catch (IOException e) {
                error(e);
            }
        }
    }


    protected String getAsRelativeString(File f) {
        String af = f.getAbsolutePath();
        String cf = cacheDir.getAbsolutePath();
        if (! af.startsWith(cf)) {
            throw new IllegalStateException("not a cache relative file: " + f);
        }
        String ret = af.substring(cf.length());
        while (ret.startsWith(File.separator)) {
            ret = ret.substring(1);
        }
        return ret;
    }

    protected File getRelativeFile(String s) {
        return new File(cacheDir, s);
    }



    void error(Throwable t) {
        if (launcherManager == null) {
            t.printStackTrace();
        } else {
            t.printStackTrace();
            launcherManager.getErrorMessageManager().gameError(
                    "Cache Repository", t, ActionSource.FILE_CACHE);
        }
    }


    static void warn(String s) {
        System.err.println("*WARN* Caching: " + s);
    }


    class DefaultResource implements Resource {
        private final Set<URI> children = new HashSet<URI>();
        private final URI url;
        private final File local;
        private boolean attemptReload = false;
        private final String type;
        private URL realUrl;

        DefaultResource(URI url, File local, String type) {
            this.type = type;
            this.url = url;
            this.local = local;
        }

        DefaultResource(JSONObject obj) throws IOException {
            this.type = obj.getString("type");
            this.url = urlMap.getURI(obj.getString("url"));
            this.local = getRelativeFile(obj.getString("local"));
            if (obj.has("old")) {
                Object o = obj.get("old");
                if (o != null && o instanceof Boolean) {
                    this.attemptReload = (Boolean) o;
                }
            }

            JSONArray kids = obj.getJSONArray("children");
            for (int i = 0; i < kids.length(); ++i) {
                String kid = kids.getString(i);
                children.add(urlMap.getURI(kid));
            }
        }

        public boolean isLocal() {
            return (url.getScheme().equals("file"));
        }


        @Override
        public boolean isUpToDate() {
            return isAvailable() && ! attemptReload;
        }


        @Override
        public URI getURI() {
            return url;
        }


        @Override
        public boolean isAvailable() {
            return local.exists();
        }

        @Override
        public boolean isStatic() {
            return true;
        }

        @Override
        public Reader read(String encoding)
                throws IOException {
            return new InputStreamReader(read(), encoding);
        }

        @Override
        public String readAsString(String encoding) throws IOException {
            Reader r = read(encoding);
            StringBuilder sb = new StringBuilder();
            try {
                char[] buff = new char[4096];
                int len;
                while ((len = r.read(buff, 0, 4096)) > 0) {
                    sb.append(buff, 0, len);
                }
            } finally {
                r.close();
            }
            return sb.toString();
        }

        @Override
        public Resource getChildResource(URI url) {
            return getResource(this, url, isStatic());
        }

        @Override
        public Resource getChildResource(URI url, boolean isStatic) {
            return getResource(this, url, isStatic);
        }

        @Override
        public void flush() {
            if (local.exists() && ! isLocal()) {
                // Rather than actually deleting the local file, we'll mark it
                // as needing an update
                attemptReload = true;
            }
            for (URI child: children) {
                Resource res = loadCachedResource(child);
                if (res != null) {
                    res.flush();
                }
            }
        }

        @Override
        public InputStream read() throws IOException {
            if (SwingUtilities.isEventDispatchThread()) {
                RuntimeException r = new Java4kException("Incorrect read() in the EDT");
                r.printStackTrace();
                throw r;
            }


            if (isLocal()) {
                // no remote file is ever downloaded
                //System.out.println("Reading local file " + getURI());
                try {
                    return openRemoteInputStream();
                } catch (FileNotFoundException e) {
                    // Badly formatted JNLPs cause this, such as
                    // Grasshopper4k (2009)
                    throw new RemoteConnectionException(getURI());
                }
            }
            if (! isUpToDate()) {
                //System.out.println("Static: No local file " + getLocal() + " / needs update: " + getURI());
                download();
            }
            try {
                return new FileInputStream(getLocal());
            } catch (IOException e) {
                throw new NoCacheException(getURI());
            }
        }


        URL getRealUrl() throws MalformedURLException {
            if (realUrl == null) {
                realUrl = url.toURL();
            }
            return realUrl;
        }


        /**
         * overwrites the existing file with the remote file.
         */
        protected void download() throws IOException {
            try {

                if (! local.getParentFile().exists()) {
                    if (! local.getParentFile().mkdirs()) {
                        error(new IOException(
                                "Could not create cache directory " +
                                        local.getParent()));
                    }
                }

                if (isLocal()) {
                    // no need to download the local file.
                    return;
                }

                System.out.println("Downloading file " + local + " from " + url);


                File tempFile = new File(local.getParentFile(), local.getName() + ".tmp");


                try {
                    FileOutputStream out = new FileOutputStream(tempFile);
                    try {
                        InputStream in = openRemoteInputStream();
                        try {
                            byte[] buff = new byte[4096];
                            int len;
                            while ((len = in.read(buff, 0, 4096)) > 0) {
                                out.write(buff, 0, len);
                            }
                        } finally {
                            in.close();
                        }
                    } catch (FileNotFoundException e) {
                        // could not connect to the remote site
                        throw new RemoteConnectionException(getURI());
                    } catch (UnknownHostException e) {
                        // could not find the remote site
                        throw new RemoteConnectionException(getURI());
                    } finally {
                        out.close();
                    }

                    // Move the temp file so it replaces the original
                    File tempFile2 = null;
                    if (local.exists()) {
                        tempFile2 = new File(local.getParentFile(), local.getName() + ".tmp2");
                        if (tempFile2.exists()) {
                            if (! tempFile2.delete()) {
                                warn("could not delete temporary file " +
                                        tempFile2);
                            }
                        }
                        if (! local.renameTo(tempFile2)) {
                            // Could not perform the rename for some reason.
                            // Just overwrite the local file.
                            tempFile2 = null;
                            if (! local.delete()) {
                                attemptReload = true;
                                warn("could not overwrite cached file " +
                                    local + "; the cache will not be updated");
                                return;
                            }
                        }
                    }
                    if (tempFile.renameTo(local)) {
                        attemptReload = false;
                        resourceLoaded(this);
                    } else {
                        if (! tempFile.delete()) {
                            warn("Could not remove temporary file " + tempFile);
                        }
                        attemptReload = true;
                        error(new IOException(
                                LauncherBundle.message("cache.rename.error",
                                tempFile.toString(), local.toString())));
                    }
                    if (tempFile2 != null && tempFile2.exists()) {
                        // Ignore status message.
                        if (! tempFile2.delete()) {
                            warn("Could not remove temporary file " +
                                    tempFile2);
                        }
                    }

                } catch (IOException e) {
                    attemptReload = true;
                    if (! tempFile.delete()) {
                        warn("Could not remove temporary file: " +
                                tempFile);
                    }

                    if (! getLocal().exists()) {
                        // no local cached copy
                        throw new NoCacheException(getURI());
                    }

                    error(e);
                } finally {
                    //System.out.println("Downloaded " + url);
                }
            } catch (SecurityException e) {
                error(e);
            }
        }

        protected InputStream openRemoteInputStream() throws IOException {
            return getRealUrl().openStream();
        }

        protected File getLocal() {
            return local;
        }


        JSONObject toJSon() {
            JSONObject obj = new JSONObject();
            obj.put("url", url.toString());
            obj.put("local", getAsRelativeString(local));
            obj.put("type", type);
            obj.put("old", attemptReload);
            JSONArray kids = new JSONArray();
            for (URI kid: children) {
                kids.put(kid.toString());
            }
            obj.put("children", kids);
            return obj;
        }

        boolean addChild(Resource res) {
            URI kid = res.getURI();
            if (children.contains(kid)) {
                return false;
            }
            children.add(kid);
            return true;
        }


        @Override
        public boolean equals(Object o) {
            if (o == null || ! (o instanceof Resource)) {
                return false;
            }
            return ((Resource) o).getURI().equals(url);
        }


        @Override
        public int hashCode() {
            return url.hashCode();
        }


        @Override
        public String toString() {
            return (isStatic() ? "static " : "") + url + " (" + local + ")";
        }
    }
}

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

import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarOutputStream;
import java.util.jar.Pack200;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * We don't use the URLClassLoader because that can add all kinds of security
 * and encryption and signing requirements that we don't want or need.  Instead,
 * we'll limit the loading to just one jar file.
 *
 * @author Groboclown
 */
public class AppletClassLoader extends ClassLoader {
    private final Resource jarResource;
    private final Map<String, File> resources = new HashMap<String, File>();
    private Map<String, byte[]> cache;

    // This must be a class in the root of the launcher.  This is a strict
    // necessity for security.
    private static final String LAUNCHER_ROOT_PACKAGE =
            AppletClassLoader.class.getPackage().getName();


    public AppletClassLoader(Resource jarResource) {
        this.jarResource = jarResource;
    }


    public Resource getJarResource() {
        return jarResource;
    }


    @Override
    public final synchronized Class<?> loadClass(String name,
            boolean resolve)
            throws ClassNotFoundException {
        if (name.startsWith(LAUNCHER_ROOT_PACKAGE)) {
            throw new Java4kException("Can't allocate this secure class");
        }
        Class<?> ret;
        try {
            ret = super.loadClass(name, resolve);
        } catch (ClassNotFoundException e) {
            ret = loadClassFromJar(name, resolve);
        } catch (AccessControlException e) {
            // The parent classloader tried reading something it shouldn't have.
            ret = loadClassFromJar(name, resolve);
        }
        return ret;
    }


    private String getClassFileName(String className) {
        return '/' + className.replace('.', '/') + ".class";
    }



    @Override
    public final synchronized URL getResource(String name) {
        name = normalizeName(name);
        File f = resources.get(name);
        if (f == null) {
            final byte[] data = loadBytes(name);
            if (data == null) {
                return null;
            }
            f = AccessController.doPrivileged(new PrivilegedAction<File>() {
                @Override
                public File run() {
                    File ret = Cache.getInstance().
                            getClassLoaderResourceFileFor(jarResource);
                    try {
                        FileOutputStream out = new FileOutputStream(ret);
                        try {
                            out.write(data, 0, data.length);
                        } finally {
                            out.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                    return ret;
                }
            });
            if (f != null) {
                resources.put(name, f);
            }
        }
        return toURL(f);
    }


    private URL toURL(final File f) {
        if (f == null) {
            return null;
        }
        return AccessController.doPrivileged(new PrivilegedAction<URL>() {
            @Override
            public URL run() {
                try {
                    return f.toURI().toURL();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        });
    }


    @Override
    public final InputStream getResourceAsStream(String name) {
        name = normalizeName(name);

        byte[] data = loadBytes(name);
        //System.out.println("classloader: loading bytes for " + name +
        //        (data == null ? " (not found)" : ""));
        if (data == null) {
            return null;
        }
        return new ByteArrayInputStream(data);
    }

    private String normalizeName(String name) {
        while (name.length() > 0 && name.charAt(0) == '/') {
            name = name.substring(1);
        }
        return name;
    }


    @Override
    protected void finalize() throws Throwable {
        for (File f: resources.values()) {
            f.delete();
        }
        super.finalize();
    }


    private byte[] loadBytes(String name) {
        name = normalizeName(name);

        //System.out.println("Loading bytes for " + name);
        synchronized (this) {
            if (cache == null) {
                cache = Collections.unmodifiableMap(loadCache());
            }
        }
        return cache.get(name);
    }


    private Class<?> loadClassFromJar(String name, boolean resolve)
            throws ClassNotFoundException {
        String filename = getClassFileName(name);
        byte[] bytes = loadBytes(filename);
        if (bytes == null) {
            throw new ClassNotFoundException(name);
        }
        Class<?> c = defineClass(name, bytes, 0, bytes.length);
        if (resolve) {
            resolveClass(c);
        }
        return c;
    }


    private synchronized Map<String, byte[]> loadCache() {
        //System.out.println("Loading cache from " + jarResource);

        File tmpFile = Cache.getInstance().getJarOutputFileFor(jarResource);

        Map<String, byte[]> newCache = new HashMap<String, byte[]>();
        ZipFile zipFile = null;
        try {
            //System.out.println("write jar to " + tmpFile);
            writeJarResource(tmpFile);
            //System.out.println("load as zip file");
            zipFile = openZipFile(tmpFile);
            //System.out.println("validate zip file");
            validateZipFile(tmpFile, zipFile);

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                //System.out.println("Found entry " + name);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                InputStream in = zipFile.getInputStream(entry);
                copyStream(in, out);
                newCache.put(name, out.toByteArray());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (zipFile != null) {
                    zipFile.close();
                }
            } catch (IOException e) {
                // ignore the close exception
            } finally {
                tmpFile.delete();
            }
        }
        return newCache;
    }

    private void validateZipFile(File tmpFile, ZipFile zipFile) {
        //if (tmpFile.length() > 4096) {
        //    System.out.println("file too big: " + tmpFile.length() +
        //            ". Might be signed?");
        //}
    }


    private void writeJarResource(File tmpFile) throws IOException {
        FileOutputStream out = new FileOutputStream(tmpFile);
        try {
            InputStream in = jarResource.read();
            copyStream(in, out);
        } finally {
            out.close();
        }
    }


    public ZipFile openZipFile(final File tempFile)
            throws IOException {
        try {
            return new ZipFile(tempFile);
        } catch (ZipException e) {
            // Try as a pack200 file
            return openPack200File(tempFile);
        }
    }


    private ZipFile openPack200File(File f) throws IOException {
        try {
            return openPack200File(new GZIPInputStream(
                    new FileInputStream(f)));
        } catch (IOException e) {
            e.printStackTrace();

            // assume the file is not actually a gzip file
            return openPack200File(new FileInputStream(f));
        }
    }

    /**
     * Closes the input stream when it returns.
     *
     * @param in the pack200 file as an input stream, possibly a gzip stream.
     * @return the pack200 file as a zip file.
     * @throws IOException
     */
    private ZipFile openPack200File(InputStream in) throws IOException {
        try {
            Pack200.Unpacker unpacker = Pack200.newUnpacker();
            File tmp = Cache.getInstance().getJarOutputFileFor(jarResource);
            JarOutputStream out = new JarOutputStream(new FileOutputStream(tmp));
            try {
                unpacker.unpack(in, out);
            } finally {
                out.close();
            }

            return new ZipFile(tmp);
        } finally {
            in.close();
        }
    }

    private static void copyStream(InputStream in, OutputStream out) throws IOException {
        try {
            try {
                byte[] buff = new byte[4096];
                int len;
                while ((len = in.read(buff, 0, 4096)) > 0) {
                    out.write(buff, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }
}

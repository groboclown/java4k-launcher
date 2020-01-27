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

import net.javagaming.java4k.launcher.AppletClassLoader;
import net.javagaming.java4k.launcher.GameConfiguration;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.GameLifeCycleRunner;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.Security;
import net.javagaming.java4k.launcher.applet.AppletLifeCycleRunner;
import net.javagaming.java4k.launcher.cache.Resource;

import java.applet.Applet;
import java.awt.Dimension;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.AccessControlException;
import java.util.HashMap;
import java.util.Map;

/**
 * Container for the parameters specified in the Applet tag of an HTML file.
 *
 * @author Groboclown
 */
public class AppletConfiguration implements GameConfiguration {
    private static final int MAX_WIDTH = 1000;
    private static final int MAX_HEIGHT = 1000;

    private final GameDetail detail;
    private int width;
    private int height;
    private URL baseUrl;
    private Resource documentBase;
    private final Resource archive;
    private final String className;
    private final ThreadGroup threadGroup;

    private Map<String, String> parameters = new HashMap<String, String>();

    private AppletClassLoader classLoader;


    public AppletConfiguration(String className,
            Resource jar, Resource documentBase, int width, int height,
            ThreadGroup parentAppletThreadGroup, GameDetail detail) {
        this.detail = detail;
        threadGroup = new ThreadGroup(
                parentAppletThreadGroup, "Applet " + className);

        setWidth(width);
        setHeight(height);

        this.className = className;
        this.archive = jar;
        setDocumentBaseUrl(documentBase);
    }

    @Override
    public boolean isHost(String hostname, int port) {
        // For security checks
        // For now only look at the archive URL
        URI archiveUrl = getDocumentBase().getURI();
        return Security.isHost(archiveUrl, hostname, port);
    }

    @Override
    public GameLifeCycleRunner createGameLifeCycleRunner(
            LauncherManager launcherManager) {
        return new AppletLifeCycleRunner(detail, this, launcherManager);
    }

    public Applet loadApplet() throws IOException {
        if (className == null) {
            throw new IOException("Never set class name");
        }
        if (classLoader == null) {
            classLoader = new AppletClassLoader(archive);
        }
        try {
            Class<?> c = classLoader.loadClass(className);
            if (Applet.class.isAssignableFrom(c)) {
                Object applet = c.newInstance();
                return Applet.class.cast(applet);
            }
        } catch (AccessControlException e) {
            throw e;
        } catch (SecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } catch (LinkageError e) {
            throw new IOException(e);
        }
        throw new IOException("not an applet class: "
                + getClassName());
    }

    @Override
    public boolean isClassLoader(ClassLoader cl) {
        return classLoader != null && classLoader.equals(cl);
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        if (width < 0 || width > MAX_WIDTH) {
            throw new IllegalArgumentException("bad width: " + width);
        }
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        if (height < 0 || height > MAX_HEIGHT) {
            throw new IllegalArgumentException("bad height: " + height);
        }
        this.height = height;
    }

    public Dimension getSize() {
        return new Dimension(getWidth(), getHeight());
    }

    /**
     * @return the base URL where the html page was loaded from.
     */
    public URL getBaseUrl() {
        return baseUrl;
    }

    public void setDocumentBaseUrl(Resource documentBase) {
        try {
            this.documentBase = documentBase;
            URI documentBaseURL = documentBase.getURI();
            this.baseUrl = new URL(documentBaseURL.getScheme(),
                    documentBaseURL.getHost(), documentBaseURL.getPort(),
                    getParentFile(documentBaseURL.getPath()));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public Resource getDocumentBase() {
        return documentBase;
    }

    /**
     * @return the "ARCHIVE" attribute of the Applet tag
     */
    public Resource getArchive() {
        return archive;
    }

    /**
     * @return the "CODE" attribute of the Applet tag
     */
    public String getClassName() {
        return className;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    @Override
    public void loadCache() throws IOException {
        getArchive().read().close();
    }

    @Override
    public boolean isWebApp() {
        return false;
    }

    private String getParentFile(String file) {
        if (file.endsWith("/")) {
            return file;
        }
        return file.substring(0, file.lastIndexOf('/'));
    }
}

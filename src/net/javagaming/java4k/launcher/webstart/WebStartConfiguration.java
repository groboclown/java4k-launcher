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
package net.javagaming.java4k.launcher.webstart;

import net.javagaming.java4k.launcher.AppletClassLoader;
import net.javagaming.java4k.launcher.GameConfiguration;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.GameLifeCycleRunner;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.Security;
import net.javagaming.java4k.launcher.cache.Resource;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.security.AccessControlException;

/**
 * @author Groboclown
 */
public class WebStartConfiguration implements GameConfiguration {
    private final String className;
    private final Resource jar;
    private final Resource documentBase;
    private final ThreadGroup threadGroup;
    private final GameDetail detail;
    private final AppletClassLoader classLoader;

    public WebStartConfiguration(String className,
            Resource jar, Resource documentBase,
            ThreadGroup parentAppletThreadGroup,
            GameDetail detail) {
        this.className = className;
        this.jar = jar;
        this.documentBase = documentBase;
        this.threadGroup = new ThreadGroup(
                parentAppletThreadGroup, "WebStart " + className);
        this.detail = detail;

        classLoader = new AppletClassLoader(jar);
    }


    @Override
    public boolean isHost(String hostname, int port) {
        // For security checks
        // For now only look at the archive URL
        URI archiveUrl = documentBase.getURI();
        return Security.isHost(archiveUrl, hostname, port);
    }

    @Override
    public GameLifeCycleRunner createGameLifeCycleRunner(
            LauncherManager launcherManager) {
        return new WebStartRunner(detail, this, launcherManager);
    }

    @Override
    public boolean isClassLoader(ClassLoader cl) {
        return classLoader.equals(cl);
    }

    @Override
    public ThreadGroup getThreadGroup() {
        return threadGroup;
    }

    @Override
    public void loadCache() throws IOException {
        if (jar != null) {
            jar.read().close();
        } else {
            throw new IOException("No jar file defined");
        }
    }

    @Override
    public boolean isWebApp() {
        return true;
    }

    public Method loadMainMethod() throws IOException {
        if (className == null) {
            throw new IOException("Never set class name for " +
                    detail.getName());
        }
        try {
            Class<?> c = classLoader.loadClass(className);
            Method m = c.getDeclaredMethod("main", String[].class);

            int mods = m.getModifiers();
            if (Modifier.isStatic(mods) && Modifier.isPublic(mods)) {
                return m;
            }
            throw new IOException("main method of " + className +
                    " is not public static");
        } catch (AccessControlException e) {
            throw e;
        } catch (SecurityException e) {
            throw e;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        } catch (LinkageError e) {
            throw new IOException(e);
        }
    }
}

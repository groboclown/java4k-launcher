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

import net.javagaming.java4k.launcher.Java4kException;

import java.applet.Applet;
import java.applet.AppletContext;
import java.applet.AudioClip;
import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * @author Groboclown
 */
public class DefaultAppletContext implements AppletContext {

    @Override
    public AudioClip getAudioClip(URL url) {
        throw new Java4kException("Cannot load external resources: " + url);
    }

    @Override
    public Image getImage(URL url) {
        throw new Java4kException("Cannot load external resources: " + url);
    }

    @Override
    public Applet getApplet(String name) {
        throw new Java4kException("Cannot load external resources: " + name);
    }

    @Override
    public Enumeration<Applet> getApplets() {
        return (new Vector<Applet>()).elements();
    }

    @Override
    public void showDocument(URL url) {
        throw new Java4kException("Cannot load external resources: " + url);
    }

    @Override
    public void showDocument(URL url, String target) {
        throw new Java4kException("Cannot load external resources: " + url);
    }

    @Override
    public void showStatus(String status) {

        // TODO we may want this implemented.

    }

    @Override
    public void setStream(String key, InputStream stream) throws IOException {
        throw new Java4kException("Not supported (key " + key + ")");
    }

    @Override
    public InputStream getStream(String key) {
        throw new Java4kException("Not supported (key " + key + ")");
    }

    @Override
    public Iterator<String> getStreamKeys() {
        List<String> list = Collections.emptyList();
        return list.iterator();
    }

}

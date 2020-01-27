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

import net.javagaming.java4k.launcher.AbstractGameDetail;
import net.javagaming.java4k.launcher.DefaultGameDetailListResourceReader;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.applet.AppletGameDetail;
import net.javagaming.java4k.launcher.cache.Cache;
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.progress.ProgressWorker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.imageio.ImageIO;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Processes a GameDescription resource to produce GameDetail instances
 * for JNLP files.  JNLP files can define either applets or WebStart apps.
 *
 * @author Groboclown
 */
public class JnlpDetailResourceReader implements
        DefaultGameDetailListResourceReader.SourceTypeResourceReader {

    public static final JnlpDetailResourceReader READER = new JnlpDetailResourceReader();

    private static final int APPLET = 0;
    private static final int WEBSTART = 1;
    private static final int UNKNOWN = 2;

    @Override
    public void processLoadedResource(Resource jnlp,
            ProgressWorker.Publisher<GameDetail> controller,
            GameDescription source) throws Exception {
        Document doc = loadXml(jnlp);
        controller.advanceBy(30);
        Element top = doc.getDocumentElement();
        if (!"jnlp".equals(top.getTagName().toLowerCase())) {
            throw new JnlpFormatException(jnlp.getURI());
        }




        /* General format
<jnlp spec="1.0" codebase="http://www.java4k.com/games/1/" href="4K-tris.jnlp">
<information>
<title>4K-tris</title>
<vendor>pb33</vendor>
<homepage href="http://sweb.cz/petrblahos/4ktris/"/>
<description>Now this is a great 4K version of tetris! The movement is smooth, there's a semi-animated background, and classic gameplay that seems to be flawless. There's also different levels of difficulty!</description>
</information>
<resources>
<j2se href="http://java.sun.com/products/autodl/j2se" version="1.4+"/>
<jar href="jar/4K-tris.jar"/>
</resources>
<application-desc main-class="package1.package2.MyMainClass" />
</jnlp>
        */

        Resource jar;

        // For now, just load the resource and main class, and ignore the
        // other details.
        {
            URI baseUrl = Cache.uri(top.getAttribute("codebase"));
            String relJar = null;

            // The main class is in the first jar, or the jar with the attribute 'main="true"'
            // We will just look for one jar file.

            NodeList jarList = top.getElementsByTagName("jar");
            if (jarList != null) {
                for (int i = 0; i < jarList.getLength(); ++i) {
                    Element el = (Element) jarList.item(i);
                    String href = el.getAttribute("href");
                    if (href != null) {
                        if (relJar != null) {
                            throw new IOException(
                                    "too many jar definitions in JNLP file");
                        }
                        relJar = href;
                    }
                }
            }
            if (relJar == null) {
                throw new IOException("no jar file defined in JNLP file");
            }

            if (! relJar.toLowerCase().startsWith("http://")) {
                // not a relative jar file - see Grasshopper4k (2009)
                relJar = baseUrl.toString() + relJar;
            }

            jar = jnlp.getChildResource(Cache.uri(relJar));
            //System.out.println("JNLP jar: baseURL: " + baseUrl +
            //        "; jar file: " + relJar + "; final URL: " +
            //        jar);

        }
        controller.advanceBy(2);

        int type = UNKNOWN;
        Dimension size = null;
        String mainClass = null;
        {
            NodeList applicationList = top.getElementsByTagName("application-desc");
            if (applicationList != null) {
                for (int i = 0; i < applicationList.getLength(); ++i) {
                    Element el = (Element) applicationList.item(i);
                    String name = el.getAttribute("main-class");
                    if (name != null) {
                        if (mainClass != null) {
                            throw new IOException(
                                    "too many main class definitions in JNLP file");
                        }
                        mainClass = name;
                        type = WEBSTART;
                    }
                }
            }
        }
        controller.advanceBy(2);
        if (mainClass == null || mainClass.length() <= 0) {
            NodeList appletList = top.getElementsByTagName("applet-desc");
            if (appletList != null) {
                for (int i = 0; i < appletList.getLength(); ++i) {
                    // NOTE: in this case, the thing acts like an applet,
                    // as this tag also includes the width and height

                    Element el = (Element) appletList.item(i);
                    String name = el.getAttribute("main-class");
                    if (name != null) {
                        if (mainClass != null) {
                            throw new IOException(
                                "too many main class definitions in JNLP file");
                        }
                        mainClass = name;
                        type = APPLET;

                        String width = el.getAttribute("width");
                        String height = el.getAttribute("height");

                        if (width != null && height != null) {
                            try {
                                size = new Dimension(
                                        Integer.parseInt(width),
                                        Integer.parseInt(height));
                            } catch (NumberFormatException e) {
                                System.err.println(
                                    "incorrect width/height for webstart applet: " +
                                    width + "x" + height);
                            }
                        }

                        // could be parameters, but we won't load those now.
                    }
                }
            }
        }
        controller.advanceBy(1);

        boolean readJar = false;

        if (mainClass == null || mainClass.length() <= 0) {
            // need to load it from the mainifest "Main-Class" value.
            mainClass = loadManifestMainClass(jar);
            readJar = true;
            if (mainClass == null  || mainClass.length() <= 0) {
                throw new IOException("JNLP file (" + jnlp.getURI() +
                        ") did not declare the main class, nor did the jar file's manifest (" +
                        jar.getURI() + ")");
            }
            type = WEBSTART;
            controller.advanceBy(10);
        }


        AbstractGameDetail gd;
        switch (type) {
            case APPLET:
                gd = new AppletGameDetail(source, jnlp);
                break;
            case WEBSTART:
                gd = new WebStartGameDetail(source, jnlp);
                break;
            default:
                throw new IOException(
                        "jnlp did not define either a webapp or applet");
        }
        gd.setDocumentBase(jnlp);
        gd.setClassName(mainClass);
        gd.setSize(size);

        // force a reading of the resources
        if (! readJar) {
            jar.read().close();
        }
        gd.setJar(jar);
        controller.advanceBy(5);

        if (source.getIconResource() != null) {
            InputStream in = source.getIconResource().read();
            try {
                gd.setIcon(ImageIO.read(in));
            } finally {
                in.close();
            }
        }
        controller.publish(gd);
        controller.advanceTo(100);
    }

    private String loadManifestMainClass(Resource jar) throws IOException {
        InputStream in = jar.read();

        // Note: JarInputStream usually doesn't work as written.  If the
        // manifest file is not the first or second entry, it will return
        // null for the manifest.  Instead, we must search for it explicitly.

        try {
            ZipInputStream jin = new ZipInputStream(in);
            ZipEntry entry;
            while ((entry = jin.getNextEntry()) != null) {
                if (JarFile.MANIFEST_NAME.equalsIgnoreCase(entry.getName())) {
                    Manifest manifest = new Manifest();
                    manifest.read(jin);
                    Attributes attribs = manifest.getMainAttributes();
                    if (attribs != null && attribs.containsKey(Attributes.Name.MAIN_CLASS)) {
                        return attribs.get(Attributes.Name.MAIN_CLASS).toString();
                    }
                }
            }
        } finally {
            in.close();
        }
        return null;
    }


    private Document loadXml(Resource r) throws IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);
        InputStream in = r.read();
        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            return db.parse(in);
        } catch (SAXException e) {
            // See 4k Maze (2009)
            e.printStackTrace();
            throw new JnlpFormatException(r.getURI());
        } catch (ParserConfigurationException e) {
            throw new IOException(e);
        } finally {
            in.close();
        }
    }

}

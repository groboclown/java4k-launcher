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

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.Dimension;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for handling web pages.
 *
 * @author Groboclown
 */
public class WebUtil {

    public static HTMLDocument loadWebPage(Resource url) throws IOException {
        return loadWebPage(url.read(Resource.DEFAULT_ENCODING));
    }

    public static HTMLDocument loadWebPage(Reader page) throws IOException {
        try {
            HTMLEditorKit kit = new HTMLEditorKit();
            HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
            doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
            try {
                kit.read(page, doc, 0);
            } catch (BadLocationException e) {
                throw new IOException(e);
            }

            return doc;
        } finally {
            page.close();
        }
    }


    public static HTMLDocument toWebPage(String text) throws IOException {
        HTMLEditorKit kit = new HTMLEditorKit();
        HTMLDocument doc = (HTMLDocument) kit.createDefaultDocument();
        doc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        try {
            kit.read(new StringReader(text), doc, 0);
        } catch (BadLocationException e) {
            throw new IOException(e);
        }

        return doc;
    }



    public static boolean isEndTag(Element tag) {
        String endTag = getUncheckedAttribute(tag, HTML.Attribute.ENDTAG);
        boolean ret = endTag != null && "true".equalsIgnoreCase(endTag);
        return ret;
    }


    public static Integer getIntAttribute(Element elem, HTML.Attribute attribute) {
        String value = getUncheckedAttribute(elem, attribute);
        if (value == null) {
            return null;
        }
        try {
            return new Integer(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            System.err.println("expected int " + attribute +
                    " attribute, found " + value);
            return null;
        }
    }



    public static String getUncheckedAttribute(Element elem, HTML.Attribute attribute) {
        Object value = elem.getAttributes().getAttribute(attribute);
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public static String getAttribute(Element elem, HTML.Attribute attribute) throws IOException {
        Object value = elem.getAttributes().getAttribute(attribute);
        if (value == null) {
            throw new IOException("Applet element does not define required attribute " + attribute);
        }
        return value.toString();
    }


    public static String getText(Element elem) {
        if (elem == null) {
            return null;
        }
        Document doc = elem.getDocument();
        if (doc == null) {
            return null;
        }
        try {
            return doc.getText(elem.getStartOffset(), elem.getEndOffset());
        } catch (BadLocationException e) {
            throw new IllegalStateException("Unkown document location", e);
        }
    }

    public static boolean isStartTag(Element elem, HTML.Tag tagName) {
        return elem.getName().equalsIgnoreCase(tagName.toString()) &&
                !isEndTag(elem);
    }

    public static boolean isAppletTag(Element elem) {
        return isStartTag(elem, HTML.Tag.APPLET);
    }


    public static void loadAppletTag(
            AbstractGameDetail builder, Resource sourcePage,
            Element applet) throws IOException {

        builder.setDocumentBase(sourcePage);
        builder.setParameters(getAppletParameters(applet));

        String code = getAttribute(applet, HTML.Attribute.CODE);
        if (code.endsWith(".class")) {
            code = code.substring(0, code.length() - 6);
        }
        builder.setClassName(code);

        try {
            int width = WebUtil.getIntAttribute(applet, HTML.Attribute.WIDTH);
            int height = WebUtil.getIntAttribute(applet, HTML.Attribute.HEIGHT);

            builder.setSize(new Dimension(width, height));
        } catch (NumberFormatException e) {
            // ignore
        }

        // ignoring code base

        String archive = getAttribute(applet, HTML.Attribute.ARCHIVE);

        URI base = builder.getDocumentBase().getURI();
        String file = base.getPath();
        int pos = file.lastIndexOf('/');
        if (pos > 0) {
            file = file.substring(0, pos + 1);
        } else {
            file = "/";
        }
        if (! file.endsWith("/")) {
            file = file + '/';
        }
        Resource jar = builder.getDocumentBase().getChildResource(Cache.uri(
                base.getScheme(), base.getHost(), base.getPort(),
                file + archive));

        builder.setJar(jar);
    }

    private static Map<String, String> getAppletParameters(Element appletTag) {
        Map<String, String> ret = new HashMap<String, String>();
        // The applet tag is parsed such that it puts the
        // <param> tags into the applet attributes.
        AttributeSet attributes = appletTag.getAttributes();
        Enumeration<?> e = attributes.getAttributeNames();
        while (e.hasMoreElements()) {
            Object name = e.nextElement();
            Object value = attributes.getAttribute(name);
            if (name instanceof String && value instanceof String) {
                // parameter
                ret.put(name.toString(), value.toString());
            }
        }
        return ret;
    }

}

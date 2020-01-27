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

import net.javagaming.java4k.launcher.cache.Resource;

import java.awt.Dimension;
import java.awt.Image;
import java.util.Map;

/**
 * Mutable GameDetail.
 *
 * @author Groboclown
 */
public abstract class AbstractGameDetail implements GameDetail {
    private final GameDescription source;
    private final Resource detailsSource;
    private String name;
    private String id;
    private Image icon;
    private Resource jar;
    private Dimension size;
    private Resource documentBase;
    private String className;
    private Map<String, String> parameters;

    public AbstractGameDetail(GameDescription source, Resource detailsSource) {
        this.name = source.getName();
        this.source = source;
        this.id = source.getId();
        this.detailsSource = detailsSource;
    }


    @Override
    public GameDescription getSource() {
        return source;
    }


    public void setIcon(Image icon) {
        this.icon = icon;
    }

    @Override
    public Image getIcon() {
        return icon;
    }

    public void setJar(Resource jar) {
        this.jar = jar;
    }

    @Override
    public Resource getJar() {
        return jar;
    }

    public void setSize(Dimension size) {
        this.size = size;
    }

    @Override
    public Dimension getSize() {
        return size;
    }

    public void setDocumentBase(Resource documentBase) {
        this.documentBase = documentBase;
    }

    @Override
    public Resource getDocumentBase() {
        return documentBase;
    }

    @Override
    public Resource getDetailsSource() {
        return detailsSource;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String getClassName() {
        return className;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Map<String, String> getParameters() {
        return parameters;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

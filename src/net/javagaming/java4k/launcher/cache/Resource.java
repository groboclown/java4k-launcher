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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;

/**
 * The general resource managed by the {@link Cache}.
 *
 * @author Groboclown
 */
public interface Resource {
    public static final String DEFAULT_ENCODING = "UTF-8";


    /**
     * Is this resource available for immediate use?
     *
     * @return true if available, or false if not ready.
     */
    public boolean isAvailable();


    /**
     * Is this resource static, such that it doesn't need to be reloaded once
     * downloaded.
     *
     * @return true if it only needs to be downloaded once, or false if it
     *      may need to be updated.
     */
    public boolean isStatic();


    /**
     * Performs a quick check to see if the resource is using an out-of-date
     * cached copy, or the most recently known copy.  This state should
     * return false if {@link #isAvailable()} returns false, and it should
     * always return true if {@link #isStatic()} returns true and it's
     * available.
     * <p/>
     * If this returns false and {@link #isAvailable()} returns true, then
     * that means that the server (probably) has a more recent version, but
     * the resource either hasn't, or can't, connect to the server to get
     * the latest version.
     *
     * @return true if the resource is up-to-date.
     */
    public boolean isUpToDate();


    /**
     *
     * @return the data for this resource.
     */
    public InputStream read() throws IOException;


    /**
     * Read the string in the given encoding.
     *
     * @param encoding the encoding type
     * @return the encoded string
     * @throws IOException
     */
    public Reader read(String encoding) throws IOException;


    public String readAsString(String encoding) throws IOException;


    /**
     * Flush the cached file, and all child files.
     */
    public void flush();


    /**
     * Get a child resource.  Child resources are flushed when the parents
     * are flushed, and they inherit the "isStatic" flag.
     * <p />
     * This method helps to remove a heavy dependency upon the singleton
     * {@link Cache} object.
     *
     * @param url
     * @return the child resource
     */
    public Resource getChildResource(URI url);


    /**
     * Get a child resource, but with a potentially different static usage
     * from the parent.
     *
     * @param url
     * @return the child resource
     */
    public Resource getChildResource(URI url, boolean isStatic);


    /**
     * Usees a URI instead of a URL because it avoids a security check, which
     * can be really slow.
     *
     * @return the URI for the remote resource.
     */
    public URI getURI();
}

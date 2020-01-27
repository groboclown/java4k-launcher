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

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Describes a game (applet or webstart).
 *
 * @author Groboclown
 */
public interface GameDescription {
    String getId();

    public static enum SourceType {
        /**
         * HTML containing 0 or more applet tags
         */
        HTML,

        /**
         * JNLP defined applet or web app
         */
        JNLP
    }



    /**
     * Must not be dynamically loaded.
     *
     * @return a plain text name of the game.
     */
    public String getName();

    /**
     * Must not be dynamically loaded.
     *
     * @return the name of the author of the applet, or <tt>null</tt> if
     *      not known.
     */
    public String getAuthor();

    /**
     *
     * @return the primary contact information for the author
     *      (email, webpage, etc), or <tt>null</tt> if not known.
     */
    public String getAuthorContact();

    /**
     * Should not be dynamically loaded.
     *
     * @return an HTML-formatted string describing the applet.  This is
     *      usually a summary of the game.
     */
    public String getDescription();


    /**
     * The instructions for the game.
     *
     * @return the full details, plain-text formatted.
     */
    public String getInstructions();


    /**
     *
     * @return the resource that stores the details for this game.
     */
    public Resource getGameDetailSource();


    /**
     *
     * @return the date this applet was "submitted" or otherwise made
     *      publicly available.  Returns <tt>null</tt> if not available.
     */
    public Date getSubmissionDate();


    public Map<String,String> getServerInfo();


    /**
     * Can be dynamically loaded
     *
     *
     * @return an icon for this applet, or <tt>null</tt> if none is available.
     */
    public Resource getIconResource();


    /**
     *
     * @return all user comments for this applet, or <tt>null</tt> if they
     *      are not available.
     */
    public List<UserComment> getUserComments();


    public SourceType getSourceType();
}

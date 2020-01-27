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

/**
 * Handles the submission of comments, and authentication.  This takes an
 * unusual role where the login method can start a GUI interface, whereas the
 * rest of the class is intended to be non-user showing.
 *
 * @author Groboclown
 */
public interface CommentSubmission {

    /**
     * Called when this comment submission instance's active state changes;
     * essentially, when a new category is loaded, this is called.
     *
     * @param state true if this current instance is enabled, or false if it
 *                  is being turned off.
     */
    public void setActive(boolean state);

    public boolean isAuthenticated();

    public void logout();

    /**
     * This is intended to spawn a GUI dialog managed by the submission class.
     * Note that the game manager will search out Window objects and dispose
     * them when games end, so any dialogs used for this must assume that the
     * dialog will be recreated for each call.
     */
    public void login();

    public String getUserId();

    public void addAuthenticationListener(AuthenticationListener listener);

    public void removeAuthenticationListener(AuthenticationListener listener);

    public void submitComment(GameDetail detail, String text);
}

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

import javax.swing.JFrame;
import java.awt.Window;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Central manager of managers, and manages some centralized UI data tracking.
 *
 * @author Groboclown
 */
public class LauncherManager {
    private final ProgressManager workerManager;
    private final GameManager gameManager;
    private final ErrorMessageManager errorMessageManager;
    private final JFrame mainFrame;
    private final List<AuthenticationListener> authenticationListeners =
            new ArrayList<AuthenticationListener>();
    private CommentSubmission commentSubmission;
    private final AuthenticationListener localAuthListener = new LocalAuthListener();

    public LauncherManager(ProgressPanel progressPanel, Java4kLauncher mainFrame) {
        Cache.getInstance().setLauncherManager(this);
        progressPanel.setLauncherManager(this);
        workerManager = new ProgressManager(progressPanel, this);
        gameManager = new GameManager(this);
        errorMessageManager = new ErrorMessageManager(this);
        this.mainFrame = mainFrame;
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    public ProgressManager getWorkerManager() {
        return workerManager;
    }

    /**
     * Used to discover all the applet and jnlp spawned windows, so that they
     * can be correctly closed or monitored.
     *
     * @return all windows not managed by the managers.
     */
    public Collection<Window> getAllUnmanagedWindows() {
        Set<Window> owned = new HashSet<Window>(
                errorMessageManager.getManagedDialogs());
        owned.add(getMainFrame());
        List<Window> ret = new ArrayList<Window>();
        for (Window w: Window.getWindows()) {
            // For some reason, this is picking up the managed dialog as being
            // different.
            if (! isOwned(w, owned)) {
                ret.add(w);
            }
        }
        return ret;
    }

    private boolean isOwned(Window w, Set<Window> owned) {
        //System.err.println("Is " + w + " managed?");
        for (Window ow: owned) {
            //System.err.println(" - is it this owned one? " + ow);
            if (w.equals(ow)) {
                //System.err.println(" - identically yes");
                return true;
            }
            for (Window cw: w.getOwnedWindows()) {
                if (cw.equals(ow)) {
                    //System.err.println(" - owned is a child of it, so yes");
                    return true;
                }
            }

            //System.err.println(" - no");
        }
        return false;
    }


    public GameManager getGameManager() {
        return gameManager;
    }

    public ErrorMessageManager getErrorMessageManager() {
        return errorMessageManager;
    }

    /**
     * Safely stop the launcher.  Does not perform a System.exit call.
     */
    public void shutdown() {
        gameManager.shutdownManager();
        workerManager.shutdownManager();
        errorMessageManager.shutdownManager();
        mainFrame.dispose();
    }

    public void addAuthenticationListener(AuthenticationListener listener) {
        if (listener != null) {
            this.authenticationListeners.add(listener);
        }
    }

    public void removeAuthenticationListener(AuthenticationListener listener) {
        this.authenticationListeners.remove(listener);
    }


    public CommentSubmission getCommentSubmission() {
        return commentSubmission;
    }


    void setCommentSubmission(CommentSubmission commentSubmission) {
        if (this.commentSubmission != null) {
            this.commentSubmission.removeAuthenticationListener(
                    localAuthListener);
        }
        this.commentSubmission = commentSubmission;
        commentSubmission.addAuthenticationListener(localAuthListener);
        localAuthListener.onCommentSubmissionChange(commentSubmission);
    }



    private class LocalAuthListener implements AuthenticationListener {
        @Override
        public void onUserAuthenticated(CommentSubmission submission) {
            for (AuthenticationListener listener: authenticationListeners) {
                listener.onUserAuthenticated(submission);
            }
        }

        @Override
        public void onUserLogoff(CommentSubmission submission) {
            for (AuthenticationListener listener: authenticationListeners) {
                listener.onUserLogoff(submission);
            }
        }

        @Override
        public void onCommentSubmissionChange(CommentSubmission submission) {
            for (AuthenticationListener listener: authenticationListeners) {
                listener.onCommentSubmissionChange(submission);
            }
        }
    }
}

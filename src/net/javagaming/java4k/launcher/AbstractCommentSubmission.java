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

import net.javagaming.java4k.launcher.progress.ProgressWorker;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Groboclown
 */
public abstract class AbstractCommentSubmission implements CommentSubmission {
    private final LauncherManager launcherManager;
    private final List<AuthenticationListener> listeners =
            new ArrayList<AuthenticationListener>();
    private boolean authenticated = false;
    private String userId = "";

    protected AbstractCommentSubmission(LauncherManager launcherManager) {
        this.launcherManager = launcherManager;
    }


    protected abstract String getSiteName();

    /**
     *
     * @param user userid
     * @param password password
     */
    protected abstract void sendAuthenticate(String user, String password)
        throws IOException;

    protected abstract void sendLogout()
        throws IOException;

    protected abstract void sendSubmit(GameDetail detail, String text)
        throws IOException;


    @Override
    public void setActive(boolean state) {
        for (AuthenticationListener listener: listeners) {
            listener.onCommentSubmissionChange(this);
        }
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public void logout() {
        if (isAuthenticated()) {
            runLogout();
        }
    }

    @Override
    public void login() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        JLabel namel = new JLabel(LauncherBundle.getString(
                "comments.login.userid"));
        namel.setHorizontalTextPosition(SwingConstants.RIGHT);
        namel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        JLabel passwdl = new JLabel(LauncherBundle.getString(
                "comments.login.password"));
        passwdl.setHorizontalTextPosition(SwingConstants.RIGHT);
        passwdl.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));


        final JTextField userField = new JTextField(userId);
        JTextField passwordField = new JPasswordField("");

        JPanel labelp = new JPanel(new GridLayout(0, 1));
        JPanel fieldp = new JPanel(new GridLayout(0, 1));

        labelp.add(namel);
        labelp.add(passwdl);

        fieldp.add(userField);
        fieldp.add(passwordField);

        panel.add(labelp);
        panel.add(fieldp);

        String[] options = {
                LauncherBundle.getString("comments.login.login"),
                LauncherBundle.getString("comments.login.cancel")
        };
        userField.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
                JComponent component = event.getComponent();
                component.requestFocusInWindow();
                component.removeAncestorListener(this);
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                // ignore
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
                // ignore
            }
        });


        // Make our own dialog inedpendent of the error message guy.
        // Note that this means that webstart killing will kill this dialog,
        // too.
        int result = JOptionPane.showOptionDialog(
                launcherManager.getMainFrame(), panel,
                LauncherBundle.message("comments.login.title", getSiteName()),
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null, options,
                options[0]);
        if (result == JOptionPane.OK_OPTION) {
            runAuthenticate(userField.getText(), passwordField.getText());
        }
    }

    @Override
    public String getUserId() {
        return userId;
    }

    @Override
    public void addAuthenticationListener(AuthenticationListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeAuthenticationListener(AuthenticationListener listener) {
        listeners.remove(listener);
    }

    @Override
    public void submitComment(GameDetail detail, String text) {
        if (! isAuthenticated()) {
            return;
        }
        runSubmitComment(detail, text);
    }


    // -----------------------------------------------------------------------
    // Run these in worker threads



    private void runSubmitComment(final GameDetail detail, final String text) {
        launcherManager.getWorkerManager().startWorker("Submit Comment",
                ActionSource.SUBMIT_COMMENT, false, new ProgressWorker<Object>() {
            @Override
            public void doInBackground(Publisher<Object> controller) throws Exception {
                sendSubmit(detail, text);
            }
        });
    }


    private void runAuthenticate(final String username, final String password) {
        launcherManager.getWorkerManager().startWorker(
                LauncherBundle.message("comments.login.title", getSiteName()),
                ActionSource.SUBMIT_COMMENT, false, new ProgressWorker<Object>() {
            @Override
            public void doInBackground(Publisher<Object> controller) throws Exception {
                sendAuthenticate(username, password);
                authenticated = true;
                userId = username;
                for (AuthenticationListener listener: listeners) {
                    listener.onUserAuthenticated(
                            AbstractCommentSubmission.this);
                }
            }
        });
    }


    private void runLogout() {
        launcherManager.getWorkerManager().startWorker(
                LauncherBundle.message("comments.logout.title", getSiteName()),
                ActionSource.SUBMIT_COMMENT, false, new ProgressWorker<Object>() {
            @Override
            public void doInBackground(Publisher<Object> controller) throws Exception {
                sendLogout();
                authenticated = false;
                for (AuthenticationListener listener: listeners) {
                    listener.onUserLogoff(AbstractCommentSubmission.this);
                }
            }
        });
    }
}

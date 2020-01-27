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
package net.javagaming.java4k.launcher.ui;

import net.javagaming.java4k.launcher.ActionSource;
import net.javagaming.java4k.launcher.AuthenticationListener;
import net.javagaming.java4k.launcher.CommentSubmission;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * A panel that allows submitting comments for a game.
 *
 * @author Groboclown
 */
public class AddCommentPanel extends JPanel implements AuthenticationListener {
    private final LauncherManager launcherManager;
    private final JLabel loginState;
    private final JButton loginButton;
    private final JTextArea comment;
    private final JPanel commentTextPanel;
    private final JButton submitButton;
    private CommentSubmission sub;
    private GameDetail detail;

    public AddCommentPanel(LauncherManager launcherManager) {
        super(new BorderLayout());
        this.launcherManager = launcherManager;
        launcherManager.addAuthenticationListener(this);

        JPanel loginStatePanel = new JPanel(new BorderLayout());
        add(loginStatePanel, BorderLayout.NORTH);
        loginState = new JLabel(LauncherBundle.getString(
                "comments.state.disabled"));
        loginStatePanel.add(loginState, BorderLayout.WEST);
        loginButton = new JButton(LauncherBundle.getString(
                "comments.log-in"));
        loginStatePanel.add(loginButton, BorderLayout.EAST);
        loginButton.setEnabled(false);
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loginOrOut();
            }
        });

        submitButton = new JButton(LauncherBundle.getString("comments.submit"));
        JPanel submitPanel = new JPanel(new BorderLayout());
        submitPanel.add(submitButton, BorderLayout.WEST);
        submitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                submitComment();
            }
        });
        submitButton.setEnabled(false);

        commentTextPanel = new JPanel(new BorderLayout());
        add(commentTextPanel, BorderLayout.CENTER);
        commentTextPanel.add(submitPanel, BorderLayout.SOUTH);

        commentTextPanel.setVisible(false);
        comment = new JTextArea();
        commentTextPanel.add(new JScrollPane(comment,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                BorderLayout.CENTER);
        comment.setEnabled(false);
        comment.setEditable(true);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.setRows(4);
        comment.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                commentTextChanged();
            }

            public void removeUpdate(DocumentEvent e) {
                commentTextChanged();
            }

            public void insertUpdate(DocumentEvent e) {
                commentTextChanged();
            }
        });
    }


    /**
     * Called when the selected game changes.
     */
    public void onGameChange(GameDetail detail) {
        this.detail = detail;
        comment.setText("");
        setupSubmission();
    }


    private void commentTextChanged() {
        if (sub == null || ! sub.isAuthenticated() || detail == null ||
                comment.getText().length() <= 0) {
            submitButton.setEnabled(false);
        } else {
            submitButton.setEnabled(true);
        }
    }

    private void setupSubmission() {
        commentTextChanged();
        if (sub == null) {
            loginButton.setEnabled(false);
            loginState.setText(LauncherBundle.getString(
                    "comments.state.disabled"));
            comment.setEnabled(false);
            commentTextPanel.setVisible(false);
        } else
        if (! sub.isAuthenticated() && detail == null) {
            loginButton.setEnabled(true);
            loginButton.setText(LauncherBundle.getString(
                    "comments.log-in"));
            loginState.setText(LauncherBundle.getString(
                    "comments.state.not-logged-in"));
            comment.setEnabled(false);
            commentTextPanel.setVisible(false);
        } else
        if (! sub.isAuthenticated()) {
            loginButton.setEnabled(true);
            loginButton.setText(LauncherBundle.getString(
                    "comments.log-in"));
            loginState.setText(LauncherBundle.getString(
                    "comments.state.not-logged-in"));
            comment.setEnabled(true);
            commentTextPanel.setVisible(true);
        } else
        if (detail == null) {
            loginButton.setEnabled(true);
            loginButton.setText(LauncherBundle.getString(
                    "comments.log-out"));
            loginState.setText(LauncherBundle.message(
                    "comments.state.logged-in", sub.getUserId()));
            comment.setEnabled(false);
            commentTextPanel.setVisible(false);
        } else {
            loginButton.setEnabled(true);
            loginButton.setText(LauncherBundle.getString(
                    "comments.log-out"));
            loginState.setText(LauncherBundle.message(
                    "comments.state.logged-in", sub.getUserId()));
            comment.setEnabled(true);
            commentTextPanel.setVisible(true);
        }

    }

    @Override
    public void onUserAuthenticated(CommentSubmission submission) {
        setupSubmission();
    }

    @Override
    public void onUserLogoff(CommentSubmission submission) {
        setupSubmission();
    }

    @Override
    public void onCommentSubmissionChange(CommentSubmission submission) {
        // we register with the launcher manager, not the submission itself.

        sub = submission;
        setupSubmission();
    }

    private void loginOrOut() {
        if (sub != null) {
            if (sub.isAuthenticated()) {
                sub.logout();
            } else {
                sub.login();
            }
        }
    }

    private void submitComment() {
        if (sub != null && comment.getText().length() > 0) {
            if (detail == null) {
                launcherManager.getErrorMessageManager().gameError(
                        "No Game Selected",
                        new Throwable("No game selected"),
                        ActionSource.SUBMIT_COMMENT);
            } else
            if (sub.isAuthenticated()) {
                sub.submitComment(detail, comment.getText());
            } else {
                launcherManager.getErrorMessageManager().gameError(
                        "Not Authenticated",
                        new Throwable("You must be logged in to submit a comment"),
                        ActionSource.SUBMIT_COMMENT);
            }
        }
    }
}

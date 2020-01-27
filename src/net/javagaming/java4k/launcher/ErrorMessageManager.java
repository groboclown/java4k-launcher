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

import net.javagaming.java4k.launcher.cache.NoCacheException;
import net.javagaming.java4k.launcher.cache.RemoteConnectionException;
import net.javagaming.java4k.launcher.webstart.JnlpFormatException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipException;

/**
 * Central location for managing error messages sent to the user.
 *
 * @author Groboclown
 */
public class ErrorMessageManager {
    private static final boolean MODALITY = true;
    private final LauncherManager launcherManager;
    private final JDialog managedDialog;
    private final JPanel optionPaneOwner;
    private final List<DialogMessage> messageList;
    private final JLabel messagesRemaining;
    private final Set<String> reportedDiconnectedSites = new HashSet<String>();
    private boolean shutdown = false;


    public ErrorMessageManager(LauncherManager launcherManager) {
        JDialog.setDefaultLookAndFeelDecorated(true);

        this.launcherManager = launcherManager;
        managedDialog = new JDialog(launcherManager.getMainFrame()) {
            public void dispose() {
                if (shutdown) {
                    super.dispose();
                } else {
                    //(new Throwable("Incorrect dispose")).printStackTrace();
                    if (! messageList.isEmpty()) {
                        managedDialog.setVisible(true);
                    }
                }
            }
        };


        managedDialog.setIconImage(Java4kLauncher.getFavicon());
        managedDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        managedDialog.getContentPane().setLayout(new BorderLayout());
        managedDialog.setModal(MODALITY);
        managedDialog.setTitle(LauncherBundle.getString(
                "dialog.messages.title"));

        messageList = Collections.synchronizedList(
                new ArrayList<DialogMessage>());
        optionPaneOwner = new JPanel();
        optionPaneOwner.setLayout(new BoxLayout(optionPaneOwner,
                BoxLayout.PAGE_AXIS));
        JScrollPane listScroller = new JScrollPane(optionPaneOwner);
        listScroller.setAlignmentX(JScrollPane.LEFT_ALIGNMENT);

        managedDialog.add(listScroller, BorderLayout.CENTER);

        messagesRemaining = new JLabel("");
        managedDialog.add(messagesRemaining, BorderLayout.SOUTH);
        messagesRemaining.setFont(Font.getFont(
                LauncherBundle.getString("dialog.messages-remaining.font")));
        messagesRemaining.setBorder(BorderFactory.createLoweredBevelBorder());
    }


    public void shutdownManager() {
        shutdown = true;
        managedDialog.dispose();
    }



    /**
     *
     * @return a collection of all dialogs managed by this manager.
     */
    public Collection<Window> getManagedDialogs() {
        List<Window> ret = new ArrayList<Window>();

        ret.add(managedDialog);

        return ret;
    }


    /**
     * Report to the user an error message, and a nice description about the
     * cause, and maybe a solution.
     *
     * @param sourceName the resource associated with the error (if any).  May be null.
     * @param e the error generated in the error. May be null.
     * @param gameCreate the source of the problem.  Must not be null.
     */
    public void gameError(String sourceName, Throwable e, ActionSource gameCreate) {
        // This is the big logic for making friendly messages
        String title;
        String message;
        int messageType = JOptionPane.ERROR_MESSAGE;
        int style = JOptionPane.DEFAULT_OPTION;

        if (e != null && (e instanceof RuntimeException) &&
                e.getCause() != null) {
            e = e.getCause();
        }

// FIXME for debugging the encoding of error messages
System.out.println("source: [" + sourceName + "]; action: [" + gameCreate + "]; e.class: " + (e == null ? "<null>" : e.getClass()) + "; e.text: [" + (e == null ? "" : e.getMessage()) + "]");
if (e != null) { e.printStackTrace(); }
        // FIXME switch the title to a bundle property

        if (e != null && (e instanceof RemoteConnectionException)) {
            if (reportedSite(((RemoteConnectionException) e).getURI())) {
                return;
            }
            message = LauncherBundle.message(
                    "dialog.messages.errors.remote-site-not-found",
                    e.getMessage());
            title = "Using Cached Version";
        } else if (e != null && (e instanceof NoCacheException)) {
            if (reportedSite(((NoCacheException) e).getURI())) {
                return;
            }
            message = LauncherBundle.message(
                    "dialog.messages.errors.exception-no-cache",
                    e.getMessage());
            title = "Cannot Find Game";
        } else if (e != null && (e instanceof JnlpFormatException)) {
            message = LauncherBundle.message(
                    "dialog.messages.errors.bad-jnlp",
                    e.getMessage());
            title = "Invalid Game File";
        } else {
            switch (gameCreate) {
                case FILE_CACHE:
                    // Always show this message, because we don't know which
                    // site this comes from.
                    title = "Using Cached Version";
                    if (e == null) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.general-cache",
                                sourceName);
                    } else {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.exception-cache",
                                e.getMessage());
                    }
                    break;
                case GAME_DOWNLOAD:
                case DETAILS_DOWNLOAD:
                    title = "Problem Downloading";
                    if (e == null) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.general-download",
                                sourceName);
                    } else
                    if (e instanceof SecurityException) {
                        // TODO inspect for actual security message.  This is
                        // just the most common.
                        //SecurityException se = (SecurityException) e;
                        //se.getMessage();
                        message = LauncherBundle.message(
                                "dialog.messages.errors.security-download.url",
                                sourceName);
                    } else {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.unknown-download",
                                sourceName, e.getMessage());
                    }
                    break;
                case GAME_CREATE:
                    title = "Problem Creating";
                    if (e == null) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.general-create",
                                sourceName);
                    } else {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.exception-create",
                                sourceName);
                    }
                    break;
                case GAME_DESTORY:
                    title = "Failed to Stop";
                    if (e == null) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.general-destroy",
                                sourceName);
                    } else {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.exception-destroy",
                                sourceName, e.getMessage());
                    }
                    break;
                case GAME_START:
                    title = "Failed to Start";
                    if (e == null) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.general-start",
                                sourceName);
                    } else if (e instanceof ZipException) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.bad-zip",
                                sourceName,
                                e.getMessage());
                    } else if (e instanceof HardDeathException) {
                        // this is actually an expected state
                        return;
                    } else {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.exception-start",
                                sourceName, e.getMessage());
                    }
                    break;
                case GAME_DESTORY_STILL_RUNNING:
                    title = "Failed to Stop";
                    if (e == null) {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.general-still_runing",
                                sourceName);
                    } else {
                        message = LauncherBundle.message(
                                "dialog.messages.errors.exception-still_runing",
                                sourceName, e.getMessage());
                    }
                    break;
                default:
                    if (e == null) {
                        // FIXME change to bundle properties
                        title = LauncherBundle.getString(
                                "dialog.messages.errors.unknown.title");
                        message = LauncherBundle.message(
                                "dialog.messages.errors.unknown.message",
                                sourceName);
                    } else if (e instanceof IOException) {
                        title = LauncherBundle.getString(
                                "dialog.messages.errors.unknown-io.title");
                        message = LauncherBundle.message(
                                "dialog.messages.errors.unknown-io.message",
                                sourceName, e.getMessage());
                    } else {
                        title = LauncherBundle.getString(
                                "dialog.messages.errors.unknown.title");
                        message = LauncherBundle.message(
                                "dialog.messages.errors.unknown.exception-message",
                                sourceName, e.getMessage());
                    }


                    break;
            }
        }

        queueMessage(title, message, messageType, style, null);
    }


    private boolean reportedSite(URI uri) {
        String site = uri.getHost();
        if (reportedDiconnectedSites.contains(site)) {
            return true;
        }
        reportedDiconnectedSites.add(site);
        return false;
    }


    /**
     * Remove this message from the queue, and possibly advance the queue.
     *
     * @param m message to remove from the queue
     */
    void endMessage(final DialogMessage m) {
        if (messageList.remove(m)) {
            launcherManager.getWorkerManager().startEDT("message remove",
                    ActionSource.GUI_UPDATE, new Runnable() {
                @Override
                public void run() {
                    optionPaneOwner.remove(m.pane);
                    updateDialog(false);
                }
            });
        }
    }


    public void addOptionPane(String titleKey, int messageType, int styleType,
            MessageHandler handler, String messageKey, Object... params) {
        queueMessage(LauncherBundle.getString(titleKey),
                LauncherBundle.message(messageKey, params),
                messageType, styleType, handler);
    }


    void queueMessage(final String title, String message,
            final int messageType, final int style,
            final MessageHandler handler) {
        final String msg;
        if (! message.startsWith("<html>")) {
            msg = "<html><p style='width: 200px;'>" + message;
        } else {
            msg = message;
        }
        launcherManager.getWorkerManager().startEDT(
                title, ActionSource.GUI_UPDATE, new Runnable() {
            @Override
            public void run() {
                final DialogMessage dm = new DialogMessage(title,
                        new JOptionPane(msg, messageType, style), handler);
                messageList.add(dm);
                managedDialog.setTitle(title);
                optionPaneOwner.add(dm.pane);
                updateDialog(true);
            }
        });
    }

    // must be run inside a the EDT
    private void updateDialog(boolean added) {
        if (messageList.isEmpty()) {
            managedDialog.setVisible(false);
            return;
        }
        optionPaneOwner.validate();
        int count = messageList.size();

        String msg;
        switch (count) {
            case 0:
                msg = "dialog.messages-remaining.0";
                break;
            case 1:
                msg = "dialog.messages-remaining.1";
                break;
            case 2:
                msg = "dialog.messages-remaining.2";
                break;
            default:
                msg = "dialog.messages-remaining.many";
                break;
        }

        messagesRemaining.setText(LauncherBundle.message(msg, count));
        managedDialog.validate();
        managedDialog.pack();

        if (! managedDialog.isVisible()) {
            managedDialog.setLocationRelativeTo(launcherManager.getMainFrame());
            managedDialog.setVisible(true);
        }
        if (added) {
            managedDialog.requestFocus();
        }
    }


    class DialogMessage implements PropertyChangeListener {
        final String title;
        final JOptionPane pane;
        final MessageHandler handler;

        DialogMessage(String title, JOptionPane pane, MessageHandler handler) {
            this.title = title;
            this.pane = pane;
            pane.setBorder(BorderFactory.createLoweredBevelBorder());
            this.handler = handler;
            pane.addPropertyChangeListener(this);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName().equals(JOptionPane.VALUE_PROPERTY)) {
                Object value = evt.getNewValue();
                if (handler != null) {
                    handler.handleOptionSelection(value);
                }
                pane.removePropertyChangeListener(this);
                endMessage(this);
            }
        }
    }


    public interface MessageHandler {
        /**
         *
         * @param option same as {@link javax.swing.JOptionPane#getValue()}
         */
        public void handleOptionSelection(Object option);
    }
}

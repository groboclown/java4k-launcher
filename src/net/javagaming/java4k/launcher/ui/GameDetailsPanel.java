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
import net.javagaming.java4k.launcher.DefaultGameDetailListResourceReader;
import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.GameDetail;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.UserComment;
import net.javagaming.java4k.launcher.cache.Resource;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Displays the details about the game.
 *
 * @author Groboclown
 */
public class GameDetailsPanel extends ResourceReaderPanel<GameDetail> {
    private static final int MAX_ICON_WIDTH = 64;
    private static final int MAX_ICON_HEIGHT = 64;


    private final JLabel name;
    private final JLabel author;
    private final JEditorPane description;
    private final JEditorPane details;
    private final JLabel date;
    private final DefaultListModel commentModel;
    private final DefaultGameDetailListResourceReader reader;
    //private final AddCommentPanel addCommentPanel;
    private GameDetail detail;
    private BufferedImage blank;



    public GameDetailsPanel(final LauncherManager launcherManager) {
        super(launcherManager);
        name = new JLabel();
        author = new JLabel();
        description = new JEditorPane();
        description.setEditable(false);
        description.setContentType("text/html");
        details = new JEditorPane();
        details.setEditable(false);
        details.setContentType("text/html");
        date = new JLabel();
        commentModel = new DefaultListModel();
        reader = new DefaultGameDetailListResourceReader();
        setConsumer(reader);



        JPanel header = new JPanel(new BorderLayout());
        add(header, BorderLayout.NORTH);
        name.setFont(Font.decode(LauncherBundle.getString(
                "gamedetails.name.font")));
        name.setIcon(resizeIcon(null));
        header.add(name, BorderLayout.NORTH);

        JPanel top = new JPanel(new GridLayout(2, 2, 10, 5));
        header.add(top, BorderLayout.CENTER);
        top.add(new JLabel(LauncherBundle.getString(
                "gameselection.details.author"), SwingConstants.RIGHT));
        top.add(author);

        top.add(new JLabel(LauncherBundle.getString(
                "gameselection.details.date"), SwingConstants.RIGHT));
        top.add(date);

        JTabbedPane tabs = new JTabbedPane();
        add(tabs, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new GridLayout(1,2));
        tabs.add(LauncherBundle.getString("gamedetails.tab.info"), bottom);
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        bottom.add(descriptionPanel);
        descriptionPanel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.BLACK),
                        LauncherBundle.getString(
                                "gameselection.details.description")));
        descriptionPanel.add(new JScrollPane(description), BorderLayout.CENTER);
        JPanel detailPanel = new JPanel(new BorderLayout());
        bottom.add(detailPanel);
        detailPanel.setBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(Color.BLACK),
                        LauncherBundle.getString(
                                "gameselection.details.details")));
        detailPanel.add(new JScrollPane(details), BorderLayout.CENTER);

        JPanel commentPanel = new JPanel(new BorderLayout());
        tabs.add(LauncherBundle.getString("gamedetails.tab.comments"),
                commentPanel);
        //addCommentPanel = new AddCommentPanel(launcherManager);
        //commentPanel.add(addCommentPanel,
        //        BorderLayout.NORTH);
        JList comments = new JList(commentModel);
        commentPanel.add(new JScrollPane(comments,
                        ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                        ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER),
                BorderLayout.CENTER);
        comments.setCellRenderer(new UserCommentPanel());
    }

    public GameDetail getGameDetail() {
        return detail;
    }

    public void setGameDescription(GameDescription desc) {
        if (desc == null) {
            onResourceLoadStarted(null);
        } else {
            reader.addPendingSource(desc);
            reader.setResource(desc.getGameDetailSource());
            loadResource(ActionSource.DETAILS_DOWNLOAD);
        }
    }

    @Override
    protected void onResourceLoadStarted(Resource r) {
        this.detail = null;
        name.setText(" ");
        name.setIcon(resizeIcon(null));
        author.setText("");
        description.setText("");
        details.setText("");
        date.setText("");
        commentModel.removeAllElements();
    }

    @Override
    protected void processPublished(List<GameDetail> values) throws Exception {
        if (values.isEmpty()) {
            return;
        }
        if (values.size() != 1) {
            System.err.println(
                "Found multiple details for a single description.  Only showing the first.");
        }
        this.detail = values.get(0);
        if (detail == null) {
            return;
        }


        // if the current source is identical to the passed-in source, we'll
        // still refresh in case the source has been loaded.

        name.setText(notNull(detail.getName()));
        author.setText(notNull(detail.getSource().getAuthor()));
        description.setText(getHtmlText(detail.getSource().getDescription()));
        details.setText(getHtmlText(detail.getSource().getInstructions()));
        date.setText(notNull(detail.getSource().getSubmissionDate()));
        name.setIcon(resizeIcon(detail.getIcon()));
        for (UserComment c: detail.getSource().getUserComments()) {
            commentModel.addElement(c);
        }

        //addCommentPanel.onGameChange(detail);
    }

    @Override
    protected void onResourceProcessed() throws Exception {
        // do nothing
    }

    @Override
    protected void onResourceLoadFailure(Resource r, IOException e) throws Exception {
        // do nothing
    }


    private String notNull(String text) {
        return text == null ? "" : text;
    }

    private String notNull(Date submissionDate) {
        return submissionDate == null ? "" : submissionDate.toString();
    }

    private String getHtmlText(String text) {
        text = notNull(text);
        return LauncherBundle.getString("gamedetails.description.prefix") +
                text.replace("\r\n", "<br/>").
                replace("\r", "<br/>").
                replace("\n", "<br/>");
    }


    private Icon resizeIcon(Image img) {
        if (img == null) {
            if (blank == null) {
                blank = new BufferedImage(
                        MAX_ICON_WIDTH, MAX_ICON_HEIGHT,
                        BufferedImage.TYPE_INT_ARGB);

                Color c = new Color(0, 0, 0, 0);
                Graphics g = blank.getGraphics();
                g.setColor(c);
                g.fillRect(0, 0, MAX_ICON_WIDTH, MAX_ICON_HEIGHT);
            }
            return new ImageIcon(blank);
        }
        return new ImageIcon(
                img.getScaledInstance(MAX_ICON_WIDTH, MAX_ICON_HEIGHT,
                        Image.SCALE_SMOOTH));
    }
}

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

import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.UserComment;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;

/**
 * A list cell renderer for a single user comment.
 *
 *
 * @author Groboclown
 */
public class UserCommentPanel extends JPanel implements ListCellRenderer {
    public static final Class<?> VALUE_TYPE = UserComment.class;

    private final JLabel user;
    private final JLabel date;
    private final JLabel id;
    private final JTextArea comment;
    private final Color background;
    private final Color highlight;


    public UserCommentPanel() {
        super(new BorderLayout());
        setOpaque(true);

        user = new JLabel();
        date = new JLabel();
        id = new JLabel();
        comment = new JTextArea();
        comment.setEditable(false);
        comment.setLineWrap(true);
        comment.setWrapStyleWord(true);
        comment.setOpaque(false);
        comment.setFont(Font.decode(LauncherBundle.getString("comments.font")));

        JPanel top = new JPanel(new BorderLayout());
        add(top, BorderLayout.NORTH);
        top.setOpaque(false);

        top.add(id, BorderLayout.NORTH);

        JPanel userp = new JPanel();
        top.add(userp, BorderLayout.WEST);
        userp.setOpaque(false);
        JLabel usert = new JLabel(LauncherBundle.getString("comments.usert"));
        usert.setOpaque(false);
        userp.add(usert);
        usert.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        usert.setHorizontalTextPosition(JLabel.TRAILING);
        userp.add(user);
        user.setHorizontalTextPosition(JLabel.LEADING);
        user.setOpaque(false);

        JPanel datep = new JPanel();
        top.add(datep, BorderLayout.EAST);
        datep.setOpaque(false);
        JLabel datet = new JLabel(LauncherBundle.getString("comments.datet"));
        datep.add(datet);
        datet.setOpaque(false);
        datet.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        datet.setHorizontalTextPosition(JLabel.TRAILING);
        datep.add(date);
        date.setHorizontalTextPosition(JLabel.LEADING);
        date.setOpaque(false);

        add(comment, BorderLayout.CENTER);

        /*
        Color bg = getBackground();
        Color hi = bg.brighter();
        background = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 0x30);
        highlight = new Color(hi.getRed(), hi.getGreen(), hi.getBlue(), 0x30);
        */
        background = getBackground();
        highlight = background.brighter().brighter();
    }

    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        if (value == null || !(value instanceof UserComment)) {
            // FIXME return an empty label
            return null;
        }


        UserComment userComment = (UserComment) value;

        if ((index % 2) == 0) {
            setBackground(background);
        } else {
            setBackground(highlight);
        }

        id.setText(userComment.getId());
        user.setText(userComment.getUsername());
        date.setText(userComment.getPostDate().toString());
        comment.setText(userComment.getText());

        return this;
    }
}

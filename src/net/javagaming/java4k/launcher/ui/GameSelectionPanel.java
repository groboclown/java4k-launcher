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

import net.javagaming.java4k.launcher.GameDescription;
import net.javagaming.java4k.launcher.GameSelectAction;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.cache.Resource;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Groboclown
 */
public class GameSelectionPanel extends ResourceReaderPanel<GameDescription> {
    private final List<GameDescription> currentSources;
    private final JTable appletList;
    private final Color highlightBackground = Color.LIGHT_GRAY;
    private final Color highlightForeground = Color.BLACK;
    private volatile int selectedRow = -1;

    public GameSelectionPanel(LauncherManager launcherManager,
            final GameSelectAction appletLauncher) {
        super(launcherManager);

        if (appletLauncher == null) {
            throw new NullPointerException("null applet launcher");
        }
        currentSources = Collections.synchronizedList(
                new ArrayList<GameDescription>());

        appletList = new JTable(new AppletListTableModel());
        add(new JScrollPane(appletList), BorderLayout.CENTER);
        appletList.setRowSelectionAllowed(false);
        appletList.setCellSelectionEnabled(true);
        appletList.setColumnSelectionAllowed(false);
        appletList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        appletList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    selectedRow = appletList.getSelectedRow();
                    GameDescription source = getActiveApplet();
                    appletLauncher.selectGame(source);
                }
            }
        });

        appletList.setDefaultRenderer(Object.class, new HighlightRenderer());
    }

    private GameDescription getActiveApplet() {
        int row = appletList.getSelectedRow();
        if (row >= 0 && row < currentSources.size()) {
            return currentSources.get(row);
        }
        return null;
    }


    boolean isRowSelected(int row) {
        return selectedRow == row;
    }

    @Override
    protected void onResourceLoadStarted(Resource r) {
        //appletList.clearSelection();
        int count = currentSources.size();
        currentSources.clear();
        if (count > 0) {
            ((AppletListTableModel) appletList.getModel()).fireTableRowsDeleted(
                    0, count - 1);
            //((AppletListTableModel) appletList.getModel()).fireTableDataChanged();
        }
    }

    @Override
    protected void processPublished(List<GameDescription> values) throws Exception {
        int start = currentSources.size();
        currentSources.addAll(values);
        ((AppletListTableModel) appletList.getModel()).fireTableRowsInserted(
                start, start + values.size() - 1);
    }

    @Override
    protected void onResourceProcessed() throws Exception {
        ((AppletListTableModel) appletList.getModel()).fireTableDataChanged();
    }

    @Override
    protected void onResourceLoadFailure(Resource r, IOException e) throws Exception {

    }


    class AppletListTableModel extends AbstractTableModel {
        @Override
        public String getColumnName(int col) {
            return col == 0
                ? LauncherBundle.getString("gameselection.list.name")
                : LauncherBundle.getString("gameselection.list.author");
        }

        @Override
        public int getRowCount() {
            return currentSources.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (rowIndex >= 0 && rowIndex < currentSources.size()) {
                GameDescription source = currentSources.get(rowIndex);
                if (columnIndex == 0) {
                    return source.getName();
                } else {
                    return source.getAuthor();
                }
            }
            return null;
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    }


    class HighlightRenderer extends JLabel implements TableCellRenderer {
        private final Color foreground = getForeground();



        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            if (value == null) {
                if (row >= 0 && row < currentSources.size()) {
                    GameDescription source = currentSources.get(row);
                    if (column == 0) {
                        value = source.getName();
                    } else {
                        value = source.getAuthor();
                    }
                }
                if (value == null) {
                    value = "";
                }
            }
            setText(value.toString());

            // Set up default state here
            //c.setBackground(Color.white);
            //c.setForeground(Color.black);
            // ...
            if (isRowSelected(row) ) {
                // set the highlight color
                setOpaque(true);
                setBackground(highlightBackground);
                setForeground(highlightForeground);
            } else {
                setOpaque(false);
                setForeground(foreground);
            }
            return this;
        }
    }


}

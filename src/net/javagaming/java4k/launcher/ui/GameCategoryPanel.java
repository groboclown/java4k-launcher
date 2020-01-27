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
import net.javagaming.java4k.launcher.GameDescriptionListResourceReader;
import net.javagaming.java4k.launcher.GameSelectAction;
import net.javagaming.java4k.launcher.LauncherBundle;
import net.javagaming.java4k.launcher.LauncherManager;
import net.javagaming.java4k.launcher.cache.Resource;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Groboclown
 */
public class GameCategoryPanel
        extends ResourceReaderPanel<GameDescriptionListResourceReader> {
    private final Object sync = new Object();
    private final Map<String, GameDescriptionListResourceReader> categories =
            new HashMap<String, GameDescriptionListResourceReader>();
    private final GameSelectionPanel selectionPanel;
    private String currentSelection = null;
    private JComboBox selection;

    public GameCategoryPanel(final LauncherManager launcherManager,
            GameSelectAction launchGameAction) {
        super(launcherManager);
        selection = new JComboBox();
        selectionPanel = new GameSelectionPanel(launcherManager,
                launchGameAction);

        JPanel categoryPanel = new JPanel(new BorderLayout());
        add(categoryPanel, BorderLayout.NORTH);

        JPanel statusPanel = new JPanel();
        categoryPanel.add(statusPanel, BorderLayout.WEST);

        // the category status isn't very interesting
        //statusPanel.add(getStatusIconComponent());
        statusPanel.add(selectionPanel.getStatusIconComponent());

        JButton refreshButton = new JButton();
        statusPanel.add(refreshButton);
        refreshButton.setIcon(loadIcon("reload.png"));
        refreshButton.setToolTipText(LauncherBundle.getString(
                "download.refresh-games"));
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (selection.getSelectedItem() != null) {
                    selectionPanel.reloadResource(
                            ActionSource.DETAILS_DOWNLOAD);
                }
            }
        });


        categoryPanel.add(selection, BorderLayout.CENTER);
        add(selectionPanel, BorderLayout.CENTER);

        // FIXME this should be a property change event
        selection.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launcherManager.getWorkerManager().startEDT(
                        "select panel", ActionSource.GUI_UPDATE, new Runnable() {
                    @Override
                    public void run() {
                        setActivePanel((String) selection.getSelectedItem());
                    }
                });
            }
        });
    }


    private void setActivePanel(String cat) {
        synchronized (sync) {
            if (cat != null && cat.equals(currentSelection)) {
                return;
            }
            currentSelection = cat;
            selectionPanel.setConsumer(categories.get(cat));
            selectionPanel.loadResource(ActionSource.DETAILS_DOWNLOAD);
        }
    }


    @Override
    protected void onResourceLoadStarted(Resource r) {
        categories.clear();

        // TODO does this need to happen in the EDT?
        // This probably clears the selected item.  We may want to cache it.
        selection.removeAllItems();
    }

    @Override
    protected void processPublished(List<GameDescriptionListResourceReader> values) throws Exception {
        for (GameDescriptionListResourceReader factory : values) {
            categories.put(factory.getName(),
                    factory);
        }
    }

    @SuppressWarnings("SuspiciousMethodCalls")
    @Override
    protected void onResourceProcessed() throws Exception {
        Object selectedItem = selection.getSelectedItem();
        List<String> names = new ArrayList<String>(
                categories.keySet());
        Collections.sort(names);
        Collections.reverse(names);
        selection.removeAllItems();
        for (String name : names) {
            selection.addItem(name);
        }
        if (selectedItem != null && names.contains(selectedItem)) {
            selection.setSelectedItem(selectedItem);
        } else if (!names.isEmpty()) {
            selection.setSelectedItem(names.get(0));
        }
        selectionPanel.validate();
        selectionPanel.setVisible(true);
    }


    @Override
    protected void onResourceLoadFailure(Resource r, IOException e) throws Exception {
        getLauncherManager().getErrorMessageManager().gameError(
                r.toString(), e, ActionSource.CATEGORY_DOWNLOAD);
    }
}

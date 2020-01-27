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
import net.javagaming.java4k.launcher.cache.Resource;
import net.javagaming.java4k.launcher.java4kcom.Java4kResourceReaderFactory;
import net.javagaming.java4k.launcher.ui.GameCategoryPanel;
import net.javagaming.java4k.launcher.ui.GameDetailsPanel;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.io.IOException;

/**
 * Launcher for Java 4k games.
 * <p/>
 * Original post:
 * http://www.java-gaming.org/topics/discuss-the-future-of-4k-contest/30600/msg/284588/view.html#msg284588
 * <p/>
 *
 * @author Sunsword
 * @author Groboclown
 */
public class Java4kLauncher extends JFrame {
    private static final long serialVersionUID = 1L;
    private static final int WIDTH = 810;
    private static final int HEIGHT = 640;

    private final LauncherManager launcherManager;
    private GameDetailsPanel detailsPanel;
    private GameCategoryPanel categoryPanel = null;
    private ProgressPanel progressPanel;
    private File lastDir = new File(".");



    /**
     * @param args command-line arguments
     */
    public static void main(final String[] args) {
        Security.setupOptions(args);
        setupLAF();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                final Java4kLauncher frame = new Java4kLauncher(
                        LauncherBundle.getString("application.title"));

                frame.setIconImage(getFavicon());
                frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));

                frame.pack();
                frame.validate();
                frame.setVisible(true);

                frame.launcherManager.getGameManager().setupSecurity();
                if (! Security.showLocalGames()) {
                    frame.openJava4k();
                }
            }
        });
    }

    private static void setupLAF() {
        try {
            String laf = LauncherBundle.getString("lookandfeel");
            if (laf.length() <= 0) {
                laf = UIManager.getSystemLookAndFeelClassName();
            }
            UIManager.setLookAndFeel(laf);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        JFrame.setDefaultLookAndFeelDecorated(true);
    }

    public Java4kLauncher(String title) {
        super(title);
        this.progressPanel = new ProgressPanel();
        this.launcherManager = new LauncherManager(progressPanel, this);
        setupGUI();
    }


    private void setupGUI() {
        if (Security.showLocalGames()) {
            JMenuBar menuBar = new JMenuBar();
            setJMenuBar(menuBar);

            JMenu file = new JMenu(LauncherBundle.getString("menu.file"));
            file.setMnemonic(KeyStroke.getKeyStroke(
                    LauncherBundle.getString("menu.file.m")).getKeyCode());
            menuBar.add(file);


            JMenuItem openJar = new JMenuItem(
                    LauncherBundle.getString("menu.file.open-java4k"));
            openJar.setMnemonic(KeyStroke.getKeyStroke(
                    LauncherBundle.getString("menu.file.open-java4k.m")).getKeyCode());
            file.add(openJar);
            openJar.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openJava4k();
                }
            });

            JMenuItem openLocal = new JMenuItem(
                    LauncherBundle.getString("menu.file.open-local"));
            openLocal.setMnemonic(KeyStroke.getKeyStroke(
                    LauncherBundle.getString("menu.file.open-local.m")).getKeyCode());
            file.add(openLocal);
            openLocal.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openLocal();
                }
            });

            JMenuItem openUrl = new JMenuItem(
                    LauncherBundle.getString("menu.file.open-url"));
            openUrl.setMnemonic(KeyStroke.getKeyStroke(
                    LauncherBundle.getString("menu.file.open-url.m")).getKeyCode());
            file.add(openUrl);
            openUrl.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    openWebPage();
                }
            });

            file.addSeparator();

            JMenuItem exit = new JMenuItem(
                    LauncherBundle.getString("menu.file.exit"));
            exit.setMnemonic(KeyStroke.getKeyStroke(
                    LauncherBundle.getString("menu.file.exit.m")).getKeyCode());
            file.add(exit);
            exit.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    exit();
                }
            });
        }

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(WIDTH / 3);
        JPanel left = new JPanel(new BorderLayout());
        split.add(left, JSplitPane.LEFT);

        final JButton start = new JButton(
                LauncherBundle.getString("gamedetails.button.launch"));

        categoryPanel = new GameCategoryPanel(launcherManager,
                new GameSelectAction() {
            @Override
            public void selectGame(GameDescription source) {
                detailsPanel.setGameDescription(source);
            }
        });
        left.add(categoryPanel, BorderLayout.CENTER);

        // The progress bar isn't really that helpful.  If it is fixed to be
        //left.add(progressPanel, BorderLayout.SOUTH);


        detailsPanel = new GameDetailsPanel(launcherManager) {
            @Override
            protected void onResourceProcessed() throws Exception {
                if (launcherManager.getGameManager().getActiveGame() == null) {
                    // FIXME need to ensure that the current detail
                    // is the just-loaded detail.
                    start.setEnabled(true);
                }
            }

            @Override
            protected void onResourceLoadFailure(Resource r, IOException e) throws Exception {
                if (launcherManager.getGameManager().getActiveGame() == null &&
                        getGameDetail() != null &&
                        getGameDetail().getDetailsSource().equals(r)) {
                    start.setEnabled(false);
                }
            }
        };



        JPanel overRight = new JPanel(new BorderLayout());
        split.add(overRight, JSplitPane.RIGHT);
        JPanel gameStatusButtonPanel = new JPanel(new BorderLayout());
        overRight.add(gameStatusButtonPanel, BorderLayout.NORTH);


        JPanel buttonPanel = new JPanel();
        gameStatusButtonPanel.add(buttonPanel, BorderLayout.CENTER);
        buttonPanel.add(start);
        start.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (detailsPanel != null &&
                        detailsPanel.getGameDetail() != null) {
                    //System.out.println("Setting active game " +
                    //        detailsPanel.getGameDetail().getName());
                    launcherManager.getGameManager().setActiveGame(
                            detailsPanel.getGameDetail());
                }
            }
        });
        start.setEnabled(false);
        final JButton stop = new JButton(
                LauncherBundle.getString("gamedetails.button.stop"));
        buttonPanel.add(stop);
        stop.setEnabled(false);
        stop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                launcherManager.getGameManager().setActiveGame(null);
            }
        });
        gameStatusButtonPanel.add(detailsPanel.getStatusIconComponent(), BorderLayout.EAST);



        launcherManager.getGameManager().addGameStateListener(new GameStateListener() {
            @Override
            public void onGameStarted(GameDetail desc) {
                start.setEnabled(false);
                stop.setEnabled(true);
            }

            @Override
            public void onGameStopped(GameDetail desc) {
                start.setEnabled(
                    launcherManager.getGameManager().getActiveGame() != null);
                stop.setEnabled(false);
            }
        });
        overRight.add(detailsPanel, BorderLayout.CENTER);

        getContentPane().add(split, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                exit();
            }
        });
    }


    public void openJava4k() {
        Java4kResourceReaderFactory factory = new Java4kResourceReaderFactory();
        try {
            setCategory(factory.createCategoryListResourceReader(
                    launcherManager));
        } catch (IOException e) {
            launcherManager.getErrorMessageManager().gameError(
                    "Open Java4k site", e, ActionSource.CATEGORY_DOWNLOAD);
        }
    }


    public void openLocal() {
        JFileChooser fc = new JFileChooser(lastDir);
        fc.setDialogTitle(LauncherBundle.getString("dialog.open-local.title"));
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int choice = fc.showDialog(this,
                LauncherBundle.getString("dialog.open-local.button"));
        if (choice == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            lastDir = f.getParentFile();
            String url = f.toURI().toString();
            openURL(url);
        }
    }

    public void openWebPage() {
        String url = JOptionPane.showInputDialog(
                LauncherBundle.getString("dialog.open-url.prompt"));

        openURL(url);
    }


    protected void openURL(String url) {
        try {
            CategoryListResourceReader cat =
                    new SingleUrlCategoryResourceReader(url,
                            Cache.getInstance().getTopResource(
                                    Cache.uri(url), true));
            setCategory(cat);
        } catch (IOException e) {
            launcherManager.getErrorMessageManager().gameError(
                    "Open " + url, e, ActionSource.CATEGORY_DOWNLOAD);
        }
    }


    protected void setCategory(CategoryListResourceReader cat) {
        launcherManager.setCommentSubmission(cat.getCommentSubmission());
        categoryPanel.setConsumer(cat);
        categoryPanel.loadResource(ActionSource.CATEGORY_DOWNLOAD);
    }


    public void exit() {
        /*if (JOptionPane.showConfirmDialog(Java4kLauncher.this,
                "Are you sure to close this window?", "Really Closing?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
        }*/
        launcherManager.shutdown();

        System.exit(0);
    }


    /**
     * Get the icon for this application.
     * @return An Image or null.
     */
    static Image getFavicon() {
        java.net.URL imgURL = Java4kLauncher.class.getResource("favicon.png");
        if (imgURL != null) {
            return new ImageIcon(imgURL).getImage();
        } else {
            return null;
        }
    }
}

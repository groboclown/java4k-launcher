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

import net.javagaming.java4k.launcher.progress.ProgressController;
import net.javagaming.java4k.launcher.progress.ProgressState;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;

/**
 * Handles a single gui for showing all the worker progress.  It draws the
 * label text directly on the progress bar.
 *
 * @author Groboclown
 */
public class ProgressPanel extends JPanel {
    private static final int BAR_RANGE = 10000;


    //private final LabelProgressBar bar;
    private final JProgressBar bar;
    private final JLabel count;
    private final JButton cancel;
    private final Map<Worker, ProgressState> activeWorkers =
            new HashMap<Worker, ProgressState>();
    private LauncherManager launcherManager;




    public ProgressPanel() {
        super(new BorderLayout());

        bar = new JProgressBar();
        bar.setIndeterminate(false);
        bar.setMinimum(0);
        bar.setMaximum(BAR_RANGE);
        bar.setValue(0);
        cancel = new JButton(LauncherBundle.getString("progress.cancel"));
        cancel.setEnabled(false);
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
        // FIXME add an icon
        count = new JLabel(LauncherBundle.message("progress.count", 0));

        JPanel sub = new JPanel(new BorderLayout());
        add(sub, BorderLayout.CENTER);
        // Nothing right now looks for a cancelation, so remove it.
        //sub.add(cancel, BorderLayout.WEST);
        sub.add(bar, BorderLayout.CENTER);
        sub.add(count, BorderLayout.EAST);
    }


    void setLauncherManager(LauncherManager launcherManager) {
        if (this.launcherManager != null) {
            this.launcherManager.getErrorMessageManager().gameError(
                    "initialization", new Throwable(), ActionSource.GUI_UPDATE);
            return;
        }
        this.launcherManager = launcherManager;
    }


    public ProgressController createWorkerController(
            String name, boolean canCancel) {
        Worker worker = new Worker(name, canCancel);
        setWorkerState(worker, null);
        return worker;
    }


    public void removeWorkerController(ProgressController controller) {
        if (controller instanceof Worker) {
            controller.completed();
        }
    }


    /**
     * Cancel all active workers who can be cancelled.
     */
    public void cancel() {
        boolean cancelled = false;
        synchronized (activeWorkers) {
            for (Worker w: activeWorkers.keySet()) {
                if (w.allowsCancel) {
                    // FIXME send an event instead?
                    w.isCancelled = true;


                    cancelled = true;
                }
            }
        }
        if (cancelled) {
            refreshGUI();
        }
    }


    void removeWorker(Worker w) {
        synchronized (activeWorkers) {
            if (activeWorkers.remove(w) != null) {
                refreshGUI();
            }
        }
    }


    /**
     * called when a worker is created, or when a worker's state is updated.
     *
     * @param w
     * @param newState
     */
    void setWorkerState(Worker w, ProgressState newState) {
        if (w == null) {
            throw new NullPointerException("null worker");
        }
        if (newState == null) {
            newState = new ProgressState(w.name);
        }
        synchronized (activeWorkers) {
            activeWorkers.put(w, newState);
        }
        refreshGUI();
    }


    public void refreshGUI() {
        if (launcherManager != null) {
            launcherManager.getWorkerManager().startEDT("Progress Bar",
                    ActionSource.GUI_UPDATE, new Runnable() {
                @Override
                public void run() {
                    runUIUpdate();
                }
            });
        }
    }


    /**
     * Recompute the gui components.  Called in EDT.
     */
    private void runUIUpdate() {

        synchronized (activeWorkers) {
            int workerCount = activeWorkers.size();
            int downloads = launcherManager.getWorkerManager().
                    getPendingDownloads();
            count.setText(LauncherBundle.message("progress.count",
                    workerCount + downloads));

            if (workerCount + downloads == 0) {
                bar.setValue(0);
                bar.setIndeterminate(false);
                cancel.setEnabled(false);
                bar.setStringPainted(false);
                return;
            }
            if (workerCount == 0) {
                bar.setIndeterminate(true);
                // FIXME use a resource
                bar.setString("Downloading...");
                bar.setStringPainted(true);
                return;
            }

            int value = 0;
            int workerRange = BAR_RANGE / workerCount;
            boolean indeterminant = false;
            String labelText = "";
            boolean canCancel = false;

            for (Map.Entry<Worker, ProgressState> e: activeWorkers.entrySet()) {
                // ignore this state for now; only use what the process is
                // actually doing, not what the user requested.
                // FIXME However, we do need some kind of notice to the users
                // that a cancel was triggered.
                // if (e.getKey().isCancelled())

                ProgressState state = e.getValue();
                if (state.isIndeterminate() ||
                        state.getMinimum() >= state.getMaximum()) {
                    indeterminant = true;
                } else {
                    value += ((state.getValue() - state.getMinimum()) *
                            workerRange) / (state.getMaximum() - state.getMinimum());
                }
                if (state.getLabelText() != null) {
                    labelText = state.getLabelText();
                }
                Worker worker = e.getKey();
                if (! worker.isCancelled() && worker.allowsCancel) {
                    canCancel = true;
                }
            }

            bar.setIndeterminate(indeterminant);
            bar.setValue(value);
            if (labelText == null || labelText.length() <= 0) {
                bar.setStringPainted(false);
            } else {
                bar.setStringPainted(true);
                bar.setString(labelText);
            }
            cancel.setEnabled(canCancel);
        }
    }


    class Worker extends ChildProgressController {
        final String name;
        final boolean allowsCancel;
        boolean isCancelled = false;

        public Worker(String name, boolean allowsCancel) {
            super(null, 10000);

            this.name = name;
            this.allowsCancel = allowsCancel;
        }

        @Override
        public boolean isCancelled() {
            return isCancelled;
        }

        @Override
        public void completed() {
            removeWorker(this);
        }

        @Override
        protected void sendState(ProgressState p) {
            setWorkerState(this, p);
        }
    }



/*
    static class LabelProgressBar extends JProgressBar {
        private final JLabel label;

        LabelProgressBar() {
            label = new JLabel();
            label.setOpaque(true);
        }


        public void setLabel(String text) {
            if (text == null) {
                text = "";
            }
            label.setText("   " + text);
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            if (isShowing()) {
                label.setSize(getSize());
                String text = label.getText();
                if (text != null && text.length() > 0) {
                    label.setForeground(Color.RED);
                    label.getUI().paint(g, label);
                //g.setFont(f);
                //g.setColor(label.getForeground());
                //g.drawString(label.getText(),
                //        3, f.getBaselineFor('M'));
                }
            }
        }
    }
                    */
}

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

import net.javagaming.java4k.launcher.progress.ParentProgressController;
import net.javagaming.java4k.launcher.progress.ProgressState;

/**
 * @author Groboclown
 */
public class ChildProgressController implements ParentProgressController {
    private final ParentProgressController parent;
    private final int parentRange;
    private ProgressState state;
    private boolean hasChild = false;
    private boolean completed = false;
    private ProgressState beforeChild;
    private int childRange;

    public ChildProgressController(ParentProgressController parent, int parentRange) {
        this.parent = parent;
        this.parentRange = parentRange;
    }


    @Override
    public void advance(String labelMsg, Object... params) {
        String message = labelMsg;
        try {
            message = LauncherBundle.message(labelMsg, params);
        } catch (Exception e) {
            e.printStackTrace();
        }
        setProgressState(new ProgressState(message));
    }

    @Override
    public void advance(int min, int max, int value) {
        setProgressState(new ProgressState(min, max, value));
    }

    @Override
    public void advanceBy(int valueIncr) {
        setProgressState(new ProgressState(state, valueIncr));
    }

    @Override
    public void advanceTo(int value) {
        setProgressState(new ProgressState(state.getLabelText(),
                state.getMinimum(), state.getMaximum(), value));
    }

    @Override
    public void completed() {
        completed = true;
        if (parent != null) {
            if (state == null) {
                state = new ProgressState(0, 1, 1);
            }
            parent.childCompleted(state.getLabelText(), state.isIndeterminate(),
                    toParentRange(state));
        }
    }

    @Override
    public boolean isCancelled() {
        if (parent == null) {
            return false;
        }
        return parent.isCancelled();
    }

    @Override
    public void setProgressState(ProgressState p) {
        validate();
        if (hasChild) {
            throw new IllegalStateException(
                    "cannot advance with an active child");
        }
        this.state = p;
        sendState(p);
    }

    @Override
    public ChildProgressController createChild(int parentRange) {
        validate();
        if (hasChild) {
            throw new IllegalStateException("already have a child");
        }
        boolean updatedState = false;
        if (state == null) {
            state = new ProgressState("");
        }
        if (state.isIndeterminate()) {
            updatedState = true;
            state = new ProgressState(state.getLabelText(), 0, parentRange, 0);
        }
        if (state.getValue() + parentRange > state.getMaximum()) {
            updatedState = true;
            state = new ProgressState(state.getLabelText(), 0,
                    state.getValue() + parentRange, state.getValue());
        }

        if (updatedState) {
            setProgressState(state);
        }
        beforeChild = state;
        hasChild = true;
        childRange = parentRange;
        return new ChildProgressController(this, parentRange);
    }

    @Override
    public void advanceChild(String labelText, boolean indeterminant,
            int value) {
        validate();
        if (! hasChild) {
            throw new IllegalStateException("no existing child");
        }


        if (indeterminant) {
            this.state = new ProgressState(labelText);
        } else {
            value = Math.min(value, childRange);
            this.state = new ProgressState(this.state,
                    beforeChild.getValue() + value);
        }
        sendState(this.state);
    }

    @Override
    public void childCompleted(String labelText, boolean indeterminant,
            int value) {
        advanceChild(labelText, indeterminant, value);
        if (indeterminant && ! beforeChild.isIndeterminate()) {
            // don't use the child's state
            state = beforeChild;
        }
        hasChild = false;
        advanceTo(beforeChild.getValue() + childRange);
        beforeChild = null;
        childRange = -1;
    }


    protected void sendState(ProgressState p) {
        // convert the p state into within the parent range
        if (parent != null) {
            if (p.isIndeterminate()) {
                parent.advanceChild(p.getLabelText(), p.isIndeterminate(),
                        toParentRange(p));
            }
        }
    }

    private int toParentRange(ProgressState p) {
        if (p.getMaximum() == 0) {
            return 0;
        }
        return ((p.getValue() - p.getMinimum()) * parentRange) / p.getMaximum();
    }

    private void validate() {
        if (completed) {
            throw new IllegalStateException("already completed");
        }
    }

    protected ProgressState getProgressState() {
        return state;
    }
}

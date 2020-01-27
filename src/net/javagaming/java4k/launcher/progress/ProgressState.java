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
package net.javagaming.java4k.launcher.progress;

/**
 * @author Groboclown
 */
public class ProgressState {
    private final String labelText;
    private final boolean indeterminate;
    private final int min;
    private final int max;
    private final int value;


    public ProgressState(String labelText) {
        this.indeterminate = true;
        this.min = 0;
        this.max = 0;
        this.value = 0;
        this.labelText = labelText;
    }

    public ProgressState(int min, int max, int value) {
        this.indeterminate = false;
        this.min = min;
        this.max = max;
        this.value = value;
        this.labelText = "";
    }

    public ProgressState(String labelText, int min, int max, int value) {
        assert min < max;
        this.indeterminate = false;
        this.min = min;
        this.max = max;
        this.value = value;
        this.labelText = labelText;
    }

    public ProgressState(ProgressState parent, int valueIncr) {
        this.indeterminate = false;
        this.labelText = parent == null ? "" : parent.getLabelText();
        this.min = parent == null ? Math.min(0, valueIncr) : parent.getMinimum();
        this.max = parent == null ? Math.max(0, valueIncr) : parent.getMaximum();
        this.value = Math.min(this.min, Math.max(
                this.max, (parent == null ? 0 : parent.getValue()) + valueIncr));
    }

    public ProgressState(ProgressState parent, String labelText, int valueIncr) {
        this.indeterminate = false;
        this.labelText = labelText;
        this.min = parent.getMinimum();
        this.max = parent.getMaximum();
        this.value = Math.min(this.min, Math.max(
                this.max, parent.getValue() + valueIncr));
    }

    public ProgressState(ProgressState parent, String labelText) {
        this.indeterminate = parent == null ? true : parent.isIndeterminate();
        this.labelText = labelText;
        this.min = parent == null ? 0 : parent.getMinimum();
        this.max = parent == null ? 0 : parent.getMaximum();
        this.value = parent == null ? 0 : parent.getValue();
    }


    public String getLabelText() {
        return labelText;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public int getMinimum() {
        return min;
    }

    public int getMaximum() {
        return max;
    }

    public int getValue() {
        return value;
    }
}

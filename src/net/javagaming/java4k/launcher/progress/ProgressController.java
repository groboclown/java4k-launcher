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
public interface ProgressController {
    /**
     * Set the progress message to the given bundle message.
     *
     * @param labelMsg bundle message key
     * @param params parameters for the message
     */
    void advance(String labelMsg, Object... params);

    void advance(int min, int max, int value);

    void advanceBy(int valueIncr);

    void setProgressState(ProgressState p);

    void advanceTo(int value);

    /**
     * Informs the progress manager or parent that this process has completed.
     * Future calls to advance methods will result in an error.  Multiple calls
     * to this method are fine; each additional one after the first will be
     * silently ignored.
     */
    void completed();

    /**
     * Is this specific process cancelled?  A parent cancellation will be
     * populated down to all the children.
     *
     * @return true if cancelled.
     */
    boolean isCancelled();

    /**
     * Create a child controller whose progress will be a fraction of the given
     * range within this parent controller.  This controller will not be usable
     * until the child's {@link #completed()} method is called.  Upon being
     * returned, this child controller is active.  When the child is completed,
     * the parent's progress will be advanced to take up the given range.
     *
     * @param parentRange range of the current controller that the child will
     *                    use.
     * @return the child controller.
     */
    ProgressController createChild(int parentRange);
}

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

import java.util.List;

/**
 * A worker, similar to a {@link javax.swing.SwingWorker}, but designed
 * such that we have tight control over its behavior.
 *
 * @param <V> the type that's published
 * @author Groboclown
 */
public abstract class ProgressWorker<V> {

    public static interface Publisher<V> extends ProgressController {
        public void publish(V... values);
    }


    /**
     * Perform an action in a worker thread.  All accumulated data that should
     * be processed in the event dispatch thread needs to be published in
     * the controller by calling
     * {@link ProgressWorker.Publisher#publish(java.util.Collection)}
     * or {@link ProgressWorker.Publisher#publish(Object[])}.
     *
     * @param controller
     * @throws Exception
     */
    public abstract void doInBackground(Publisher<V> controller)
            throws Exception;


    /**
     * Process a set of published values (possibly not all of them).  This will
     * run in the event dispatch thread, so here you can perform UI work that
     * you couldn't do in
     * {@link #doInBackground(ProgressWorker.Publisher)}.
     * This does not take a {@link ProgressController} because the progress
     * should be monitored in the worker, which can be running in parallel
     * to this thread.
     *
     * @param values
     * @throws Exception
     */
    public void process(List<V> values) throws Exception {
        // do nothing by default, so the developer doesn't need to
        // implement this with an empty method every time.
    }


    /**
     * Called in the event dispatch thread when the worker process
     * {@link #doInBackground(ProgressWorker.Publisher)}
     * and all publish processing
     * {@link #process(java.util.List)}
     * is completed.
     *
     * @param controller controller to monitor progress
     * @throws Exception
     */
    public void done(ProgressController controller) throws Exception {
        // do nothing by default, so the developer doesn't need to
        // implement this with an empty method every time.
    }
}

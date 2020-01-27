package net.javagaming.java4k.launcher.ui;

import javax.swing.Icon;
import javax.swing.Timer;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Based on the AnimatedIcon from
 * http://tips4java.wordpress.com/2009/06/21/animated-icon/
 *
 * @author Rob Camick
 * @author Groboclown
 */
public abstract class AbstractAnimatedIcon implements Icon, Runnable,
        ActionListener {
    private final static int DEFAULT_DELAY = 50;
    private final static int DEFAULT_CYCLES = -1;

    public final static float TOP = 0.0f;
    public final static float LEFT = 0.0f;
    public final static float CENTER = 0.5f;
    public final static float BOTTOM = 1.0f;
    public final static float RIGHT = 1.0f;

    private Component component;

    private int cycles;
    private boolean showFirstIcon = false;
    private boolean pendingStart = true;

    private float alignmentX = CENTER;
    private float alignmentY = CENTER;

    //  Track the X, Y location of the Icon within its parent JComponent so we
    //  can request a repaint of only the Icon and not the entire JComponent

    private int iconX;
    private int iconY;

    //  Used for the implementation of Icon interface

    private int iconWidth;
    private int iconHeight;

    //  Use to control processing

    private int currentIconIndex;
    private int cyclesCompleted;
    private boolean animationFinished = true;
    private Timer timer;

    private final Object sync = new Object();


    /**
     * Create an AnimatedIcon that will continuously cycle with the
     * default (500ms).
     *
     * @param component the component the icon will be painted on
     */
    public AbstractAnimatedIcon(Component component) {
        this(component, DEFAULT_DELAY);
    }

    /**
     * Create an AnimatedIcon that will continuously cycle
     *
     * @param component the component the icon will be painted on
     * @param delay     the delay between painting each icon, in milli seconds
     */
    public AbstractAnimatedIcon(Component component, int delay) {
        this(component, delay, DEFAULT_CYCLES);
    }

    /**
     * Create an AnimatedIcon specifying all the properties.
     *
     * @param component the component the icon will be painted on
     * @param delay     the delay between painting each icon, in milli seconds
     * @param cycles    the number of times to repeat the animation sequence
     */
    public AbstractAnimatedIcon(Component component, int delay, int cycles) {
        this.component = component;
        setCycles(cycles);
        timer = new Timer(delay, this);
    }

    /**
     * Calculate the width and height of the Icon based on the maximum
     * width and height of any individual Icon.
     */
    protected abstract Dimension calculateIconDimensions();

    /**
     * Get the number of frames displayed by this Icon.
     *
     * @return the total number of frames in this animation sequence
     */
    public abstract int getFrameCount();

    /**
     *
     * @param c component to render into
     * @param g the graphics context
     * @param x the X coordinate of the icon's top-left corner
     * @param y the Y coordinate of the icon's top-left corner
     * @param cwidth the containing component width
     * @param cheight the containing component height
     * @param frameIndex the frame to paint.
     */
    protected abstract void paintFrame(Component c, Graphics g, int x, int y,
            int cwidth, int cheight, int frameIndex);


    /**
     * Called by the subclass to indicate that the size of the icon was
     * set or changed.  This must be called in the constructor, and then
     * when necessary.
     */
    protected final void updateIconSize() {
        Dimension size = calculateIconDimensions();
        iconWidth = (int) size.getWidth();
        iconHeight = (int) size.getHeight();
    }


    /**
     * Get the alignment of the Icon on the x-axis
     *
     * @return the alignment
     */
    public float getAlignmentX() {
        return alignmentX;
    }

    /**
     * Specify the horizontal alignment of the icon.
     *
     * @param alignmentX common values are LEFT, CENTER (default)  or RIGHT
     *                   although any value between 0.0 and 1.0 can be used
     */
    public void setAlignmentX(float alignmentX) {
        this.alignmentX = alignmentX > 1.0f ? 1.0f : alignmentX < 0.0f ? 0.0f : alignmentX;
    }

    /**
     * Get the alignment of the icon on the y-axis
     *
     * @return the alignment
     */
    public float getAlignmentY() {
        return alignmentY;
    }

    /**
     * Specify the vertical alignment of the Icon.
     *
     * @param alignmentY common values TOP, CENTER (default) or BOTTOM
     *                   although any value between 0.0 and 1.0 can be used
     */
    public void setAlignmentY(float alignmentY) {
        this.alignmentY = alignmentY > 1.0f ? 1.0f : alignmentY < 0.0f ? 0.0f : alignmentY;
    }

    /**
     * Get the index of the currently visible Icon
     *
     * @return the index of the Icon
     */
    public final int getCurrentFrameIndex() {
        return currentIconIndex;
    }

    /**
     * Set the index of the Icon to be displayed and then repaint the Icon.
     *
     * @param index the index of the Icon to be displayed
     */
    public final void setCurrentIconIndex(int index) {
        currentIconIndex = index;

        Rectangle d = calculateRepaintArea(component, iconX, iconY);

        component.repaint(d.x, d.y, d.width, d.height);

    }

    /**
     * Get the cycles to complete before animation stops.
     *
     * @return the number of cycles
     */
    public final int getCycles() {
        return cycles;
    }

    /**
     * Specify the number of times to repeat each animation sequence, or cycle.
     *
     * @param cycles the number of cycles to complete before the animation
     *               stops. The default is -1, which means the animation is
     *               continuous.
     */
    public final void setCycles(int cycles) {
        this.cycles = cycles;
    }

    /**
     * Get the delay between painting each Icon
     *
     * @return the delay
     */
    public final int getDelay() {
        return timer.getDelay();
    }

    /**
     * Specify the delay
     *
     * @param delay the delay between painting eachIcon (in milli seconds)
     */
    public final void setDelay(int delay) {
        timer.setDelay(delay);
    }

    /**
     * Get the showFirstIcon
     *
     * @return the showFirstIcon value
     */
    public final boolean isShowFirstIcon() {
        return showFirstIcon;
    }

    /**
     * Display the first icon when animation is finished. Otherwise the Icon
     * that was visible when the animation stopped will remain visible.
     *
     * @param showFirstIcon true when the first icon is to be displayed,
     *                      false otherwise
     */
    public final void setShowFirstIcon(boolean showFirstIcon) {
        synchronized (sync) {
            this.showFirstIcon = showFirstIcon;
        }
    }

    /**
     * Pause the animation. The animation can be restarted from the
     * current Icon using the restart() method.
     */
    public final void pause() {
        timer.stop();
    }

    /**
     * Start the animation from the beginning.
     */
    public final void start() {
        synchronized (sync) {
            pendingStart = true;

            if (component.isShowing()) {
                realStart();
            }
        }
    }

    /**
     * Start the animation from the beginning.
     */
    private void realStart() {
        synchronized (sync) {
            pendingStart = false;
            if (!timer.isRunning()) {
                setCurrentIconIndex(0);
                animationFinished = false;
                cyclesCompleted = 0;
                timer.start();
            }
        }
    }

    /**
     * Restart the animation from where the animation was paused. Or, if the
     * animation has finished, it will be restarted from the beginning.
     */
    public final void restart() {
        if (!timer.isRunning()) {
            if (animationFinished) {
                start();
            } else {
                timer.restart();
            }
        }
    }

    /**
     * Stop the animation. The first icon will be redisplayed.
     */
    public final void stop() {
        synchronized (sync) {
            timer.stop();
            setCurrentIconIndex(0);
            animationFinished = true;
            pendingStart = false;
        }
    }

//
//  Implement the Icon Interface
//

    /**
     * Gets the width of this icon.
     *
     * @return the width of the icon in pixels.
     */
    @Override
    public final int getIconWidth() {
        return iconWidth;
    }

    /**
     * Gets the height of this icon.
     *
     * @return the height of the icon in pixels.
     */
    @Override
    public final int getIconHeight() {
        return iconHeight;
    }

    /**
     * Paint the icons of this compound icon at the specified location
     *
     * @param c The component on which the icon is painted
     * @param g the graphics context
     * @param x the X coordinate of the icon's top-left corner
     * @param y the Y coordinate of the icon's top-left corner
     */
    @Override
    public final void paintIcon(Component c, Graphics g, int x, int y) {
        //  Saving the x, y coordinates allows us to only repaint the icon and
        //  not the entire component for each animation

        // This allows us to not be sending constant updates until we're
        // actually ready to paint
        synchronized (sync) {
            if (pendingStart) {
                realStart();
            }
        }


        Rectangle d = calculateRepaintArea(c, x, y);

        paintFrame(c, g, d.x, d.y,
                d.width, d.height, getCurrentFrameIndex());
    }

    protected Rectangle calculateRepaintArea(Component c, int x, int y) {
        if (c == component) {
            iconX = x;
            iconY = y;
        }

        int width = getIconWidth();
        int height = getIconHeight();

        int cwidth = c.getWidth();
        int cheight = c.getHeight();

        int offsetX = getOffset(cwidth, width, alignmentX);
        int offsetY = getOffset(cheight, height, alignmentY);


        Rectangle r = new Rectangle(x + offsetX, y + offsetY, width, height);
        return r;
    }

    /*
     *  When the icon value is smaller than the maximum value of all icons the
     *  icon needs to be aligned appropriately. Calculate the offset to be used
     *  when painting the icon to achieve the proper alignment.
     */
    private int getOffset(int maxValue, int iconValue, float alignment) {
        float offset = (maxValue - iconValue) * alignment;
        return Math.round(offset);
    }

//
//  Implement the ActionListener interface
//

    /**
     * Control the animation of the Icons when the Timer fires.
     */
    public void actionPerformed(ActionEvent e) {
        //	Display the next Icon in the animation sequence

        // Setting the current icon index calls repaint
        setCurrentIconIndex(getNextIconIndex(currentIconIndex, getFrameCount()));

        //  Track the number of cycles that have been completed

        if (isCycleCompleted(currentIconIndex, getFrameCount())) {
            cyclesCompleted++;
        }

        //  Stop the animation when the specified number of cycles is completed

        if (cycles > 0
                && cycles <= cyclesCompleted) {
            synchronized (sync) {
                timer.stop();
                animationFinished = true;
            }

            //  Display the first Icon when required
            // This is really inefficient

            if (isShowFirstIcon()
                    && getCurrentFrameIndex() != 0) {

                new Thread(this).start();
            }
        }
    }

    //
//  Implement the Runnable interface
//
    public void run() {
        //  Wait one more delay interval before displaying the first Icon

        try {
            Thread.sleep(timer.getDelay());
            setCurrentIconIndex(0);
        } catch (Exception e) {
            // ignore errors
        }
    }

    /**
     * Get the index of the next Icon to be displayed.
     * <p/>
     * This implementation displays the Icons in the order in which they were
     * added to this class. When the end is reached it will start back at the
     * first Icon.
     * <p/>
     * Typically this method, along with the isCycleCompleted() method, would
     * be extended to provide a custom animation sequence.
     *
     * @param currentIndex the index of the Icon currently displayed
     * @param iconCount    the number of Icons to be displayed
     * @return the index of the next Icon to be displayed
     */
    protected int getNextIconIndex(int currentIndex, int iconCount) {
        return ++currentIndex % iconCount;
    }

    /**
     * Check if the currently visible Icon is the last Icon to be displayed
     * in the animation sequence. If so, this indicates the completion of a
     * single cycle. The animation can continue for an unlimited number of
     * cycles or for a specified number of cycles.
     * <p/>
     * This implemention checks if the last icon is currently displayed.
     * <p/>
     * Typically this method, along with the getNextIconIndex() method, would
     * be extended to provide a custom animation sequence.
     *
     * @param currentIndex the index of the Icon currently displayed
     * @param iconCount    the number of Icons to be displayed
     * @return the index of the next Icon to be displayed
     */
    protected boolean isCycleCompleted(int currentIndex, int iconCount) {
        return currentIndex == iconCount - 1;
    }
}

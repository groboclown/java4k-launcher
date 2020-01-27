package net.javagaming.java4k.launcher.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.color.ColorSpace;

/**
 * Paints a processing icon.
 *
 * @author Groboclown
 */
public class ProcessingIcon extends AbstractAnimatedIcon {
    private static final int ANIMATION_FRAMES = 30;
    private Color foreground = Color.BLUE;
    private Color background = Color.DARK_GRAY;

    private final Dimension iconSize;

    /**
     * Create an AnimatedIcon that will continuously cycle with the
     * default (500ms).
     *
     * @param component the component the icon will be painted on
     * @param size      the height and width of the icon
     */
    public ProcessingIcon(Component component, int size) {
        super(component);
        iconSize = new Dimension(size, size);

        updateIconSize();
    }

    @Override
    protected Dimension calculateIconDimensions() {
        return iconSize;
    }

    @Override
    public int getFrameCount() {
        return ANIMATION_FRAMES;
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
    protected void paintFrame(Component c, Graphics g, int x, int y,
            int cwidth, int cheight, int frameIndex) {
        int arcAngle = ((frameIndex * 360 * 2) / getFrameCount()) - 360;
        int minsize = Math.min(Math.min(cwidth, cheight),
                Math.min(getIconWidth(), getIconHeight()));

        g.setColor(background);
        g.fillRoundRect(x, y, minsize, minsize, 3, 3);
        g.setColor(foreground);
        g.fillArc(x, y, minsize, minsize, 0, arcAngle);
        g.setColor(background);
        int diameter = (int) ((minsize) * 0.5);
        g.fillOval(x + (minsize - diameter)/2, y + (minsize - diameter)/2,
                diameter, diameter);
    }

    public Color getForeground() {
        return foreground;
    }

    public void setForeground(Color foreground) {
        this.foreground = foreground;
    }

    public Color getBackground() {
        return background;
    }

    public void setBackground(Color background) {
        this.background = background;
    }
}

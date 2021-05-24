package Peer;

import java.awt.*;
import java.io.Serializable;

/**
 * Shizhan Xu, 771900
 * University of Melbourne
 * All rights reserved
 */
public class MyShape implements Serializable {
    public enum Shapes {rect, oval, circle, line, text}
    protected int x, y, x1, y1;
    protected Shapes shape;
    protected Color color;
    protected String s;

    /**
     * Construct a non-text shape
     * @param shape the shape of this drawing
     * @param x initial X
     * @param y initial Y
     * @param finalX final X
     * @param finalY final Y
     * @param color the colour of this drawing
     */
    public MyShape(Shapes shape, int x, int y, int finalX, int finalY, Color color) {
        this.x = x;
        this.y = y;
        this.x1 = finalX;
        this.y1 = finalY;
        this.shape = shape;
        this.color = color;
    }

    /**
     * Construct a text drawing
     * @param x initial X
     * @param y initial Y
     * @param s the text input
     * @param color the colour of this text input
     */
    public MyShape(int x, int y, String s, Color color) {
        this.shape = Shapes.text;
        this.x = x;
        this.y = y;
        this.s = s;
        this.color = color;
    }

    /**
     * Draw this drawing onto the whiteboard
     * @param g Graphics of this whiteboard
     */
    protected void draw(Graphics g) {
        int width = x1 - x, height = y1 - y;
        switch (shape) {
            case rect:
                g.setColor(Color.BLACK);
                g.drawRect(x, y, width, height);
                g.setColor(color);
                g.fillRect(x, y, width, height);
                break;
            case line:
                g.setColor(color);
                g.drawLine(x, y, x1, y1);
                break;
            case circle:
                width = Math.min(width, height);
                height = Math.min(width, height);
            case oval:
                g.setColor(Color.BLACK);
                g.drawOval(x, y, width, height);
                g.setColor(color);
                g.fillOval(x, y, width, height);
                break;
            case text:
                g.setColor(color);
                g.drawString(s, x, y);
                break;
        }
    }
}

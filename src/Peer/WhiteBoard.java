package Peer;

import java.awt.*;
import java.awt.event.*;
import java.util.LinkedList;
import javax.swing.*;
/*
 * Created by JFormDesigner on Thu May 20 00:28:26 CST 2021
 */



/**
 * Shizhan Xu, 771900
 * University of Melbourne
 * All rights reserved
 */
public class WhiteBoard extends JPanel {
    private int initialX;
    private int initialY;
    // Keep track of all the drawings performed on this whiteboard
    protected LinkedList<MyShape> drawings = new LinkedList<>();

    protected Color myColor;
    protected MyShape.Shapes shape;

    public WhiteBoard() {
        initComponents();
        myColor = Color.BLACK;
        shape = MyShape.Shapes.rect;
    }

    /**
     * When the mouse is pressed, record the initial X and Y, and add a null
     * to the end of the drawings list for later removal by the mouse dragged
     * event. This is for the preview functionality.
     */
    protected void mousePressed(MouseEvent e) {
        initialX = e.getX();
        initialY = e.getY();
        drawings.add(null);
    }

    /**
     * Retrieve the latest position of the mouse when the mouse is being
     * dragged. If the current selected shape is not text, remove the last
     * shape and draw a new one on this whiteboard according to the latest
     * X and Y to achieve the effect of previewing.
     */
    protected void mouseDragged(MouseEvent e) {
        int finalX = e.getX();
        int finalY = e.getY();
        if (shape != MyShape.Shapes.text){
            drawings.removeLast();
            drawings.add(new MyShape(shape, initialX, initialY, finalX, finalY, myColor));
        }
        repaint();
    }

    /**
     * When mouse is released, draw the text input if shape "text" is selected.
     */
    protected void mouseReleased(MouseEvent e) {
        MyShape last = drawings.getLast();
        if (shape == MyShape.Shapes.text) {
            String input = JOptionPane.showInputDialog("Input your text here please");
            drawings.removeLast();
            if (input != null) {
                drawings.add(new MyShape(initialX, initialY, input, myColor));
            }
            repaint();
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents

        //======== this ========
        setMaximumSize(new Dimension(400, 400));
        setMinimumSize(new Dimension(400, 400));
        setPreferredSize(new Dimension(400, 400));
        setBackground(Color.white);
        setLayout(null);

        setPreferredSize(new Dimension(400, 300));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (MyShape shape: drawings) {
            if (shape != null)
                shape.draw(g);
        }
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}

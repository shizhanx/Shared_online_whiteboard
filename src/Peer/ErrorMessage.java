package Peer;

import javax.swing.*;
import java.awt.*;

/**
 * Shizhan Xu, 771900
 * University of Melbourne
 * All rights reserved
 */
public class ErrorMessage {
    /**
     * Incorrect arguments leading to system exit.
     */
    public static void argError(String msg) {
        System.err.println("Argument error: " + msg);
        System.exit(1);
    }

    /**
     * Connection failed, pop up a notice and exit.
     */
    public static void connectionError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent,
                "Connection error: " + msg,
                "Error",
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    /**
     * The user input is somehow incorrect.
     */
    public static void inputError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent,
                "Input error: " + msg,
                "Error",
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * There shouldn't be such an error happening, possibly a bug exists.
     */
    public static void unexpectedError(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent,
                "Unexpected error: " + msg,
                "There's a bug!",
                JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    /**
     * Successfully done something, show some info to the user.
     */
    public static void successMessage(Component parent, String msg) {
        JOptionPane.showMessageDialog(parent,
                msg,
                "Success",
                JOptionPane.INFORMATION_MESSAGE);
    }
}

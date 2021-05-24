/*
 * Created by JFormDesigner on Mon May 17 21:18:49 CST 2021
 */

package Peer;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

/**
 * Shizhan Xu, 771900
 * University of Melbourne
 * All rights reserved
 */
public class UI extends JFrame implements IRemoteDraw {

    public static void main(String[] args) {
        // Ensure one argument input
        if (args.length != 1) {
            ErrorMessage.argError("need 1 arguments");
        }

        UI ui = new UI(args[0]);
        ui.setVisible(true);
        ui.exportThis();
    }

    private Registry registry;
    protected String name;
    private boolean admin;
    private String adminName;
    private final Map<String, IRemoteDraw> peers = new HashMap<>();

    private final CardLayout cardLayout;
    DefaultListModel<String> listModel;

    /**
     * Construct a new peer UI with the given user name
     * @param name the name of this peer user.
     */
    public UI(String name) {
        initComponents();
        // Adopt the naming method of Blizzard Games and add a random
        // number after the name to avoid duplication
        this.name = name + '#' + (new Random().nextInt(9000) + 1000);
        ErrorMessage.successMessage(this,
                "Welcome! Your id is " + this.name);
        setTitle(this.name + "'s whiteboard");

        cardLayout = (CardLayout) getContentPane().getLayout();
        listModel = new DefaultListModel<>();
        clientList.setModel(listModel);

        try {
            registry = LocateRegistry.getRegistry();
        } catch (RemoteException e) {
            ErrorMessage.connectionError(this,
                    "Failed to join the RMI registry.");
        }
    }

    /**
     * export the current UI to the RMI registry.
     */
    private void exportThis() {
        // While true loop to continuously trying to export when user name duplication occurs.
        while (true) {
            try {
                IRemoteDraw stub = (IRemoteDraw) UnicastRemoteObject.exportObject(this, 0);
                registry.bind(name, stub);
                break;
            } catch (AlreadyBoundException alreadyBoundException) {
                // If the name is already in use, generate a new user name and try again
                name = name.substring(0, name.length() - 4) + (new Random().nextInt(9000) + 1000);
                setTitle(name + "'s whiteboard");
            } catch (AccessException accessException) {
                ErrorMessage.unexpectedError(this,
                        "The RMI registry is local.");
            } catch (RemoteException remoteException) {
                ErrorMessage.unexpectedError(this,
                        "Cannot export this RMI object, something went wrong.");
            }
        }
    }

    /**
     * Create a new whiteboard and set the current peer to admin.
     */
    private void createButtonActionPerformed(ActionEvent e) {
        admin = true;
        ErrorMessage.successMessage(this, "Welcome to your whiteboard");
        cardLayout.show(getContentPane(), "whiteBoardPanel");
    }

    /**
     * Set thisTry to join the whiteboard with the name input by the user. Upon success,
     * disable those functionalities only for admins.
     */
    private void joinButtonActionPerformed(ActionEvent e) {
        admin = false;
        adminName = JOptionPane.showInputDialog("Please input the admin's name here: ");
        if (adminName != null) {
            try {
                IRemoteDraw adminStub = (IRemoteDraw) registry.lookup(adminName);
                // request to join the admin's whiteboard
                if (adminStub.requestToJoin(name)) {
                    // Upon success, save the admin's remote stub
                    peers.put(adminName, adminStub);
                    ErrorMessage.successMessage(this, "Welcome to " +
                            adminName + "'s whiteboard");
                    setTitle(adminName + "'s whiteboard");
                    // Disable functionalities only for the admin
                    clientList.setVisible(false);
                    kickButton.setEnabled(false);
                    fileMenu.setEnabled(false);
                    cardLayout.show(getContentPane(), "whiteBoardPanel");
                } else {
                    ErrorMessage.inputError(this,
                            "You were refused to join, or maybe the other side is not an admin.");
                }
            } catch (RemoteException remoteException) {
                ErrorMessage.unexpectedError(this,
                        "Connection failed.");
            } catch (NotBoundException notBoundException) {
                ErrorMessage.inputError(this,
                        "No such admin in the current server.");
            }
        }
    }

    /**
     * The user selected a shape from the shapes menu.
     */
    private void shapeButtonItemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getItem();
            if (rectShapeButton.equals(item)) {
                whiteBoard.shape = MyShape.Shapes.rect;
            } else if (ovalShapeButton.equals(item)) {
                whiteBoard.shape = MyShape.Shapes.oval;
            } else if (circleShapeButton.equals(item)) {
                whiteBoard.shape = MyShape.Shapes.circle;
            } else if (lineShapeButton.equals(item)) {
                whiteBoard.shape = MyShape.Shapes.line;
            } else if (textShapeButton.equals(item)) {
                whiteBoard.shape = MyShape.Shapes.text;
            }
        }
    }

    /**
     * The user clicked on colour selection button
     */
    private void colourChoiceButtonActionPerformed(ActionEvent e) {
        Color result = JColorChooser.showDialog(this, "Choose a colour pls", null);
        if (result != null) {
            colourChoiceButton.setBackground(result);
            whiteBoard.myColor = result;
        }
    }

    private void whiteBoardMousePressed(MouseEvent e) {
        whiteBoard.mousePressed(e);
    }

    private void whiteBoardMouseDragged(MouseEvent e) {
        whiteBoard.mouseDragged(e);
    }

    /**
     * The user has confirmed an input of a shape by releasing the mouse.
     * Besides this single whiteboard, also updates all the peers.
     * Closes the whiteboard when the admin becomes unreachable.
     */
    private void whiteBoardMouseReleased(MouseEvent e) {
        whiteBoard.mouseReleased(e);
        try {
            if (admin) {
                updateWhiteBoard(whiteBoard.drawings);
            } else {
                peers.get(adminName).updateWhiteBoard(whiteBoard.drawings);
            }
        } catch (RemoteException remoteException) {
            ErrorMessage.connectionError(this,
                    "Admin has left, closing now.");
        }
    }

    /**
     * Kick the selected peer from this whiteboard.
     */
    private void kickButtonActionPerformed(ActionEvent e) {
        String clientName = clientList.getSelectedValue();
        if (clientName != null) {
            try {
                peers.get(clientName).kick();
            } catch (RemoteException remoteException) {
                // Whether the peer has already left or has just been kicked, this
                // exception will occur.
                ErrorMessage.successMessage(this,
                        clientName + " has been kicked");
            }
            peers.remove(clientName);
            updateClientList();
        }
    }

    /**
     * Update the client list on the UI after updating the internal data structure
     */
    private void updateClientList() {
        listModel.removeAllElements();
        for (String peer: peers.keySet())
            listModel.addElement(peer);
    }

    /**
     * First prompt the admin to save the current whiteboard, then create a new empty
     * whiteboard and update all the peers.
     */
    private void menuItemNewActionPerformed(ActionEvent e) {
        boolean isCancelled = false;
        switch (JOptionPane.showOptionDialog(this, "Save this whiteboard?",
                "Creating new", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[] {"Save", "Save as", "Don't save", "Cancel"}, null)) {
            case 0:
                menuItemSaveActionPerformed(e);
                break;
            case 1:
                menuItemSaveAsActionPerformed(e);
                break;
            case 2:
                break;
            case 3:
            case -1:
                isCancelled = true;
                break;
        }
        if (!isCancelled) {
            try {
                updateWhiteBoard(new LinkedList<>());
            } catch (RemoteException remoteException) {
                ErrorMessage.unexpectedError(this,
                        "remote exception happened when updating admin's own whiteboard");
            }
        }
    }

    /**
     * Try to open a whiteboard from an existing txt file, whose name is
     * input by the admin.
     */
    private void menuItemOpenActionPerformed(ActionEvent e) {
        String fileName = JOptionPane.showInputDialog("File name: ");
        if (fileName != null) {
            File file = new File(fileName + ".txt");
            try (BufferedReader r = new BufferedReader(new FileReader(file))) {
                whiteBoard.drawings.clear();
                String line;
                while ((line = r.readLine()) != null) {
                    String[] a = line.split(",");
                    if (a[0].equals("text")) {
                        whiteBoard.drawings.add(new MyShape(Integer.parseInt(a[1]),
                                Integer.parseInt(a[2]), a[3], new Color(Integer.parseInt(a[4]))));
                    } else {
                        whiteBoard.drawings.add(new MyShape(MyShape.Shapes.valueOf(a[0]),
                                Integer.parseInt(a[1]), Integer.parseInt(a[2]),
                                Integer.parseInt(a[3]), Integer.parseInt(a[4]),
                                new Color(Integer.parseInt(a[5]))));
                    }
                }
                updateWhiteBoard(whiteBoard.drawings);
            } catch (FileNotFoundException fileNotFoundException) {
                ErrorMessage.inputError(this, "No such file");
            } catch (IOException ioException) {
                ErrorMessage.unexpectedError(this, "Error reading the file");
            }
        }
    }

    /**
     * The helper method for saving a whiteboard to a file. The whiteboard is saved
     * by serializing all the shapes on it to comma separated strings.
     * @param file the file that the system saves to
     */
    private void saveHelper(File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            for (MyShape shape : whiteBoard.drawings) {
                // Serialize a shape according to its corresponding constructor.
                if (shape.shape == MyShape.Shapes.text) {
                    bw.write(MyShape.Shapes.text.toString() + "," +
                            shape.x + "," + shape.y + "," + shape.s + "," + shape.color.getRGB());
                } else {
                    bw.write(shape.shape.toString() + "," + shape.x + "," + shape.y + "," +
                            shape.x1 + "," + shape.y1 + "," + shape.color.getRGB());
                }
                bw.newLine();
            }
        } catch (IOException ioException) {
            ErrorMessage.unexpectedError(this, "File IO error");
        }
    }

    /**
     * Save the current whiteboard with the admin's name
     */
    private void menuItemSaveActionPerformed(ActionEvent e) {
        File file = new File(name + ".txt");
        saveHelper(file);
    }

    /**
     * Save the current whiteboard with the name provided by the user.
     */
    private void menuItemSaveAsActionPerformed(ActionEvent e) {
        String fileName = JOptionPane.showInputDialog("File name: ");
        if (fileName != null) {
            File file = new File(fileName + ".txt");
            if (file.exists()) {
                ErrorMessage.inputError(this, "File already exists");
            } else {
                saveHelper(file);
            }
        }
    }

    /**
     * Close the whiteboard after prompting the user to save the whiteboard.
     */
    private void menuItemCloseActionPerformed(ActionEvent e) {
        switch (JOptionPane.showOptionDialog(this, "Save this whiteboard?",
                "Closing", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
                null, new String[] {"Save", "Save as", "Don't save", "Cancel"}, null)) {
            case 0:
                menuItemSaveActionPerformed(e);
                System.exit(0);
                break;
            case 1:
                menuItemSaveAsActionPerformed(e);
                System.exit(0);
                break;
            case 2:
                System.exit(0);
                break;
            case 3:
            case -1:
                break;
        }
    }

    /**
     * Whenever the peer presses enter on the chat input field, transfer
     * the input field's content to the chat box and synchronize all the peers.
     */
    private void chatInputFieldActionPerformed(ActionEvent e) {
        try {
            String input = e.getActionCommand();
            if (!input.isEmpty()) {
                chatInputField.setText("");
                updateChatBox(chatBox.getText() + name + ": " + input + "\n");
                if (!admin)
                    peers.get(adminName).updateChatBox(chatBox.getText());
            }
        } catch (RemoteException remoteException) {
            ErrorMessage.connectionError(this,
                    "Admin has left, closing now.");
        }
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        introPanel = new JPanel();
        textField3 = new JTextField();
        textField4 = new JTextField();
        initialTitle = new JTextField();
        createButton = new JButton();
        joinButton = new JButton();
        mySignature = new JTextField();
        whiteBoardPanel = new JPanel();
        menuBar1 = new JMenuBar();
        fileMenu = new JMenu();
        menuItemNew = new JMenuItem();
        menuItemOpen = new JMenuItem();
        menuItemSave = new JMenuItem();
        menuItemSaveAs = new JMenuItem();
        menuItemClose = new JMenuItem();
        shapeMenu = new JMenuBar();
        rectShapeButton = new JRadioButtonMenuItem();
        circleShapeButton = new JRadioButtonMenuItem();
        ovalShapeButton = new JRadioButtonMenuItem();
        lineShapeButton = new JRadioButtonMenuItem();
        textShapeButton = new JRadioButtonMenuItem();
        colourChoiceButton = new JButton();
        chatBoxTitle = new JTextField();
        whiteBoard = new WhiteBoard();
        clientListText = new JTextField();
        chatBoxPane = new JScrollPane();
        chatBox = new JTextArea();
        clientListPane = new JScrollPane();
        clientList = new JList<>();
        chatInputField = new JTextField();
        kickButton = new JButton();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);
        setTitle("Whiteboard");
        Container contentPane = getContentPane();
        contentPane.setLayout(new CardLayout());

        //======== introPanel ========
        {
            introPanel.setLayout(new GridBagLayout());
            ((GridBagLayout)introPanel.getLayout()).columnWidths = new int[] {400, 400, 0};
            ((GridBagLayout)introPanel.getLayout()).rowHeights = new int[] {50, 50, 150, 100, 80, 0};
            ((GridBagLayout)introPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 1.0E-4};
            ((GridBagLayout)introPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

            //---- textField3 ----
            textField3.setEditable(false);
            textField3.setText("Welcome to the GUI of");
            textField3.setHorizontalAlignment(SwingConstants.CENTER);
            textField3.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 16));
            introPanel.add(textField3, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- textField4 ----
            textField4.setEditable(false);
            textField4.setText("the ONE and the ONLY");
            textField4.setHorizontalAlignment(SwingConstants.CENTER);
            textField4.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
            introPanel.add(textField4, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- initialTitle ----
            initialTitle.setEditable(false);
            initialTitle.setFont(new Font("Footlight MT Light", Font.BOLD, 40));
            initialTitle.setText("ONLINE WHITEBOARD");
            initialTitle.setHorizontalAlignment(SwingConstants.CENTER);
            introPanel.add(initialTitle, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- createButton ----
            createButton.setText("Create");
            createButton.setPreferredSize(new Dimension(200, 30));
            createButton.setFont(createButton.getFont().deriveFont(20f));
            createButton.addActionListener(e -> createButtonActionPerformed(e));
            introPanel.add(createButton, new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- joinButton ----
            joinButton.setText("Join");
            joinButton.setPreferredSize(new Dimension(200, 30));
            joinButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
            joinButton.addActionListener(e -> joinButtonActionPerformed(e));
            introPanel.add(joinButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- mySignature ----
            mySignature.setEditable(false);
            mySignature.setText("Shizhan Xu, 771900, All rights reserved");
            mySignature.setHorizontalAlignment(SwingConstants.TRAILING);
            introPanel.add(mySignature, new GridBagConstraints(0, 4, 2, 1, 0.0, 0.0,
                GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(introPanel, "introPanel");

        //======== whiteBoardPanel ========
        {
            whiteBoardPanel.setLayout(new GridBagLayout());
            ((GridBagLayout)whiteBoardPanel.getLayout()).columnWidths = new int[] {85, 85, 465, 165, 0};
            ((GridBagLayout)whiteBoardPanel.getLayout()).rowHeights = new int[] {30, 50, 300, 50, 0, 0};
            ((GridBagLayout)whiteBoardPanel.getLayout()).columnWeights = new double[] {0.0, 0.0, 0.0, 0.0, 1.0E-4};
            ((GridBagLayout)whiteBoardPanel.getLayout()).rowWeights = new double[] {0.0, 0.0, 0.0, 0.0, 0.0, 1.0E-4};

            //======== menuBar1 ========
            {

                //======== fileMenu ========
                {
                    fileMenu.setText("File menu");

                    //---- menuItemNew ----
                    menuItemNew.setText("new");
                    menuItemNew.addActionListener(e -> menuItemNewActionPerformed(e));
                    fileMenu.add(menuItemNew);

                    //---- menuItemOpen ----
                    menuItemOpen.setText("open");
                    menuItemOpen.addActionListener(e -> menuItemOpenActionPerformed(e));
                    fileMenu.add(menuItemOpen);

                    //---- menuItemSave ----
                    menuItemSave.setText("save");
                    menuItemSave.addActionListener(e -> menuItemSaveActionPerformed(e));
                    fileMenu.add(menuItemSave);

                    //---- menuItemSaveAs ----
                    menuItemSaveAs.setText("save as");
                    menuItemSaveAs.addActionListener(e -> menuItemSaveAsActionPerformed(e));
                    fileMenu.add(menuItemSaveAs);

                    //---- menuItemClose ----
                    menuItemClose.setText("close");
                    menuItemClose.addActionListener(e -> menuItemCloseActionPerformed(e));
                    fileMenu.add(menuItemClose);
                }
                menuBar1.add(fileMenu);
            }
            whiteBoardPanel.add(menuBar1, new GridBagConstraints(0, 0, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== shapeMenu ========
            {

                //---- rectShapeButton ----
                rectShapeButton.setText("Rectangle");
                rectShapeButton.setSelected(true);
                rectShapeButton.addItemListener(e -> shapeButtonItemStateChanged(e));
                shapeMenu.add(rectShapeButton);

                //---- circleShapeButton ----
                circleShapeButton.setText("Circle");
                circleShapeButton.addItemListener(e -> shapeButtonItemStateChanged(e));
                shapeMenu.add(circleShapeButton);

                //---- ovalShapeButton ----
                ovalShapeButton.setText("Oval");
                ovalShapeButton.addItemListener(e -> shapeButtonItemStateChanged(e));
                shapeMenu.add(ovalShapeButton);

                //---- lineShapeButton ----
                lineShapeButton.setText("Line");
                lineShapeButton.addItemListener(e -> shapeButtonItemStateChanged(e));
                shapeMenu.add(lineShapeButton);

                //---- textShapeButton ----
                textShapeButton.setText("Text");
                textShapeButton.addItemListener(e -> shapeButtonItemStateChanged(e));
                shapeMenu.add(textShapeButton);
            }
            whiteBoardPanel.add(shapeMenu, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- colourChoiceButton ----
            colourChoiceButton.setText("Choose your colour here");
            colourChoiceButton.setForeground(Color.white);
            colourChoiceButton.setBackground(Color.black);
            colourChoiceButton.addActionListener(e -> colourChoiceButtonActionPerformed(e));
            whiteBoardPanel.add(colourChoiceButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- chatBoxTitle ----
            chatBoxTitle.setText("Chat box");
            chatBoxTitle.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
            chatBoxTitle.setEditable(false);
            whiteBoardPanel.add(chatBoxTitle, new GridBagConstraints(0, 1, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- whiteBoard ----
            whiteBoard.setMinimumSize(new Dimension(400, 400));
            whiteBoard.setMaximumSize(new Dimension(400, 400));
            whiteBoard.setPreferredSize(new Dimension(400, 400));
            whiteBoard.setBackground(Color.white);
            whiteBoard.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    whiteBoardMousePressed(e);
                }
                @Override
                public void mouseReleased(MouseEvent e) {
                    whiteBoardMouseReleased(e);
                }
            });
            whiteBoard.addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    whiteBoardMouseDragged(e);
                }
            });
            whiteBoardPanel.add(whiteBoard, new GridBagConstraints(2, 1, 1, 3, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- clientListText ----
            clientListText.setText("Client List");
            clientListText.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
            clientListText.setEditable(false);
            whiteBoardPanel.add(clientListText, new GridBagConstraints(3, 1, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== chatBoxPane ========
            {
                chatBoxPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                chatBoxPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED));

                //---- chatBox ----
                chatBox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
                chatBox.setLineWrap(true);
                chatBox.setEditable(false);
                chatBoxPane.setViewportView(chatBox);
            }
            whiteBoardPanel.add(chatBoxPane, new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //======== clientListPane ========
            {
                clientListPane.setPreferredSize(new Dimension(69, 200));

                //---- clientList ----
                clientList.setFont(new Font("Arial", Font.BOLD, 22));
                clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                clientList.setModel(new AbstractListModel<String>() {
                    String[] values = {
                        "sb#1234"
                    };
                    @Override
                    public int getSize() { return values.length; }
                    @Override
                    public String getElementAt(int i) { return values[i]; }
                });
                clientListPane.setViewportView(clientList);
            }
            whiteBoardPanel.add(clientListPane, new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- chatInputField ----
            chatInputField.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
            chatInputField.setText("Input your message here");
            chatInputField.setToolTipText("Enter to send");
            chatInputField.addActionListener(e -> chatInputFieldActionPerformed(e));
            whiteBoardPanel.add(chatInputField, new GridBagConstraints(0, 3, 2, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));

            //---- kickButton ----
            kickButton.setText("Kick! ");
            kickButton.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 20));
            kickButton.addActionListener(e -> kickButtonActionPerformed(e));
            whiteBoardPanel.add(kickButton, new GridBagConstraints(3, 3, 1, 1, 0.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 0, 0, 0), 0, 0));
        }
        contentPane.add(whiteBoardPanel, "whiteBoardPanel");
        pack();
        setLocationRelativeTo(getOwner());

        //---- shapeChoices ----
        ButtonGroup shapeChoices = new ButtonGroup();
        shapeChoices.add(rectShapeButton);
        shapeChoices.add(circleShapeButton);
        shapeChoices.add(ovalShapeButton);
        shapeChoices.add(lineShapeButton);
        shapeChoices.add(textShapeButton);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel introPanel;
    private JTextField textField3;
    private JTextField textField4;
    private JTextField initialTitle;
    private JButton createButton;
    private JButton joinButton;
    private JTextField mySignature;
    private JPanel whiteBoardPanel;
    private JMenuBar menuBar1;
    private JMenu fileMenu;
    private JMenuItem menuItemNew;
    private JMenuItem menuItemOpen;
    private JMenuItem menuItemSave;
    private JMenuItem menuItemSaveAs;
    private JMenuItem menuItemClose;
    private JMenuBar shapeMenu;
    private JRadioButtonMenuItem rectShapeButton;
    private JRadioButtonMenuItem circleShapeButton;
    private JRadioButtonMenuItem ovalShapeButton;
    private JRadioButtonMenuItem lineShapeButton;
    private JRadioButtonMenuItem textShapeButton;
    private JButton colourChoiceButton;
    private JTextField chatBoxTitle;
    private WhiteBoard whiteBoard;
    private JTextField clientListText;
    private JScrollPane chatBoxPane;
    private JTextArea chatBox;
    private JScrollPane clientListPane;
    private JList<String> clientList;
    private JTextField chatInputField;
    private JButton kickButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables

    /**
     * Update this whiteboard with the list of drawings provided.
     * If this is the admin, update all other peers as well.
     * Remove any unreachable peer in the list: he's already left!
     *
     * @param drawings a list of shapes to be put into the whiteboard.
     */
    @Override
    public void updateWhiteBoard(LinkedList<MyShape> drawings) throws RemoteException {
        whiteBoard.drawings = drawings;
        whiteBoard.repaint();
        if (admin) {
            for (String peer: peers.keySet()) {
                try {
                    peers.get(peer).updateWhiteBoard(drawings);
                } catch (RemoteException remoteException) {
                    peers.remove(peer);
                }
            }
            updateClientList();
        }
    }

    /**
     * Update the chat box with a new message list.
     * If this is the admin, update all other peers as well.
     *
     * @param s the new message list.
     */
    @Override
    public void updateChatBox(String s) throws RemoteException {
        chatBox.setText(s);
        if (admin) {
            for (String peer: peers.keySet()) {
                try {
                    peers.get(peer).updateChatBox(s);
                } catch (RemoteException remoteException) {
                    peers.remove(peer);
                }
            }
            updateClientList();
        }
    }

    /**
     * Kick this client and notify him.
     *
     */
    @Override
    public void kick() throws RemoteException {
        if (!admin) {
            ErrorMessage.connectionError(this,
                    "You have been kicked by the admin");
        }
    }

    /**
     * Request to join this whiteboard
     *
     * @param name your own user name
     * @return whether you are approved to join
     */
    @Override
    public boolean requestToJoin(String name) throws RemoteException {
        try {
            IRemoteDraw stub = (IRemoteDraw) registry.lookup(name);
            if (admin) {
                if (JOptionPane.showConfirmDialog(this,
                        name + " wants to join.",
                        "Joining",
                        JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    peers.put(name, stub);
                    listModel.addElement(name);
                    stub.updateWhiteBoard(whiteBoard.drawings);
                    return true;
                }
            }
        } catch (RemoteException e) {
            ErrorMessage.connectionError(this,
                    "Connection to the RMI registry has failed.");
        } catch (NotBoundException e) {
            ErrorMessage.connectionError(this,
                    "No such name in the registry when \"" + name + "\" tried to join");
        }
        return false;
    }

}

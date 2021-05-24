package Peer;


import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.LinkedList;

/**
 * Shizhan Xu, 771900
 * University of Melbourne
 * All rights reserved
 */
public interface IRemoteDraw extends Remote {

    /**
     * Update this whiteboard with the list of drawings provided.
     * If this is the admin, update all other peers as well.
     * @param drawings a list of shapes to be put into the whiteboard.
     */
    void updateWhiteBoard(LinkedList<MyShape> drawings) throws RemoteException;

    /**
     * Update the chat box with a new message list.
     * If this is the admin, update all other peers as well.
     * @param s the new message list.
     */
    void updateChatBox(String s) throws RemoteException;

    /**
     * Kick this client and notify him.
     */
    void kick() throws RemoteException;

    /**
     * Request to join this whiteboard
     * @param name your own user name
     * @return whether you are approved to join
     */
    boolean requestToJoin(String name) throws RemoteException;

}

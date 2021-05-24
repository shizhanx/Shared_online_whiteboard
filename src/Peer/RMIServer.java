package Peer;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * Shizhan Xu, 771900
 * University of Melbourne
 * All rights reserved
 */
public class RMIServer {
    public static void main(String[] args) {
        try {
            LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
            while (true);
        } catch (RemoteException remoteException) {
            ErrorMessage.argError("Cannot create RMI registry, probably already running.");
        }
    }
}

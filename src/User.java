/* This class represents a user and its tools (i.e drawings, nickname...) */

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class User {

    private User() {}
    public static void main(String[] args) {
        try {
            // Getting the registry
            Registry registry = LocateRegistry.getRegistry(null);

            // Looking up the registry for the remote object
            ServerSide stub = (ServerSide) registry.lookup("ServerSide");

            // Calling the remote method using the obtained object
            stub.printMsg();

            // System.out.println("Remote method invoked");
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

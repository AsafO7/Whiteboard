package TestRMI;

import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;

public class Server implements IServerHandler {

    public Server() {}

    public String sayHello() {
        return "Hello, world!";
    }

    public static void main(String args[]) {

        try {
            ServerHandler obj = new ServerHandler();
            IServerHandler stub = (IServerHandler) UnicastRemoteObject.exportObject(obj, 0);

            // Create an RMI registry
            Registry registry = LocateRegistry.createRegistry(0); // Use 0 for a random port!

            // Bind the remote object's stub in the registry
//            Registry registry = LocateRegistry.getRegistry(2000);
            registry.bind("IServerHandler1", stub);

            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class Server extends  ServerSideImp{

    public Server() {}
    public static void main(String args[]) {
        try {
            // Instantiating the implementation class
            ServerSideImp obj = new ServerSideImp();

            // Exporting the object of implementation class
            // (here we are exporting the remote object to the stub)
            ServerSide stub = (ServerSide) UnicastRemoteObject.exportObject(obj, 0);

            // Binding the remote object (stub) in the registry
            Registry registry = LocateRegistry.getRegistry();

            registry.bind("ServerSide", stub);
            System.err.println("Server ready");
        } catch (Exception e) {
            System.err.println("Server exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

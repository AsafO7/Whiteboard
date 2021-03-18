package whiteboard;

import java.net.ServerSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class CreateRMIRegistry {
    private int port;
    private Registry registry;

    public int getPort() {
        return port;
    }

    public Registry getRegistry() {
        return registry;
    }

    // Creates a new RMI registry with a random port number, and returns it's port
    public CreateRMIRegistry() {
        // Create an RMI registry
        int port = 0;
        Registry registry = null;
        long startTime = System.currentTimeMillis();
        while (true) {
            if (System.currentTimeMillis() - startTime > 2 * 1000) {
                System.err.println("Couldn't find a port!");
                System.exit(-1);
            }
            ServerSocket serverSocket = null;
            try {
                serverSocket = new ServerSocket(0);
                port = serverSocket.getLocalPort();
                serverSocket.close();
                registry = LocateRegistry.createRegistry(port); // Use 0 for a random port!
                break;
            }
            catch (Exception e) {}
            finally {
                try {
                    if (serverSocket != null) {
                        serverSocket.close();
                    }
                }
                catch (Exception e) {
                    System.out.println("Couldn't close the server socket");
                    System.exit(-1);
                }
            }
        }
        this.port = port;
        this.registry = registry;
    }
}

package whiteboard.server;

import whiteboard.Connection;
import whiteboard.CreateRMIRegistry;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    public static List<Handler> handlers = Collections.synchronizedList(new ArrayList<>());
    public static List<Thread> handlerThreads = Collections.synchronizedList(new ArrayList<>());

    private static List<Room> rooms = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        ServerSocket srv = null;
        try {
            srv = new ServerSocket(Connection.PORT);
            System.out.println("Server started");

            while(true) { // Keep waiting for a new client.
                //TODO: Use socket communication with the client to tell him about a new RMI registry server that we opened for him,
                // then close the socket, and then move on with RMI based communication.
                Socket socket = srv.accept();
                System.out.println("Session with " + socket.toString() + "Started");
                try {
                    Handler handler = new Handler(rooms, handlers);
                    handlers.add(handler);

                    // Export the handler for RMI
                    IServerHandler stub = (IServerHandler) UnicastRemoteObject.exportObject(handler, 0);

                    // Creates a new RMI registry with a random port
                    CreateRMIRegistry createRMIRegistry = new CreateRMIRegistry();
                    int port = createRMIRegistry.getPort();
                    Registry registry = createRMIRegistry.getRegistry();

                    // Register the handler's stub in the RMI's registry
                    registry.bind("IServerHandler", stub);

                    // Send the port to the client
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                    out.writeInt(port);
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    try {
                        in.readBoolean();
                    } catch (Exception e) {}
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    if (socket != null) socket.close();
                }
            }
        }
        catch(IOException e) {
            e.printStackTrace();
        }
        finally {
            if (srv != null) {
                try {
                    srv.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Server is closed");
        }
    }
}

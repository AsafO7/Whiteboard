package whiteboard.server;

import whiteboard.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Server {
    public static List<Handler> handlers = Collections.synchronizedList(new ArrayList<>());
    public static List<Thread> handlerThreads = Collections.synchronizedList(new ArrayList<>());

    private static List<Room> rooms = Collections.synchronizedList(new ArrayList<>());

//    //    For the purpose of testing "Refresh Rooms" on the client:
//    private static List<Room> rooms = Collections.synchronizedList(new ArrayList<Room>(Arrays.asList(
//            new Room("rave"), new Room("asaf"), new Room("tamik")
//    )));

    public static void main(String[] args) {
        ServerSocket srv = null;
        try {
            srv = new ServerSocket(Connection.PORT);
            System.out.println("Server started");

            while(true) { // Keep waiting for a new client.
                Socket socket = srv.accept();
                System.out.println("Session with " + socket.toString() + "Started");
                try {
                    Handler handler = new Handler(socket, rooms);
                    Thread handlerThread = new Thread(handler);
                    handlerThread.start();
                    handlers.add(handler);
                    handlerThreads.add(handlerThread);
                }
                catch (Exception e) {
                    e.printStackTrace();
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

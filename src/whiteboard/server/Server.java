package whiteboard.server;

import whiteboard.Connection;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Server {
    private static Handler handler;
    private static Thread handlerThread;

    private static List<Room> rooms = Collections.synchronizedList(new ArrayList<Room>());

    public static void main(String[] args) {
        ServerSocket srv = null;
        try {
            srv = new ServerSocket(Connection.PORT);
            System.out.println("Server started");

            while(true) {
                Socket socket = srv.accept();
                try {
                    handler = new Handler(socket, rooms);
                    handlerThread = new Thread(handler);
                    handlerThread.start();
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

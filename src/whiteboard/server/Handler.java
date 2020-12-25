package whiteboard.server;

import whiteboard.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Handler implements Runnable {

    private final Socket socket;
    private final ConcurrentLinkedQueue<Packet> outQueue;
    private final List<Room> rooms;
    private Thread OutputHandlerThread;
    private OutputHandler outputHandler;

    public Handler(Socket socket, List<Room> rooms) {
        this.socket = socket;
        this.outQueue = new ConcurrentLinkedQueue<Packet>();
        this.rooms = rooms;
    }

    @Override
    public void run() {
        ObjectOutputStream out = null;
        ObjectInputStream in = null;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            outputHandler = new OutputHandler(out, outQueue);
            OutputHandlerThread = new Thread(outputHandler);
            OutputHandlerThread.start();

            while (true) {
                Packet packet = (Packet)in.readObject();
                Packet.Type type = packet.getType();
                switch (type) {
                    case GET_ROOMS:
                        this.handleGetRooms();
                    default:
                        throw new Exception("Error: server received "
                                + "unexpected packet type");
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            if (out != null)
            {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleGetRooms() {
        List<String> roomsNames= new ArrayList<String>();
        synchronized (rooms) {
            for (Room room : rooms) {
                roomsNames.add(room.getName());
            }
        }
        outQueue.add(Packet.createRoomsNames(roomsNames));
    }
}

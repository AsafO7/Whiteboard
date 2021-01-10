package whiteboard.server;

import whiteboard.Packet;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class Handler implements Runnable {

    private final Socket socket;
    private final BlockingQueue<Packet> outQueue = new LinkedBlockingQueue<Packet>();
    private final List<Room> rooms;
    private Thread OutputHandlerThread;
    private OutputHandler outputHandler;

    public Handler(Socket socket, List<Room> rooms) {
        this.socket = socket;
        this.rooms = rooms;
    }

    @Override
    public void run() {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(socket.getInputStream());

            outputHandler = new OutputHandler(socket, outQueue);
            OutputHandlerThread = new Thread(outputHandler);
            OutputHandlerThread.start();

            while (true) {
                Packet packet = (Packet) in.readObject();
                Packet.Type type = packet.getType();
                switch (type) {
                    case GET_ROOMS:
                        this.handleGetRooms();
                        break;
                    case CREATE_ROOM:
                        this.handleCreateRoom(packet.getRoomName());
                        break;
                    default:
                        throw new Exception("Error: server received " + packet.getType() + " unexpected packet type");
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
        finally {
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

    private void handleCreateRoom(String name) {
        boolean answer = true;
        synchronized (rooms) {
            for (Room room : rooms) {
                if (room.getName().equals(name)) {
                    answer = false;
                    break;
                }
            }
            if(answer) { rooms.add(new Room(name)); }
        }
        try {
            outQueue.put(Packet.ackCreateRoom(answer));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        handleGetRooms();
    }

    private void handleGetRooms() {
        List<String> roomsNames= new ArrayList<String>();
        synchronized (rooms) {
            for (Room room : rooms) {
                roomsNames.add(room.getName());
            }
        }
//        roomsNames.add(String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000 + 1)));
        try {
            outQueue.put(Packet.createRoomsNames(roomsNames));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

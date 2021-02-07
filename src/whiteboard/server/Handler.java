package whiteboard.server;

import javafx.scene.text.Text;
import whiteboard.Packet;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Handler implements Runnable {

    private final Socket socket;
    private final BlockingQueue<Packet> outQueue = new LinkedBlockingQueue<Packet>();
    private final List<Room> rooms;
    private Thread OutputHandlerThread;
    private OutputHandler outputHandler;
    private Room currRoom;

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
                    case REQUEST_ADD_USER_TO_GUI:
                        this.handleRequestAddUserToGUI(packet.getUserName());
                        break;
                    case SEND_MSG:
                        this.handleSendMessage(packet.getMessageToSend());
                        break;
//                    case ADD_USER:
//                        this.addUser(packet.getRoomName());
                    case REMOVE_USER:
                        this.removeUser();
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

    private void handleRequestAddUserToGUI(String roomName) {
        for (Room room: rooms) {
            if (room.getName().equals(roomName)) {
                room.addUser(this);
                this.currRoom = room;
                /* add yourself to the GUI. */
                try { outQueue.put(Packet.addUserToGUI("CCC")); }
                catch (InterruptedException exception) { exception.printStackTrace(); }

                for(Handler handler: room.getUsers()) {
                    System.out.println("How many people in the room");
                    if(currRoom.getUsers().size() > 1 && handler != this) {
                        try {
                            /* add everyone for youself */
                            outQueue.put(Packet.addUserToGUI("DDD"));
                            /* add yourself for everyone else */
                            handler.outQueue.put(Packet.addUserToGUI("CCC"));
                        }
                        catch (InterruptedException exception) { exception.printStackTrace(); }
                    }
                }
            break;
            }
        }
    }

//    private void addUser(String roomName) {
//        synchronized (rooms) {
//            for (Room room: rooms) {
//                if (room.getName().equals(roomName)) {
//                    room.addUser(this);
//                    for(Handler handler: room.getUsers()) {
//                        System.out.println("How many people in the room");
//                        if(handler != this) {
//                            try { outQueue.put(Packet.addUserToGUI("CCC")); }
//                            catch (InterruptedException exception) { exception.printStackTrace(); } } } } } }
//    }

    private void removeUser() {
        synchronized (rooms) {
            for (Room room: rooms) {
                if (room.equals(currRoom)) {
                    room.removeUser(this);
                }
            }
            //TODO: maybe send a packet here to InputHandler to remove the user from the GUI.
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
            if(answer) {
                this.currRoom = new Room(name);
                rooms.add(currRoom);
            }
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
                System.out.println(room.getName());
            }
        }
//        roomsNames.add(String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000 + 1)));
        try {
            outQueue.put(Packet.createRoomsNames(roomsNames));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleSendMessage(String messageToSend) {
        try {
            for (Handler handler: this.currRoom.getUsers()) {
                if(handler != this) {
                    handler.outQueue.put(Packet.receiveMessage(messageToSend));
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

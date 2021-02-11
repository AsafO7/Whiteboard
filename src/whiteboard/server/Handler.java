package whiteboard.server;

import whiteboard.Packet;
import whiteboard.client.CompleteDraw;

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
    //private List<User> onlineUsers = new ArrayList<>();
    private Thread OutputHandlerThread;
    private OutputHandler outputHandler;
    private Room currRoom;
    private String username = "";

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
                    case ADD_ONLINE_USER:
                        String user = packet.getUsername();
                        this.handleAddOnlineUser(user);
                        break;
                    case GET_ROOMS:
                        this.handleGetRooms();
                        break;
                    case CREATE_ROOM:
                        this.handleCreateRoom(packet.getRoomName());
                        break;
                    case REQUEST_ADD_USER_TO_GUI:
                        this.handleRequestAddUserToGUI(packet.getUsername());
                        break;
                    case SEND_MSG:
                        this.handleSendMessage(packet.getMessageToSend());
                        break;
                    case REQUEST_REMOVE_USER_FROM_GUI:
                        String username = packet.getUsername();
                        this.handleRequestRemoveUser(username);
                        break;
                    case REQUEST_REMOVE_USER_FROM_ROOM:
                        this.handleRequestRemoveUserFromRoom();
                        break;
                    case SEND_NEW_DRAWING:
                        CompleteDraw drawing = packet.getDrawing();
                        this.handleAddNewDrawing(drawing);
                        break;
                    case REQUEST_CURRENT_DRAWINGS:
                        String roomName = packet.getRoomName();
                        this.handleRequestCurrDrawings(roomName);
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

    private void handleRequestCurrDrawings(String roomName) {
        for (Room room: rooms) {
            if(room.getName().equals(roomName)) {
                try {
                    outQueue.put(Packet.receiveCurrentDrawings(new ArrayList<>(room.getDrawings())));
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
                break;
            }
        }

//        try {
//            outQueue.put(Packet.receiveCurrentDrawings(currRoom.getDrawings()));
//        } catch (InterruptedException exception) {
//            exception.printStackTrace();
//        }
    }

    private void handleRequestRemoveUserFromRoom() {
        //this.currRoom.getUsers().remove(this);
        this.currRoom = null;
        try {
            outQueue.put(Packet.removeUserFromRoom());
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    private void handleAddOnlineUser(String user) {
        //onlineUsers.add(new User(this, user));
        this.username = user;
//        synchronized (rooms) {
//            for (Room room : rooms) {
//                synchronized (room.getUsers()) {
//                    for (Handler handler : room.getUsers()) {
//                        if (handler != this) {
//                            handler.onlineUsers.add(new User(handler, user));
//                        }
//                    }
//                }
//            }
//        }
        try {
            outQueue.put(Packet.setUsername(user));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

    }

    private void handleRequestAddUserToGUI(String roomName) {
        synchronized (rooms) {
            for (Room room : rooms) {
                if (room.getName().equals(roomName)) {
                    room.addUser(this);
                    this.currRoom = room;
                    /* add yourself to the GUI. */
                    try {
                        outQueue.put(Packet.addUserToGUI(username));
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                    synchronized (room.getUsers()) {
                        for (Handler handler : room.getUsers()) {
                            System.out.println("How many people in the room");
                            if (currRoom.getUsers().size() > 1 && handler != this) {
                                try {
                                    /* add everyone for youself */
                                    outQueue.put(Packet.addUserToGUI(handler.username));
                                    /* add yourself for everyone else */
                                    handler.outQueue.put(Packet.addUserToGUI(username));
                                } catch (InterruptedException exception) {
                                    exception.printStackTrace();
                                }
                            } } } break; } } }
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

    private void handleRequestRemoveUser(String user) {
        synchronized (currRoom.getUsers()) {
            currRoom.getUsers().remove(this);
            List<String> users = new ArrayList<>();
            for (Handler handler: currRoom.getUsers()) {
                users.add(handler.username);
            }
            for (Handler handler: currRoom.getUsers()) {
                try {
                    handler.outQueue.put(Packet.removeUserFromGUI(users));
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
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
        try {
            outQueue.put(Packet.createRoomsNames(roomsNames));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        //roomsNames.add(String.valueOf(ThreadLocalRandom.current().nextInt(1, 1000 + 1)));
//        for (Handler handler: this.currRoom.getUsers()) {
//            try {
//                handler.outQueue.put(Packet.createRoomsNames(roomsNames));
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
    }

    private void handleSendMessage(String messageToSend) {
        synchronized (currRoom.getUsers()) {
            for (Handler handler : this.currRoom.getUsers()) {
                if (handler != this) {
                    try {
                        handler.outQueue.put(Packet.receiveMessage(messageToSend));
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void handleAddNewDrawing(CompleteDraw drawing) {
        if(currRoom == null) { return; }
        synchronized (currRoom.getDrawings()) {

            // Add the new drawing to the stack on the server side
            currRoom.getDrawings().add(drawing);

            synchronized (currRoom.getUsers()) {
                for (Handler handler : this.currRoom.getUsers()) {
                    if (handler != this) {
                        handler.sendAllDrawings();
                    }
                }
            }
        }
    }

    //TODO: make a packet to send all drawings and receive all drawings
    //TODO: add the new drawing that was sent by Whiteboard before sending the drawing stack.
    private void sendAllDrawings() {
        try {
            this.outQueue.put(Packet.createAllDrawings(new ArrayList<>(currRoom.getDrawings())));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

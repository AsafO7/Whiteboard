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
    private Thread OutputHandlerThread;
    private OutputHandler outputHandler;
    private Room currRoom;
    private String username = null;

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
                Packet packet = null;
                try {
                    packet = (Packet) in.readObject();
                }
                catch (IOException e) {
                    if (currRoom != null) {
                        handleRequestExitRoom();
                    }
                    break;
                }
                Packet.Type type = packet.getType();
                switch (type) {
                    case SET_USERNAME:
                        String user = packet.getUsername();
                        this.handleSetUsername(user);
                        break;
                    case GET_ROOMS:
                        this.handleGetRooms();
                        break;
                    case CREATE_ROOM:
                        this.handleCreateRoom(packet.getRoomName());
                        break;
                    case CREATE_ROOM_WITH_DRAWINGS:
                        Packet.RoomNameAndDrawings roomNameAndDrawings = packet.getRoomNameAndDrawings();
                        String name = roomNameAndDrawings.roomName;
                        List<CompleteDraw> drawings = roomNameAndDrawings.drawings;
                        this.handleCreateRoomWithDrawings(name, drawings);
                        break;
                    case REQUEST_JOIN_ROOM:
                        String roomName = packet.getRoomName();
                        this.handleAddUserToRoom(roomName);
                        break;
                    case SEND_MSG:
                        this.handleSendMessage(packet.getMessageToSend());
                        break;
                    case REQUEST_EXIT_ROOM:
                        this.handleRequestExitRoom();
                        break;
                    case SEND_NEW_DRAWING:
                        CompleteDraw drawing = packet.getDrawing();
                        this.handleAddNewDrawing(drawing);
                        break;
                    case REQUEST_CURRENT_DRAWINGS:
                        this.handleRequestCurrDrawings();
                        break;
                    case REQUEST_USERS_LIST_GUI:
                        this.updateUsersListGUI(true);
                        break;
                    case REQUEST_UNDO:
                        this.undoDrawing();
                        break;
                    case REQUEST_REDO:
                        this.redoDrawing();
                        break;
                    case REQUEST_SET_HOST:
                        this.handleSetHost();
                        break;
                    case REQUEST_CLEAR_BOARD:
                        this.handleClearBoard();
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

    private void handleClearBoard() {
        currRoom.getDrawings().clear();
        synchronized (currRoom.getUsers()) {
            for(Handler handler: currRoom.getUsers()) {
                //if(handler != this) {
                    handler.handleRequestCurrDrawings();
                //}
            }
        }
    }

    private void handleSetHost() {
        synchronized (currRoom.getUsers()) {
            if (currRoom.getUsers().isEmpty()) {
                return;
            }
            try {
                for(Handler handler: currRoom.getUsers()) {
                    if(handler != this) {
                        handler.outQueue.put(Packet.setHost());
                        break;
                    }
                }
                //currRoom.getUsers().get(1).outQueue.put(Packet.setHost());
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }

    private void handleRequestExitRoom() {
        if (currRoom != null) {
            Room room = currRoom;
            removeUserFromRoom();
            synchronized (room.getUsers()) {
                //currRoom.getUsers().remove(this);
                List<String> users = new ArrayList<>();
                for (Handler handler : room.getUsers()) {
                    users.add(handler.username);
                }
                for (Handler handler : room.getUsers()) {
                    if (handler != this) {
                        try {
                            handler.outQueue.put(Packet.updateUsersListGUI(users));
                        } catch (InterruptedException exception) {
                        }
                    }
                }
            }
            if(room.getUsers().isEmpty()) {

                rooms.remove(room);
            }
        }
        else {
            //TODO: do some error handling if we call this function when we are not in a room
        }
    }

    private void handleRequestCurrDrawings() {
        try {
            outQueue.put(Packet.createAllDrawings(new ArrayList<>(currRoom.getDrawings())));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    private void removeUserFromRoom() {
        this.currRoom.getUsers().remove(this);
        this.currRoom = null;
    }

    private void handleSetUsername(String user) {
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
            outQueue.put(Packet.ackUsername(this.username));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }

    }

    private void handleAddUserToRoom(String roomName) {
        synchronized (rooms) {
            for (Room room : rooms) {
                if (room.getName().equals(roomName)) {
                    room.getUsers().add(this);
                    this.currRoom = room;
                    break; } } }
        if (currRoom != null) {
            updateUsersListGUI(false);
        }
        try {
            outQueue.put(Packet.ackJoinRoom(currRoom != null));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
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

    private void updateUsersListGUI(boolean updateSelf) {
        if (currRoom != null) {
            synchronized (currRoom.getUsers()) {
                //currRoom.getUsers().remove(this);
                List<String> users = new ArrayList<>();
                for (Handler handler : currRoom.getUsers()) {
                    users.add(handler.username);
                }
                for (Handler handler : currRoom.getUsers()) {
                    if (updateSelf || handler != this) {
                        try {
                            handler.outQueue.put(Packet.updateUsersListGUI(users));
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    private void handleCreateRoomWithDrawings(String name, List<CompleteDraw> drawings) {
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
                currRoom.setHost(this);
            }
        }
        if (answer) {
            handleAddUserToRoom(currRoom.getName());
            currRoom.getDrawings().addAll(drawings);
        }
        try {
            outQueue.put(Packet.ackCreateRoom(answer));
        } catch (InterruptedException e) {
            e.printStackTrace();
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
                currRoom.setHost(this);
            }
        }
        if (answer) {
            handleAddUserToRoom(currRoom.getName());
        }
        try {
            outQueue.put(Packet.ackCreateRoom(answer));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleGetRooms() {
        List<String> roomsNames = new ArrayList<String>();
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
        synchronized (currRoom.getDeletedDrawings()) {
            currRoom.getDeletedDrawings().clear();
        }
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

    private void sendAllDrawings() {
        try {
            this.outQueue.put(Packet.createAllDrawings(new ArrayList<>(currRoom.getDrawings())));
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void undoDrawing() {
        if (currRoom == null) { return; }
        CompleteDraw drawing;
        synchronized (currRoom.getDrawings()) {
            if (currRoom.getDrawings().size() == 0) { return; }
            drawing = currRoom.getDrawings().remove(currRoom.getDrawings().size() - 1);
        }
        synchronized (currRoom.getDeletedDrawings()) {
            currRoom.getDeletedDrawings().add(drawing);
        }
        synchronized (currRoom.getDrawings()) {
            synchronized (currRoom.getUsers()) {
                for (Handler handler : this.currRoom.getUsers()) {
                    handler.sendAllDrawings();
                }
            }
        }
    }

    private void redoDrawing() {
        if (currRoom == null) { return; }
        CompleteDraw drawing;
        synchronized (currRoom.getDeletedDrawings()) {
            if (currRoom.getDeletedDrawings().size() == 0) { return; }
            drawing = currRoom.getDeletedDrawings().remove(currRoom.getDeletedDrawings().size() - 1);
        }
        synchronized (currRoom.getDrawings()) {
            currRoom.getDrawings().add(drawing);

            synchronized (currRoom.getUsers()) {
                for (Handler handler : this.currRoom.getUsers()) {
                    handler.sendAllDrawings();
                }
            }
        }
    }

    public String getUsername() { return this.username; }
}

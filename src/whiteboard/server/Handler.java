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
    private List<Handler> allUsers;

    public Handler(Socket socket, List<Room> rooms, List<Handler> allUsers) {
        this.socket = socket;
        this.rooms = rooms;
        this.allUsers = allUsers;
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
                    case REQUEST_USERNAME:
                        String username = packet.getUsername();
                        this.handleRequestUsername(username);
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
//                    case REQUEST_SET_HOST:
//                        this.handleSetHost();
//                        break;
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
            synchronized (allUsers) {
                allUsers.remove(this);
            }
        }
    }

    private void handleRequestUsername(String username) {
        boolean ack = true;
        synchronized (allUsers) {
//            if (allUsers.isEmpty()) {
//                try {
//                    outQueue.put(Packet.ackUsername(true));
//                } catch (InterruptedException exception) {
//                    exception.printStackTrace();
//                }
//            }
            for (Handler handler : allUsers) {
                if (this != handler && handler.username != null && handler.username.equals(username)) {
                    ack = false;
                    break;
                }
            }
            // Add the user to allUsers
            if (ack) { this.username = username; }
        }
        try {
            outQueue.put(Packet.ackUsername(ack));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
    }

    private void handleClearBoard() {
        if (currRoom.getHost() != this) { return; }
        CompleteDraw drawing;
        synchronized (currRoom.getDrawings()) {
            if (currRoom.getDrawings().size() == 0) { return; }

            drawing = currRoom.getDrawings().remove(currRoom.getDrawings().size() - 1);
            synchronized (currRoom.getDeletedDrawings()) {
                currRoom.getDeletedDrawings().add(drawing);
                while (!currRoom.getDrawings().isEmpty()) {
                    drawing = currRoom.getDrawings().remove(currRoom.getDrawings().size() - 1);
                    currRoom.getDeletedDrawings().add(drawing);
                }
            }
        }

        synchronized (currRoom.getUsers()) {
            for(Handler handler: currRoom.getUsers()) {
                handler.handleRequestCurrDrawings();
            }
        }
    }

    private void handleRequestExitRoom() {
        if (currRoom != null) {
            Room room = currRoom;
            if (this == room.getHost()) {
                room.setHost(null);
            }
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
            else { // The room is not empty
                room.setHost(room.getUsers().get(0)); // Appoint the first user in the room as the new host
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
        if(currRoom != null) { return; }
        boolean answer = true;
        if (username == null) { answer = false; }
        else {
            synchronized (rooms) {
                for (Room room : rooms) {
                    if (room.getName().equals(name)) {
                        answer = false;
                        break;
                    }
                }
                if (answer) {
                    this.currRoom = new Room(name);
                    rooms.add(currRoom);
                    currRoom.setHost(this);
                }
            }
            if (answer) {
                handleAddUserToRoom(currRoom.getName());
                currRoom.getDrawings().addAll(drawings);
            }
        }
        try {
            outQueue.put(Packet.ackCreateRoom(answer));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleCreateRoom(String name) {
        if(currRoom != null) { return; }
        boolean answer = true;
        if (username == null) { answer = false; }
        else {
            synchronized (rooms) {
                for (Room room : rooms) {
                    if (room.getName().equals(name)) {
                        answer = false;
                        break;
                    }
                }
                if (answer) {
                    this.currRoom = new Room(name);
                    rooms.add(currRoom);
                    currRoom.setHost(this);
                }
            }
            if (answer) {
                handleAddUserToRoom(currRoom.getName());
            }
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

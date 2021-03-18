package whiteboard.server;

import whiteboard.CreateRMIRegistry;
import whiteboard.OutputHandler;
import whiteboard.Packet;
import whiteboard.client.CompleteDraw;

import java.io.*;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Handler implements IServerHandler {

    private final OutputHandler outQueue = new OutputHandler();
    private final Thread threadOutQueue;
    private final List<Room> rooms;
    private Room currRoom;
    private String username = null;
    private final List<Handler> allUsers;

    public Handler(List<Room> rooms, List<Handler> allUsers) {
        this.rooms = rooms;
        this.allUsers = allUsers;
        threadOutQueue = new Thread(outQueue);
        threadOutQueue.start();
    }

    // The client connects to the server with socket, then he connects to the RMI, and then he calls this method,
    //  so that the server also connects to the client's RMI server.
    public void setIClientHandler(IClientHandler stub) {
        CreateRMIRegistry createRMIRegistry = new CreateRMIRegistry();

        Registry registry = LocateRegistry.getRegistry(host, 2000);
        IServerHandler stub = (IServerHandler) registry.lookup("IServerHandler");
        String response = stub.sayHello();
        System.out.println("response: " + response);
    }

    private void handleChangeUsername(String userName) {
        this.username = userName;
        updateUsersListGUI(true);
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

    // When requested by the client, should be called with 'true'
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
                            handler.outQueue.put(() -> stub.handleUpdateUsersList(users));
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
        //TODO: Uncomment
//        try {
//            outQueue.put(stub.handleRoomsList(roomsNames));
//        } catch (InterruptedException exception) {
//            exception.printStackTrace();
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

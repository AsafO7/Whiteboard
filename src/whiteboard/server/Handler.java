package whiteboard.server;

import javafx.application.Platform;
import whiteboard.Packet;
import whiteboard.client.CompleteDraw;
import whiteboard.client.IClientHandler;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

public class Handler implements IServerHandler {

    private final OutputHandler outQueue = new OutputHandler();
    private final Thread threadOutQueue;
    private final List<Room> rooms;
    private Room currRoom;
    private String username = null;
    private final List<Handler> allUsers;
    private IClientHandler stub;

    public Handler(List<Room> rooms, List<Handler> allUsers) {
        this.rooms = rooms;
        this.allUsers = allUsers;
        threadOutQueue = new Thread(outQueue);
        threadOutQueue.start();
    }

    public void setClientStub(IClientHandler stub) {
        assert stub != null;
        this.stub = stub;
    }

    public void handleChangeUsername(String userName) {
        Platform.runLater(() -> {
            this.username = userName;
            updateUsersListGUI(true);
        });

    }

    public void handleRequestUsername(String username) {
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
        boolean finalAck = ack;
        outQueue.put(() -> {
            try {
                this.stub.handleAckUsername(finalAck);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void handleClearBoard() {
        Platform.runLater(() -> {
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
                for (Handler handler : currRoom.getUsers()) {
                    handler.handleRequestCurrDrawings();
                }
            }
        });
    }

    public void handleRequestExitRoom() {
        Platform.runLater(() -> {
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
                            handler.outQueue.put(() -> {
                                try {
                                    this.stub.updateUsersListGUI(users);
                                } catch (RemoteException e) {
                                    e.printStackTrace();
                                }
                            });
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
        });
    }

    public void handleRequestCurrDrawings() {
        Platform.runLater(() -> {
            List<CompleteDraw> drawings = new ArrayList<>(currRoom.getDrawings());
            outQueue.put(() -> {
                try {
                    this.stub.updateDrawStack(drawings);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void removeUserFromRoom() {
        this.currRoom.getUsers().remove(this);
        this.currRoom = null;
    }

    public void handleAddUserToRoom(String roomName) {
        Platform.runLater(() -> {
            synchronized (rooms) {
                for (Room room : rooms) {
                    if (room.getName().equals(roomName)) {
                        room.getUsers().add(this);
                        this.currRoom = room;
                        break; } } }
            if (currRoom != null) {
                updateUsersListGUI(false);
            }
            boolean ack = currRoom != null;
            outQueue.put(() -> {
                try {
                    this.stub.handleAckJoinRoom(ack);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        });
    }

    public void handleUpdateUsersListGUI() {
        Platform.runLater(() -> {
            updateUsersListGUI(true);
        });

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
                        handler.outQueue.put(() -> {
                            try {
                                handler.stub.updateUsersListGUI(users);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        }
    }

    public void handleCreateRoomWithDrawings(String name, List<CompleteDraw> drawings) {
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
        boolean finalAnswer = answer;
        outQueue.put(() -> {
            try {
                this.stub.handleAckCreateRoom(finalAnswer);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void handleCreateRoom(String name) {
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
        boolean finalAnswer = answer;
        outQueue.put(() -> {
            try {
                this.stub.handleAckCreateRoom(finalAnswer);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void handleGetRooms() {
        List<String> roomsNames = new ArrayList<String>();
        synchronized (rooms) {
            for (Room room : rooms) {
                roomsNames.add(room.getName());
                System.out.println(room.getName());
            }
        }
        outQueue.put(() -> {
            try {
                this.stub.handleRoomsList(roomsNames);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void handleSendMessage(String messageToSend) {
        Platform.runLater(() -> {
            synchronized (currRoom.getUsers()) {
                for (Handler handler : this.currRoom.getUsers()) {
                    if (handler != this) {
                        handler.outQueue.put(() -> {
                            try {
                                this.stub.handleReceivedMessage(messageToSend); /* should we replace this with handler? */
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                }
            }
        });
    }

    public void handleAddNewDrawing(CompleteDraw drawing) {
        Platform.runLater(() -> {
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
        });
    }

    private void sendAllDrawings() {
        List<CompleteDraw> drawings = new ArrayList<>(currRoom.getDrawings());
        outQueue.put(() -> {
            try {
                this.stub.updateDrawStack(drawings);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void undoDrawing() {
        Platform.runLater(() -> {
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
        });
    }

    public void redoDrawing() {
        Platform.runLater(() -> {
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
        });
    }

//    public String getUsername() { return this.username; }
}

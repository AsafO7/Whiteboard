package whiteboard;

import whiteboard.client.CompleteDraw;

import java.io.Serializable;
import java.util.List;

public class Packet implements Serializable {
    private static final long serialVersionUID = 9108779363102530646L;

    private Type type;
    private Object obj;

    public enum Type { // List of events
        SET_USERNAME,
        ACK_SET_USERNAME,
        GET_ROOMS,
        ROOMS_NAMES,
        CREATE_ROOM,
        CREATE_ROOM_WITH_DRAWINGS,
        ACK_CREATE_ROOM,
        SEND_MSG,
        RECEIVE_MSG,
        UPDATE_USERS_LIST,
        SEND_NEW_DRAWING,
        REQUEST_EXIT_ROOM,
        NEW_DRAWING,
        REQUEST_CURRENT_DRAWINGS,
        REQUEST_USERS_LIST_GUI,
        RECEIVE_CURRENT_DRAWINGS,
        REQUEST_JOIN_ROOM,
        ACK_JOIN_ROOM,
        REQUEST_UNDO,
        REQUEST_REDO,
        REQUEST_CLEAR_BOARD,
        CLEAR_BOARD,
        REQUEST_USERNAME,
        ACK_USERNAME,
    }

    public Packet(Object obj, Type type) {
        this.obj = obj;
        this.type = type;
    }

    /************************************* Data types *************************************/

    public static class RoomNameAndDrawings implements Serializable{
        public String roomName;
        public List<CompleteDraw> drawings;

        public RoomNameAndDrawings(String roomName, List<CompleteDraw> drawings) {
            this.roomName = roomName;
            this.drawings = drawings;
        }
    }

    /************************************* Requests *************************************/

    public static Packet createRequestUndo() { return new Packet(null, Type.REQUEST_UNDO); }

    public static Packet createRequestRedo() { return new Packet(null, Type.REQUEST_REDO); }

    public static Packet setUsername(String userName) { return new Packet(userName, Type.SET_USERNAME); }

    public static Packet ackUsername(String userName) { return new Packet(userName, Type.ACK_SET_USERNAME); }

    public static Packet requestUsername(String userName) { return new Packet(userName, Type.REQUEST_USERNAME); }

    public static Packet ackUsername(boolean answer) { return new Packet(answer, Type.ACK_USERNAME); }

    // Creates a list of room names request.
    public static Packet createRoomsNames(List<String> roomsNames) {
        return new Packet(roomsNames, Type.ROOMS_NAMES);
    }

    // Handles displaying the rooms to the user request.
    public static Packet requestRoomsNames() {
        return new Packet(null, Type.GET_ROOMS);
    }

    // Creates a room request.
    public static Packet createRoom(String name) {
        return new Packet(name, Type.CREATE_ROOM);
    }

    // Creates a room request with loaded drawings.
    public static Packet createRoomWithDrawings(String name, List<CompleteDraw> drawings) {
        return new Packet(new RoomNameAndDrawings(name, drawings), Type.CREATE_ROOM_WITH_DRAWINGS);
    }

    // Confirms the room is created request.
    public static Packet ackCreateRoom(boolean answer) { return new Packet(answer, Type.ACK_CREATE_ROOM); }

    public static Packet ackJoinRoom(boolean answer) { return new Packet(answer, Type.ACK_JOIN_ROOM); }

    public static Packet sendMessage(String msg) { return new Packet(msg, Type.SEND_MSG); }

    public static Packet receiveMessage(String msg) {
        return new Packet(msg, Type.RECEIVE_MSG);
    }

    public static Packet requestJoinRoom(String room) { return new Packet(room, Type.REQUEST_JOIN_ROOM); }

    public static Packet updateUsersListGUI(List<String> users) { return new Packet(users, Type.UPDATE_USERS_LIST); }

    public static Packet requestExitRoom() { return new Packet(null, Type.REQUEST_EXIT_ROOM); }

    public static Packet sendNewDrawing(CompleteDraw drawing) { return new Packet(drawing, Type.SEND_NEW_DRAWING); }

    public static Packet createAllDrawings(List<CompleteDraw> drawings) { return new Packet(drawings, Type.NEW_DRAWING); }

    public static Packet requestCurrentDrawings() { return new Packet(null, Type.REQUEST_CURRENT_DRAWINGS); }

    public static Packet requestUsersListGUI() { return new Packet(null, Type.REQUEST_USERS_LIST_GUI); }

    public static Packet requestClearBoard() { return new Packet(null, Type.REQUEST_CLEAR_BOARD); }

    //public static Packet receiveCurrentDrawings(List<CompleteDraw> drawings) { return new Packet(drawings, Type.RECEIVE_CURRENT_DRAWINGS); }

    /******************************** Type errors handling ********************************/

    public String getUsername() throws TypeError {
        if (type != Type.SET_USERNAME && type != Type.ACK_SET_USERNAME && type != Type.REQUEST_USERNAME) {
            throw new TypeError(Type.SET_USERNAME, type);
        }
        return (String) obj;
    }

    public boolean getAckUsername() throws TypeError {
        if(type != Type.ACK_USERNAME) {
            throw new TypeError(Type.ACK_USERNAME, type);
        }
        return (boolean) obj;
    }

    public List<String> getRoomsNames() throws TypeError {
        if (type != Type.ROOMS_NAMES) {
            throw new TypeError(Type.ROOMS_NAMES, type);
        }
        return (List<String>)obj;
    }

    public String getRoomName() throws TypeError{
        if(type != Type.CREATE_ROOM && type != Type.REQUEST_JOIN_ROOM) {
            throw new TypeError(Type.CREATE_ROOM, type);
        }
        return (String)obj;
    }

    public List<String> getRoomUsers() throws TypeError {
        if(type != Type.UPDATE_USERS_LIST) {
            throw new TypeError(Type.UPDATE_USERS_LIST, type);
        }
        return (List<String>) obj;
    }

    public CompleteDraw getDrawing() throws TypeError {
        if(type != Type.SEND_NEW_DRAWING) {
            throw new TypeError(Type.SEND_NEW_DRAWING, type);
        }
        return (CompleteDraw) obj;
    }

    public List<CompleteDraw> receiveAllDrawings() throws TypeError {
        if(type != Type.NEW_DRAWING && type != Type.RECEIVE_CURRENT_DRAWINGS) {
            throw new TypeError(Type.NEW_DRAWING, type);
        }
        return (List<CompleteDraw>) obj;
    }

    public RoomNameAndDrawings getRoomNameAndDrawings() throws TypeError {
        if(type != Type.CREATE_ROOM_WITH_DRAWINGS) {
            throw new TypeError(Type.CREATE_ROOM_WITH_DRAWINGS, type);
        }
        return (RoomNameAndDrawings) obj;
    }

//    public String getUserToRemove() throws TypeError {
//        if(type != Type.REQUEST_REMOVE_USER_FROM_GUI) {
//            throw new TypeError(Type.REQUEST_REMOVE_USER_FROM_GUI, type);
//        }
//        return (String) obj;
//    }

    public boolean getAckCreateRoom() throws TypeError {
        if(type != Type.ACK_CREATE_ROOM) {
            throw new TypeError(Type.ACK_CREATE_ROOM, type);
        }
        return (boolean)obj;
    }

    public boolean getAckJoinRoom() throws TypeError {
        if(type != Type.ACK_JOIN_ROOM) {
            throw new TypeError(Type.ACK_JOIN_ROOM, type);
        }
        return (boolean)obj;
    }

    public String getMessageToSend() throws TypeError{
        if(type != Type.SEND_MSG) {
            throw new TypeError(Type.SEND_MSG, type);
        }
        return (String) obj;
    }

    public String getMessageToReceive() throws TypeError{
        if(type != Type.RECEIVE_MSG) {
            throw new TypeError(Type.RECEIVE_MSG, type);
        }
        return (String) obj;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}

class TypeError extends Exception {
    public TypeError(Packet.Type expected, Packet.Type got) {
        super("Error: expected object of type " + expected.name() + ", received object of type " + got.name());
    }
}


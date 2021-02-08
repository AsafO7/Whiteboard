package whiteboard;

import whiteboard.client.MyDraw;

import java.io.Serializable;
import java.util.List;

public class Packet implements Serializable {
    private static final long serialVersionUID = 9108779363102530646L;

    private Type type;
    private Object obj;

    public enum Type { // List of events
        ADD_ONLINE_USER,
        SET_USERNAME,
        GET_ROOMS,
        ROOMS_NAMES,
        CREATE_ROOM,
        ACK_CREATE_ROOM,
        SEND_MSG,
        RECEIVE_MSG,
        REQUEST_ADD_USER_TO_GUI,
        ADD_USER_TO_GUI,
        REQUEST_REMOVE_USER_FROM_GUI,
        REMOVE_USER_FROM_GUI,
        REQUEST_CREATE_ALL_DRAWINGS,
        CREATE_DRAWING,
    }

    public Packet(Object obj, Type type) {
        this.obj = obj;
        this.type = type;
    }

    /************************************* Requests *************************************/

    public static Packet addOnlineUser(String userName) { return new Packet(userName, Type.ADD_ONLINE_USER); }

    public static Packet setUsername(String userName) { return new Packet(userName, Type.SET_USERNAME); }

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

    // Confirms the room is created request.
    public static Packet ackCreateRoom(boolean answer) { return new Packet(answer, Type.ACK_CREATE_ROOM); }

    public static Packet sendMessage(String msg) { return new Packet(msg, Type.SEND_MSG); }

    public static Packet receiveMessage(String msg) {
        return new Packet(msg, Type.RECEIVE_MSG);
    }

    public static Packet requestAddUserToGUI(String user) { return new Packet(user, Type.REQUEST_ADD_USER_TO_GUI); }

    public static Packet addUserToGUI(String user) { return new Packet(user, Type.ADD_USER_TO_GUI); }

    public static Packet requestRemoveUserFromGUI(String user) { return new Packet(user, Type.REQUEST_REMOVE_USER_FROM_GUI); }

    public static Packet removeUserFromGUI(List<String> users) { return new Packet(users, Type.REMOVE_USER_FROM_GUI); }

    public static Packet requestCreateAllDrawing(MyDraw drawing) { return new Packet(drawing, Type.REQUEST_CREATE_ALL_DRAWINGS); }

    public static Packet createAllDrawings(List<MyDraw> drawing) { return new Packet(drawing, Type.CREATE_DRAWING); }

    /******************************** Type errors handling ********************************/

    public List<String> getRoomsNames() throws TypeError {
        if (type != Type.ROOMS_NAMES) {
            throw new TypeError(Type.ROOMS_NAMES, type);
        }
        return (List<String>)obj;
    }

    public String getRoomName() throws TypeError{
        if(type != Type.CREATE_ROOM) {
            throw new TypeError(Type.CREATE_ROOM, type);
        }
        return (String)obj;
    }

    public String getUsername() throws TypeError {
        if(type != Type.ADD_ONLINE_USER && type != Type.SET_USERNAME && type != Type.ADD_USER_TO_GUI && type != Type.REQUEST_ADD_USER_TO_GUI
        && type != Type.REQUEST_REMOVE_USER_FROM_GUI) {
            throw new TypeError(Type.ADD_USER_TO_GUI, type);
        }
        return (String) obj;
    }

    public List<String> getRoomUsers() throws TypeError {
        if(type != Type.REMOVE_USER_FROM_GUI) {
            throw new TypeError(Type.REMOVE_USER_FROM_GUI, type);
        }
        return (List<String>) obj;
    }

    public MyDraw getDrawing() throws TypeError {
        if(type != Type.REQUEST_CREATE_ALL_DRAWINGS) {
            throw new TypeError(Type.REQUEST_CREATE_ALL_DRAWINGS, type);
        }
        return (MyDraw) obj;
    }

    public List<MyDraw> sendAllDrawings() throws TypeError {
        if(type != Type.CREATE_DRAWING) {
            throw new TypeError(Type.CREATE_DRAWING, type);
        }
        return (List<MyDraw>) obj;
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


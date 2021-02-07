package whiteboard;

import javafx.scene.Scene;
import javafx.scene.text.Text;

import java.io.Serializable;
import java.util.List;

public class Packet implements Serializable {
    private static final long serialVersionUID = 9108779363102530646L;

    private Type type;
    private Object obj;

    public enum Type { // List of events
        GET_ROOMS,
        ROOMS_NAMES,
        CREATE_ROOM,
        ACK_CREATE_ROOM,
        SEND_MSG,
        RECEIVE_MSG,
        ADD_USER,
        REQUEST_ADD_USER_TO_GUI,
        ADD_USER_TO_GUI,
        REMOVE_USER,
    }

    public Packet(Object obj, Type type) {
        this.obj = obj;
        this.type = type;
    }

    /************************************* Requests *************************************/

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

    public static Packet addUser(String roomName) { return new Packet(roomName, Type.ADD_USER); }

    public static Packet requestAddUserToGUI(String user) { return new Packet(user, Type.REQUEST_ADD_USER_TO_GUI); }

    public static Packet addUserToGUI(String user) { return new Packet(user, Type.ADD_USER_TO_GUI); }

    public static Packet removeUser(/*String roomName*/) { return new Packet(/*roomName*/null, Type.REMOVE_USER); }

    /******************************** Type errors handling ********************************/

    public List<String> getRoomsNames() throws TypeError {
        if (type != Type.ROOMS_NAMES) {
            throw new TypeError(Type.ROOMS_NAMES, type);
        }
        return (List<String>)obj;
    }

    public String getRoomName() throws TypeError{
        if(type != Type.CREATE_ROOM && type != Type.ADD_USER /*&& type != Type.REMOVE_USER*/) {
            throw new TypeError(Type.CREATE_ROOM, type);
        }
        return (String)obj;
    }

    public String getUserName() throws TypeError {
        if(type != Type.ADD_USER_TO_GUI && type != Type.REQUEST_ADD_USER_TO_GUI) {
            throw new TypeError(Type.ADD_USER_TO_GUI, type);
        }
        return (String) obj;
    }

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


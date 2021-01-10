package whiteboard;

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
    }

    public Packet(Object obj, Type type) {
        this.obj = obj;
        this.type = type;
    }

    public static Packet createRoomsNames(List<String> roomsNames) {
        return new Packet(roomsNames, Type.ROOMS_NAMES);
    }

    public static Packet requestRoomsNames() {
        return new Packet(null, Type.GET_ROOMS);
    }

    public static Packet createRoom(String name) {
        return new Packet(name, Type.CREATE_ROOM);
    }

    public static Packet ackCreateRoom(boolean answer) {
        return new Packet(answer, Type.ACK_CREATE_ROOM);
    }

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

    public boolean getAckCreateRoom() throws TypeError {
        if(type != Type.ACK_CREATE_ROOM) {
            throw new TypeError(Type.ACK_CREATE_ROOM, type);
        }
        return (boolean)obj;
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


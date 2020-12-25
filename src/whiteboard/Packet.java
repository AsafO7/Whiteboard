package whiteboard;

import java.io.Serializable;
import java.util.List;

public class Packet implements Serializable {
    private Type type;
    private Object obj;

    public enum Type {
        GET_ROOMS,
        ROOMS_NAMES,
    }

    public Packet(Object obj, Type type) {
        this.obj = obj;
        this.type = type;
    }

    public static Packet createRoomsNames(List<String> roomsNames) {
        return new Packet(roomsNames, Type.ROOMS_NAMES);
    }

    public List<String> getRoomsNames() throws TypeError {
        if (type != Type.ROOMS_NAMES) {
            throw new TypeError(Type.ROOMS_NAMES, type);
        }
        return (List<String>)obj;
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


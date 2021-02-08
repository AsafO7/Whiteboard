package whiteboard.server;

import whiteboard.client.MyDraw;
import whiteboard.client.WhiteboardRoom;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room implements Serializable {
    private String name;
    private List<Handler> users = Collections.synchronizedList(new ArrayList<>());
    private List<MyDraw> drawings = Collections.synchronizedList(new ArrayList<>());

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addUser(Handler name) { users.add(name); }

    public void removeUser(Handler name) { users.remove(name); }

    public List<Handler> getUsers() { return users; }

    public List<MyDraw> getDrawings() { return drawings; }


}

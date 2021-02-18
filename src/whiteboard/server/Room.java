package whiteboard.server;

import whiteboard.client.CompleteDraw;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Room implements Serializable {
    private final String name;
    private Handler host;
    private final List<Handler> users = Collections.synchronizedList(new ArrayList<>());
    private final List<CompleteDraw> drawings = Collections.synchronizedList(new ArrayList<>());
    private final List<CompleteDraw> deletedDrawings = Collections.synchronizedList(new ArrayList<>());

    public Room(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<Handler> getUsers() { return users; }

    public List<CompleteDraw> getDrawings() { return drawings; }

    public List<CompleteDraw> getDeletedDrawings() { return deletedDrawings; }

    public void setHost(Handler host) { this.host = host; }

    public Handler getHost() { return host; }
}

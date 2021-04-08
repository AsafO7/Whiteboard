package whiteboard;

import whiteboard.client.CompleteDraw;

import java.io.Serializable;
import java.util.List;

public class RoomNameAndDrawings implements Serializable {
    private static final long serialVersionUID = 9108779363102530646L;

    public String roomName;
    public List<CompleteDraw> drawings;

    public RoomNameAndDrawings(String roomName, List<CompleteDraw> drawings) {
        this.roomName = roomName;
        this.drawings = drawings;
    }
}


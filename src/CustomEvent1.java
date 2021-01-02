import javafx.event.EventType;
import whiteboard.server.Room;

import java.util.ArrayList;
import java.util.List;

public class CustomEvent1 extends CustomEvent {

    public static final EventType<CustomEvent> CUSTOM_EVENT_TYPE_1 = new EventType(CUSTOM_EVENT_TYPE, "CustomEvent1");

    private List<Room> rooms;

    public CustomEvent1(ArrayList<Room> list) {
        super(CUSTOM_EVENT_TYPE_1);
        rooms = list;
    }

    @Override
    public void invokeHandler(MyCustomEventHandler handler) {
        handler.onEvent1(rooms);
    }

}
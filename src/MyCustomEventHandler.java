import javafx.event.EventHandler;
import whiteboard.server.Room;

import java.util.List;

public abstract class MyCustomEventHandler implements EventHandler<CustomEvent> {

    public abstract void onEvent1(List<Room> rooms); // Event1

    //public abstract void onEvent2(String param0); // Event2

    @Override
    public void handle(CustomEvent event) {
        event.invokeHandler(this);
    }
}
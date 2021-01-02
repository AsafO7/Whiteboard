import javafx.event.Event;
import javafx.event.EventType;

public abstract class CustomEvent extends Event {

    public static final EventType<CustomEvent> CUSTOM_EVENT_TYPE = new EventType(ANY);

    public CustomEvent(EventType<? extends Event> eventType) {
        super(eventType);
    }

    public abstract void invokeHandler(MyCustomEventHandler handler);

}

/* EXAMPLE

Button btn = new Button("Say 'Hello World'");
btn.setOnAction((ActionEvent event) -> {
    btn.fireEvent(new CustomEvent1(42));
    btn.fireEvent(new CustomEvent2("Hello World"));
});

btn.addEventHandler(CustomEvent.CUSTOM_EVENT_TYPE, new MyCustomEventHandler() {

    @Override
    public void onEvent1(int param0) {
        System.out.println("integer parameter: " + param0);
    }

});

*/
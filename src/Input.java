import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import whiteboard.Packet;
import whiteboard.client.CssLayouts;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Input implements Runnable{

    private final VBox lobby;

    public Input(VBox currentLobby) {
        lobby = currentLobby;
    }
    @Override
    public void run() {
        /* Receive input from the server here. */
        try {
            Socket socket = new Socket("localhost", 5555);
            ObjectOutputStream out = null;
            ObjectInputStream in = null;
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            Packet packet = (Packet) in.readObject();

            switch(packet.getType()) {
                case ROOMS_NAMES:

                    List<String> roomsNames = packet.getRoomsNames();
                    Platform.runLater(() -> this.handleRoomsList(roomsNames));
                    break;
            }
        }
        catch (Exception e) { e.printStackTrace(); }


    }

    /* This method will update the UI with the lobby rooms. */
    private void handleRoomsList(List<String> roomsNames) {

        List<Text> roomsContainers = new ArrayList<>();
        lobby.getChildren().clear();
        /* Organizing the rooms in the layout. */
        for(int i = 0; i < roomsNames.size(); i++) {
            roomsContainers.add(new Text(roomsNames.get(i)));
            roomsContainers.get(i).setStyle(CssLayouts.cssBorder);
            lobby.getChildren().add(roomsContainers.get(i));
        }
    }
}

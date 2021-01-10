package whiteboard.client;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import whiteboard.Connection;
import whiteboard.Packet;
import whiteboard.client.CssLayouts;
import whiteboard.server.Room;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class InputHandler implements Runnable {

    private final Socket socket;
    private final VBox lobby;

    public InputHandler(Socket socket, VBox currentLobby) {
        this.socket = socket;
        this.lobby = currentLobby;
    }
    @Override
    public void run() {
        /* Receive input from the server here. */
        ObjectInputStream in = null;

        try {
            in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                Packet packet = (Packet) in.readObject();

                switch (packet.getType()) {
                    case ROOMS_NAMES:
                        List<String> roomsNames = packet.getRoomsNames();
                        Platform.runLater(() -> this.handleRoomsList(roomsNames));
                        break;
                    case ACK_CREATE_ROOM:
                        //TODO: if ok, change scene to whiteboard. else pop an alert dialog.
                        System.out.println(packet.getAckCreateRoom());
                        break;
                }
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* This method will update the UI with the lobby rooms. */
    private void handleRoomsList(List<String> roomsNames) {

        lobby.getChildren().clear();
        /* Organizing the rooms in the layout. */
        for(String roomName : roomsNames) {
            Text roomText = new Text(roomName);
            roomText.setStyle(CssLayouts.cssBorder);
            lobby.getChildren().add(roomText);
        }
    }
}

/* This class takes care of inputs the user receives from the server */

package whiteboard.client;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import whiteboard.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

public class InputHandler implements Runnable {

    private static final int CHAT_MESSAGE_WRAPPING_WIDTH = 230;
    private final Stage stage;
    private Scene scene;
    private final Socket socket;
    private final VBox lobby;
    private BlockingQueue<Packet> outQueue;
    private Text user = new Text("AAA");
    private WhiteboardRoom myRoom = null;
    private String roomName = "";

    private List<WhiteboardRoom> drawingRooms = new ArrayList<>();

    private final String SAME_ROOM_NAME_TITLE = "Room already exists",
            SAME_ROOM_NAME_MSG = "There's a room with that name already, please choose a different name.";

    public InputHandler(Socket socket, VBox currentLobby, Stage stage, Scene scene, BlockingQueue<Packet> outQueue) {
        this.socket = socket;
        this.lobby = currentLobby; // the GUI container of the rooms.
        this.stage = stage;
        this.scene = scene; // lobby scene.
        this.outQueue = outQueue;
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
                    case SET_USERNAME:
                        String username = packet.getUsername();
                        Platform.runLater(() -> handlerSetUser(username));
                        break;
                    case ROOMS_NAMES:
                        List<String> roomsNames = packet.getRoomsNames();
                        Platform.runLater(() -> this.handleRoomsList(roomsNames));
                        break;
                    case ACK_CREATE_ROOM:
                        if(packet.getAckCreateRoom()) {
                            Platform.runLater(this::handleNewRoomTransfer);
                        }
                        else {
                            //TODO: find a way to display alert for room of the same name
                        }
                        break;
                    case  RECEIVE_MSG:
                        String msg = packet.getMessageToReceive();
                        Platform.runLater(() -> this.handleReceivedMessage(msg));
                        break;
                    case ADD_USER_TO_GUI:
                        String user = packet.getUsername();
                        Platform.runLater(() -> addUserToGUI(user));
                        break;
                    case REMOVE_USER_FROM_GUI:
                        List<String> users = packet.getRoomUsers();
                        Platform.runLater(() -> removeUserFromGUI(users));
                        break;
                    case CREATE_DRAWING:
                        List<MyDraw> drawings = packet.sendAllDrawings();
                        Platform.runLater(() -> this.receiveAllDrawings(drawings));
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

    private void handlerSetUser(String username) {
        this.user.setText(username);
    }

    private void removeUserFromGUI(List<String> users) {
        myRoom.getOnlineUsersPanel().getChildren().clear();
        for (String user: users) {
            myRoom.getOnlineUsersPanel().getChildren().add(new Text(user));
        }
    }

    private void addUserToGUI(String user) {
        myRoom.getOnlineUsersPanel().getChildren().add(new Text(user));
        myRoom.getOnlineUsers().add(new Text(user));
    }

    private void handleNewRoomTransfer() {
        String roomName = this.roomName;
        myRoom = new WhiteboardRoom(this.user.getText());
        try {
            outQueue.put(Packet.requestAddUserToGUI(roomName));
        } catch (InterruptedException exception) {
            exception.printStackTrace();
        }
        stage.setScene(myRoom.showBoard(stage,user,scene, outQueue));
    }

    //TODO: different users don't have the same rooms list.

    /* This method will update the UI with the lobby rooms. */
    private void handleRoomsList(List<String> roomsNames) {
        //if(drawingRooms.isEmpty()) { return; }
        lobby.getChildren().clear();
        VBox[] containers = new VBox[roomsNames.size()];
        /* Organizing the rooms in the layout. */
        for(int i = 0; i < roomsNames.size(); i++) {
            containers[i] = new VBox();
            containers[i].setAlignment(Pos.CENTER);
            containers[i].getChildren().add(new Label(roomsNames.get(i)));
            lobby.getChildren().add(containers[i]);
            lobby.getChildren().get(i).setStyle(CssLayouts.cssLobbiesStyle);

            addEventListener(lobby, i, roomsNames);
        }
    }

    //TODO: in this function the user will receive all the drawings currently in the room.
    private void addEventListener(VBox lobby, int i, List<String> roomsNames) {
        lobby.getChildren().get(i).setOnMouseClicked(e -> {
            myRoom = new WhiteboardRoom(this.user.getText());
            try {
                outQueue.put(Packet.requestAddUserToGUI(roomsNames.get(i)));
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
            stage.setScene(myRoom.showBoard(stage, user, scene, outQueue));
//            stage.setScene(drawingRooms.get(i).showBoard(stage,user,scene, outQueue));
//            drawingRooms.get(i).repaint();
        });
        lobby.getChildren().get(i).setOnMouseEntered(e -> {
            lobby.setCursor(Cursor.HAND);
        });
        lobby.getChildren().get(i).setOnMouseExited(e -> {
            lobby.setCursor(Cursor.DEFAULT);
        });
    }

    private void handleReceivedMessage(String msg) {
        Text msgToReceive = new Text(user.getText() + ": " + msg);
        msgToReceive.setStyle(CssLayouts.cssChatText);
        msgToReceive.setWrappingWidth(CHAT_MESSAGE_WRAPPING_WIDTH);
        //System.out.println(currRoom);
        myRoom.getChatBox().getChildren().add(msgToReceive);
    }

    public void setRoomName(String name) {
        this.roomName = name;
    }

    public void receiveAllDrawings(List<MyDraw> drawings) {
        if(myRoom == null) { return; }
        myRoom.myDraws.clear();
        myRoom.myDraws.addAll(drawings);
    }
}

/* This class takes care of inputs the user receives from the server */

package whiteboard.client;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import whiteboard.Packet;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;

//TODO: for EOF error check if you put break; in the switch.

public class InputHandler implements Runnable {

    private static final int CHAT_MESSAGE_WRAPPING_WIDTH = 230;
    private final Board board;
    private final Stage stage;
    private Scene scene;
    private final Socket socket;
    private final VBox lobby;
    private BlockingQueue<Packet> outQueue;
    //private Text user = new Text("AAA");
    public WhiteboardRoom myRoom = null;
    public String roomName = null;
    private List<String> roomsNames = new ArrayList<>();
//    public Stage signIn = null;
    public String username = null;

    private final String SAME_ROOM_NAME_TITLE = "Room already exists",
            SAME_ROOM_NAME_MSG = "There's a room with that name already, please choose a different name.";

    public InputHandler(Socket socket, Board board, VBox currentLobby, Stage stage, Scene scene, BlockingQueue<Packet> outQueue) {
        this.socket = socket;
        this.board = board;
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
                boolean ack;

                switch (packet.getType()) {
                    case ACK_USERNAME:
                        ack = packet.getAckUsername();
                        Platform.runLater(() -> board.handleAckUsername(ack));
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
                    case UPDATE_USERS_LIST:
                        List<String> users = packet.getRoomUsers();
                        Platform.runLater(() -> updateUsersListGUI(users));
                        break;
                    case NEW_DRAWING:
                        List<CompleteDraw> drawings = packet.receiveAllDrawings();
                        Platform.runLater(() -> this.updateDrawStack(drawings));
                        break;
                    case ACK_JOIN_ROOM:
                        if(packet.getAckJoinRoom()) {
                            Platform.runLater(this::handleNewRoomTransfer);
                        }
                        else {
                            outQueue.put(Packet.requestRoomsNames());
                            //TODO: find a way to display alert for not being able to join the room (or don't, I don't give a fuck)
                        }
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

//    private void handleAckUsername(boolean ack) {
//        if(ack) {
//            Text helloMsg = new Text("Hello " + username);
//            //user = new Text(username);
//            helloMsg.setStyle(CssLayouts.cssBottomLayoutText);
//            bottomM.getChildren().add(helloMsg);
//            login.setVisible(false);
//            input.signIn.close();
//        }
//        else {
//            displayAlert("Name exists", USERNAME_EXISTS);
//        }
//    }

    private void handlerSetUser(String username) {
        this.username = username;
    }

    private void setNewHost() {
        myRoom.setHost(true);
    }

    private void updateUsersListGUI(List<String> users) {
        myRoom.getOnlineUsersPanel().getChildren().clear();
        for (String user: users) {
            myRoom.getOnlineUsersPanel().getChildren().add(new Text(user));
        }
    }

    private void addUserToGUI(String user) {
        myRoom.getOnlineUsersPanel().getChildren().add(new Text(user));
        //myRoom.getOnlineUsers().add(new Text(user));
    }

    private void handleNewRoomTransfer() {
        String roomName = this.roomName;
        myRoom = new WhiteboardRoom(this.username, this);
        stage.setScene(myRoom.showBoard(stage,new Text(username),scene, outQueue));
    }

    //different users don't have the same rooms list.

    /* This method will update the UI with the lobby rooms. */
    private void handleRoomsList(List<String> roomsNames) {
        //if(drawingRooms.isEmpty()) { return; }
        lobby.getChildren().clear();
        this.roomsNames.clear();
        VBox[] containers = new VBox[roomsNames.size()];
        /* Organizing the rooms in the layout. */
        for(int i = 0; i < roomsNames.size(); i++) {
            this.roomsNames.add(roomsNames.get(i));
            containers[i] = new VBox();
            containers[i].setAlignment(Pos.CENTER);
            containers[i].getChildren().add(new Label(roomsNames.get(i)));
            lobby.getChildren().add(containers[i]);
            lobby.getChildren().get(i).setStyle(CssLayouts.cssLobbiesStyle);

            addEventListener(lobby, i, roomsNames);
        }
    }

    //in this function the user will receive all the drawings currently in the room.

    private void addEventListener(VBox lobby, int i, List<String> roomsNames) {
        lobby.getChildren().get(i).setOnMouseClicked(e -> {
            if(board.isLoggedIn) {
                try {
                    outQueue.put(Packet.requestJoinRoom(roomsNames.get(i)));
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
            }
            else {
                board.displayAlert("", board.EMTER_LOBBY_TEXT);
            }
        });
        lobby.getChildren().get(i).setOnMouseEntered(e -> {
            lobby.setCursor(Cursor.HAND);
        });
        lobby.getChildren().get(i).setOnMouseExited(e -> {
            lobby.setCursor(Cursor.DEFAULT);
        });
    }

    private void handleReceivedMessage(String msg) {
        Text msgToReceive = new Text(msg);
        msgToReceive.setStyle(CssLayouts.cssChatText);
        msgToReceive.setWrappingWidth(CHAT_MESSAGE_WRAPPING_WIDTH);
        //System.out.println(currRoom);
        myRoom.getChatBox().getChildren().add(msgToReceive);
    }

    public void setRoomName(String name) {
        this.roomName = name;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public List<String> getRoomsNames() {
        return roomsNames;
    }

    public void updateDrawStack(List<CompleteDraw> drawings) {
        if(myRoom == null) { return; }
        myRoom.myDraws.clear();
        for(int i = 0; i < drawings.size(); i++) {
            myRoom.myDraws.add(convertToMyDraw(drawings.get(i)));
        }
        //myRoom.myDraws.addAll(drawingStack);
        myRoom.repaint();
    }

    private MyDraw convertToMyDraw(CompleteDraw completeDraw) {
        String shape = completeDraw.getShape();
        Color color = Color.web(completeDraw.getColor());
        switch (shape) {
            case "MyBrush":
                MyBrush drawing =  new MyBrush(completeDraw.getXPoints().get(0), completeDraw.getYPoints().get(0),
                        color, completeDraw.getThickness(), completeDraw.isFill());
                drawing.setXPoints(completeDraw.getXPoints());
                drawing.setYPoints(completeDraw.getYPoints());
                return drawing;
            case "MyLine":
                return new MyLine(completeDraw.getX1(), completeDraw.getY1(), completeDraw.getX2(), completeDraw.getY2(),
                        color, completeDraw.getThickness());
            case "MyRect":
                return new MyRect(completeDraw.getX1(), completeDraw.getY1(), completeDraw.getX2(), completeDraw.getY2(),
                        color, completeDraw.getThickness(), completeDraw.isFill());
            case "MyRoundRect":
                return new MyRoundRect(completeDraw.getX1(), completeDraw.getY1(), completeDraw.getX2(), completeDraw.getY2(),
                        color, completeDraw.getThickness(), completeDraw.isFill(), completeDraw.getArcW(), completeDraw.getArcH());
            case "MyOval":
                return new MyOval(completeDraw.getX1(), completeDraw.getY1(), completeDraw.getX2(), completeDraw.getY2(),
                        color, completeDraw.getThickness(), completeDraw.isFill());
            case "TextBox":
                return new TextBox(completeDraw.getX1(), completeDraw.getY1(), color, completeDraw.getText());
        }
        return null;
    }
}

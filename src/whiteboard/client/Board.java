package whiteboard.client;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import whiteboard.Connection;
import whiteboard.Packet;
import whiteboard.server.Handler;
import whiteboard.server.Server;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class Board extends Application{

    private GraphicsContext gc;

    private Scene lobby;

    private Button redo, undo, clear, backToLobby, exit;

    private Slider thickness;

    private ColorPicker colorChooser;

    private InputHandler input;

    private Text topM = new Text("Welcome to Whiteboard! Draw your minds out!"), user = new Text("AAA");

    final String /* pSTitle = "Password too short", pSText = "Password must be at least 6 characters long.",
            regisTitle = "Successfully registered", regisText = "You've been successfully registered and logged in.",
            signInTitle = "Successfully logged in", signInText = "You've been successfully logged in.",
            pWTitle = "Wrong password", pWText = "You've entered a wrong password, please try again.",
            enterLobbyText = "You must be logged in before entering a lobby",*/ EMPTY_USERNAME_TEXT = "Username can't be blank.",
            USERNAME_EXISTS = "This username has been chosen already.",
            programExplained = "This program is a whiteboard.\nThe whiteboard is a canvas where multiple users can draw on at the same time.\n" +
                    "First the user logs in\\registers in the database of the program, and then chooses to enter an existing lobby or create " +
                    "one of their own.\nFrom there the user enters the whiteboard where they can draw freely on the board or add shapes and text" +
                    " to it!.\nA user is also able to see other online users in the same lobby at the left side of the program.",
            BLANK_ROOM_TITLE = "Blank room name", BLANK_ROOM_MSG = "Room name can't be blank",
            SAME_ROOM_NAME_TITLE = "Room already exists", SAME_ROOM_NAME_MSG = "There's a room with that name already, please choose a different name.";

    private final String[] shapes = {"Brush", "Line", "Oval", "Rectangle", "Rounded Rectangle", "Text"};
    private final ComboBox<String> shapeChooser = new ComboBox<>();
    private final Stack<MyDraw> myDraws = new Stack<>();
    private final Stack<MyDraw> deletedDraws = new Stack<>();

    private boolean toFill = false, isRoundRectChosen = false, isLoggedIn = false;
    private Color color = Color.BLACK; // Default color is black.

    // If the user isn't the host don't clear the board.
    private final boolean isHost = false;

    private final int CANVAS_HEIGHT = 590, CANVAS_WIDTH = 900, DEFAULT_ARC_VALUE = 10;

    public static final String DATABASE_URL = "jdbc:sqlite:saved_drawing.db";

    private Socket socket;
    private BlockingQueue<Packet> outQueue = new LinkedBlockingQueue<Packet>();

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) throws IOException /* throws Exception */ {

        final int GRID_MENU_SPACING = 10, LEFT_MENU_WIDTH = 200, RIGHT_MENU_WIDTH = 300, TOP_MENU_HEIGHT = 60,
                BOTTOM_MENU_LEFT = 220, TOOLBAR_HEIGHT = 60, TEXT_WRAPPING_WIDTH = 125, DRAWING_TEXT_DIALOG_WINDOW_WIDTH = 300,
                DRAWING_TEXT_DIALOG_WINDOW_HEIGHT = 200, CHAT_MESSAGE_WRAPPING_WIDTH = 230, EXPLAIN_TEXT_WRAPPING_WIDTH = 280,
                ROOM_SPACING = 20, BOTTOM_MENU_LOBBY_LEFT = 420, BOTTOM_MENU_LOBBY_SPACING = 150, LOGIN_WINDOW_WIDTH = 250, LOGIN_WINDOW_HEIGHT = 120,
                MIN_LINE_THICKNESS = 1, MAX_LINE_THICKNESS = 15;


        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight() - TOOLBAR_HEIGHT;

    /******************************** Lobby scene ********************************/

        /******************************** Top menu ********************************/

        Text welcome = new Text("Welcome to the lobby!");
        HBox title = new HBox();
        title.getChildren().add(welcome);
        welcome.setFill(Color.GREEN);
        title.setStyle(CssLayouts.cssTopLayout);
        title.setMinHeight(TOP_MENU_HEIGHT);

        /******************************** Left menu ********************************/

        AnchorPane lobbyLeftMenu = new AnchorPane();
        lobbyLeftMenu.setStyle(CssLayouts.cssBorder);
        lobbyLeftMenu.setMaxWidth(LEFT_MENU_WIDTH);
        lobbyLeftMenu.setMinWidth(LEFT_MENU_WIDTH);
//        Image brush = new Image("brushes-3129361_640.jpg");
//        ImageView leftMenuImage = new ImageView(brush);
//        leftMenuImage.fitWidthProperty().bind(lobbyLeftMenu.widthProperty());
//        leftMenuImage.fitHeightProperty().bind(lobbyLeftMenu.heightProperty());
//        lobbyLeftMenu.getChildren().add(leftMenuImage);
        //TODO: add a picture here of a brush maybe?

        /******************************** Right menu - info ********************************/

        Text toKnow = new Text(programExplained);
        toKnow.setStyle(CssLayouts.cssExplanationText);
        toKnow.setWrappingWidth(EXPLAIN_TEXT_WRAPPING_WIDTH);
        VBox info = new VBox();
        info.getChildren().add(toKnow);
        setLayoutWidth(info, RIGHT_MENU_WIDTH);
        //TODO: check out why the fuck it doesn't work as a whole string.
        info.setStyle(info.getStyle() + ";\n-fx-padding: 6 0 0 6;");

        /******************************** Center menu ********************************/

        BorderPane test = new BorderPane();
        ScrollPane lobbyCenter = new ScrollPane();
        VBox lobbies = new VBox(), empty1 = new VBox(), empty2 = new VBox();
        empty1.getChildren().add(new Text("AAAAA"));
        empty2.getChildren().add(new Text("BBBB"));
        empty1.setStyle(CssLayouts.cssBorder);
        empty2.setStyle(CssLayouts.cssBorder);
        test.setCenter(lobbyCenter);
        test.setBottom(empty1);
        test.setTop(empty2);
        lobbyCenter.setStyle(CssLayouts.cssBorder);
        lobbyCenter.setContent(lobbies);
        lobbyCenter.setVvalue(1.0);
        lobbyCenter.setFitToWidth(true);

        /******************************** Bottom menu ********************************/

        Button createR = new Button("Create room"), login = new Button("Log in"), refreshRooms = new Button("Refresh rooms"),
        loadDrawing = new Button("Load drawing");
        HBox bottomM = new HBox();
        bottomM.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING));
        bottomM.setSpacing(BOTTOM_MENU_LOBBY_SPACING);
        bottomM.getChildren().addAll(createR, loadDrawing, refreshRooms, login);
        bottomM.setAlignment(Pos.CENTER);
        bottomM.setStyle(CssLayouts.cssBottomLayout);

        /******************************* Load drawing button event handler ******************************/

        loadDrawing.setOnAction(e -> {


            TextField roomNameTextBox = new TextField();
            Button btn = new Button("OK");

            // Dialog window wrapper.
            BorderPane textBox = new BorderPane();
            textBox.setCenter(roomNameTextBox);
            textBox.setBottom(btn);
            BorderPane.setAlignment(btn, Pos.BOTTOM_CENTER);

            // Dialog window.
            final Stage dialog = new Stage();
            dialog.initModality(Modality.NONE);
            dialog.initOwner(stage);
            Scene dialogScene = new Scene(textBox, 200, 90);
            dialog.setScene(dialogScene);
            dialog.show();

            AtomicBoolean roomNameExists = new AtomicBoolean(false);
            btn.setOnAction(eBtn -> {
                if(roomNameTextBox.getText().trim().length() == 0) {
                    displayAlert(BLANK_ROOM_TITLE, BLANK_ROOM_MSG);
                    roomNameExists.set(true);
                    input.setRoomName(null);
                }
                else {
                    for (int i = 0; i < input.getRoomsNames().size(); i++) {
                        if (input.getRoomsNames().get(i).equals(roomNameTextBox.getText())) {
                            roomNameExists.set(true);
                            break;
                        }
                    }
                }
                if(roomNameExists.get()) {
                    input.setRoomName(null);
                    displayAlert(SAME_ROOM_NAME_TITLE, SAME_ROOM_NAME_MSG);
                }
                else {
                    input.setRoomName(roomNameTextBox.getText());
                }

                dialog.close();
                List<CompleteDraw> completeDraws;
                //boolean tableExists = true;
                //java.sql.Connection connection = null;
                try (java.sql.Connection connection = DriverManager.getConnection(Board.DATABASE_URL)){

                    //TODO: Check if the db exists, if it doesn't tell the user to jump off a 1k height bridge.
                    Statement statement = connection.createStatement();
                    statement.setQueryTimeout(2);  // set timeout to 30 sec.
                    ResultSet resultSet = statement.executeQuery("SELECT * FROM save");


                    // get ResultSet's meta data
                    //ResultSetMetaData metaData = resultSet.getMetaData();
                    //int numberOfColumns = metaData.getColumnCount();

                    //if(resultSet.next()) {
                    resultSet.next();
                    byte[] arr = resultSet.getBytes("DRAWING");
//                    InputStream is = blob.getBinaryStream();
//                    byte[] arr = IOUtils.toByteArray(is);
                    completeDraws = (List<CompleteDraw>) deserialize(arr);
                    try {
                        outQueue.put(Packet.createRoomWithDrawings(input.getRoomName(), completeDraws));
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                    // }
//                    while (resultSet.next()) {
//                        //for (int i = 1; i <= metaData.getColumnCount(); i++) {
//
//                            //TODO: read all the other variables from the table into color, thickness, x1, y1, x2, y2, fill, arcW, arcH, ...
//
//                            if(resultSet.getObject(1).equals("MyBrush")) {
//
//                                // xString == "1.3, 4.2, 5.6, 9.7"
//                                String xString = resultSet.getString("XPOINTS"); // Read into this variable from the database
//                                String yString = resultSet.getString("YPOINTS"); // Read into this variable from the database
//
//                                // Take the X,Y points strings and convert them to ArrayList<Double>
//                                // xStringArr == ["1.3", "4.2", "5.6", "9.7"]
//                                String[] xStringArr = xString.split(", ");
//                                // xPoints == ArrayList[1.3, 4.2, 5.6, 9.7]
//                                ArrayList<Double> xPoints = new ArrayList<>();
//                                for (String x : xStringArr) {
//                                    xPoints.add(Double.parseDouble(x));
//                                }
//                                String[] yStringArr = yString.split(", ");
//                                ArrayList<Double> yPoints = new ArrayList<>();
//                                for (String y : yStringArr) {
//                                    yPoints.add(Double.parseDouble(y));
//                                }
//
//
//
//                                completeDraws.add(new CompleteDraw("#" + resultSet.getString("COLOR"),
//                                        resultSet.getDouble("THICKNESS"),
//                                        resultSet.getDouble("X1"),
//                                        resultSet.getDouble("Y1"),
//                                        resultSet.getDouble("X2"),
//                                        resultSet.getDouble("Y2"),
//                                        resultSet.getBoolean("FILL"),
//                                        resultSet.getInt("ARCW"),
//                                        resultSet.getInt("ARCH"),
//                                        /*deserialize(resultSet.getObject("XPOINTS"))*/null,
//                                        /*deserialize(resultSet.getObject("YPOINTS"))*/null, "MyBrush",
//                                        resultSet.getString("TEXTBOX")));
//                                // After getting all the variables of CompleteDraw from the database
////                    completeDraws.add(new CompleteDraw(color, thickness, x1, y1, x2, y2, fill, arcW, arcH, xPoints, yPoints, shape));
//                            }
//
//                            else if(resultSet.getObject(1).equals("MyLine")) {
//                                completeDraws.add(new CompleteDraw("#" + resultSet.getString("COLOR"),
//                                        resultSet.getDouble("THICKNESS"),
//                                        resultSet.getDouble("X1"),
//                                        resultSet.getDouble("Y1"),
//                                        resultSet.getDouble("X2"),
//                                        resultSet.getDouble("Y2"),
//                                        resultSet.getBoolean("FILL"),
//                                        resultSet.getInt("ARCW"),
//                                        resultSet.getInt("ARCH"),
//                                        null, null, "MyLine",
//                                        resultSet.getString("TEXTBOX")));
//                            }
//
//                            else if(resultSet.getObject(1).equals("MyOval")) {
//                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
//                                        resultSet.getDouble("THICKNESS"),
//                                        resultSet.getDouble("X1"),
//                                        resultSet.getDouble("Y1"),
//                                        resultSet.getDouble("X2"),
//                                        resultSet.getDouble("Y2"),
//                                        resultSet.getBoolean("FILL"),
//                                        resultSet.getInt("ARCW"),
//                                        resultSet.getInt("ARCH"),
//                                        null, null, "MyOval",
//                                        resultSet.getString("TEXTBOX")));
//                            }
//
//                            else if(resultSet.getObject(1).equals("MyRect")) {
//                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
//                                        resultSet.getDouble("THICKNESS"),
//                                        resultSet.getDouble("X1"),
//                                        resultSet.getDouble("Y1"),
//                                        resultSet.getDouble("X2"),
//                                        resultSet.getDouble("Y2"),
//                                        resultSet.getBoolean("FILL"),
//                                        resultSet.getInt("ARCW"),
//                                        resultSet.getInt("ARCH"),
//                                        null, null, "MyRect",
//                                        resultSet.getString("TEXTBOX")));
//                            }
//
//                            else if(resultSet.getObject(1).equals("MyRoundRect")) {
//                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
//                                        resultSet.getDouble("THICKNESS"),
//                                        resultSet.getDouble("X1"),
//                                        resultSet.getDouble("Y1"),
//                                        resultSet.getDouble("X2"),
//                                        resultSet.getDouble("Y2"),
//                                        resultSet.getBoolean("FILL"),
//                                        resultSet.getInt("ARCW"),
//                                        resultSet.getInt("ARCH"),
//                                        null, null, "MyRoundRect",
//                                        resultSet.getString("TEXTBOX")));
//                            }
//
//                            else if(resultSet.getObject(1).equals("TextBox")) {
//                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
//                                        resultSet.getDouble("THICKNESS"),
//                                        resultSet.getDouble("X1"),
//                                        resultSet.getDouble("Y1"),
//                                        resultSet.getDouble("X2"),
//                                        resultSet.getDouble("Y2"),
//                                        resultSet.getBoolean("FILL"),
//                                        resultSet.getInt("ARCW"),
//                                        resultSet.getInt("ARCH"),
//                                        null, null, "TextBox",
//                                        resultSet.getString("TEXTBOX")));
//                            }
//                        //}
//                    }

                    //TODO: this block gives NotSerializableException

                }
                catch (SQLException | IOException | ClassNotFoundException sqlException)
                {
                    sqlException.printStackTrace();
                    System.exit(1);
                }
            });

            //if (input.getRoomName() == null) { return; }

//            List<CompleteDraw> completeDraws;
//            //boolean tableExists = true;
//            java.sql.Connection connection = null;
//            try {
//                connection = DriverManager.getConnection(Board.DATABASE_URL);
//                Statement statement = connection.createStatement();
//                statement.setQueryTimeout(30);  // set timeout to 30 sec.
//                ResultSet resultSet = statement.executeQuery("SELECT * FROM save");
//
//
//                // get ResultSet's meta data
//                ResultSetMetaData metaData = resultSet.getMetaData();
//                int numberOfColumns = metaData.getColumnCount();
//
//                //if(resultSet.next()) {
//                resultSet.next();
//                    byte[] arr = resultSet.getBytes("DRAWING");
////                    InputStream is = blob.getBinaryStream();
////                    byte[] arr = IOUtils.toByteArray(is);
//                    completeDraws = (List<CompleteDraw>) deserialize(arr);
//                    try {
//                        outQueue.put(Packet.createRoomWithDrawings(input.getRoomName(), completeDraws));
//                    } catch (InterruptedException exception) {
//                        exception.printStackTrace();
//                    }
//               // }
////                    while (resultSet.next()) {
////                        //for (int i = 1; i <= metaData.getColumnCount(); i++) {
////
////                            //TODO: read all the other variables from the table into color, thickness, x1, y1, x2, y2, fill, arcW, arcH, ...
////
////                            if(resultSet.getObject(1).equals("MyBrush")) {
////
////                                // xString == "1.3, 4.2, 5.6, 9.7"
////                                String xString = resultSet.getString("XPOINTS"); // Read into this variable from the database
////                                String yString = resultSet.getString("YPOINTS"); // Read into this variable from the database
////
////                                // Take the X,Y points strings and convert them to ArrayList<Double>
////                                // xStringArr == ["1.3", "4.2", "5.6", "9.7"]
////                                String[] xStringArr = xString.split(", ");
////                                // xPoints == ArrayList[1.3, 4.2, 5.6, 9.7]
////                                ArrayList<Double> xPoints = new ArrayList<>();
////                                for (String x : xStringArr) {
////                                    xPoints.add(Double.parseDouble(x));
////                                }
////                                String[] yStringArr = yString.split(", ");
////                                ArrayList<Double> yPoints = new ArrayList<>();
////                                for (String y : yStringArr) {
////                                    yPoints.add(Double.parseDouble(y));
////                                }
////
////
////
////                                completeDraws.add(new CompleteDraw("#" + resultSet.getString("COLOR"),
////                                        resultSet.getDouble("THICKNESS"),
////                                        resultSet.getDouble("X1"),
////                                        resultSet.getDouble("Y1"),
////                                        resultSet.getDouble("X2"),
////                                        resultSet.getDouble("Y2"),
////                                        resultSet.getBoolean("FILL"),
////                                        resultSet.getInt("ARCW"),
////                                        resultSet.getInt("ARCH"),
////                                        /*deserialize(resultSet.getObject("XPOINTS"))*/null,
////                                        /*deserialize(resultSet.getObject("YPOINTS"))*/null, "MyBrush",
////                                        resultSet.getString("TEXTBOX")));
////                                // After getting all the variables of CompleteDraw from the database
//////                    completeDraws.add(new CompleteDraw(color, thickness, x1, y1, x2, y2, fill, arcW, arcH, xPoints, yPoints, shape));
////                            }
////
////                            else if(resultSet.getObject(1).equals("MyLine")) {
////                                completeDraws.add(new CompleteDraw("#" + resultSet.getString("COLOR"),
////                                        resultSet.getDouble("THICKNESS"),
////                                        resultSet.getDouble("X1"),
////                                        resultSet.getDouble("Y1"),
////                                        resultSet.getDouble("X2"),
////                                        resultSet.getDouble("Y2"),
////                                        resultSet.getBoolean("FILL"),
////                                        resultSet.getInt("ARCW"),
////                                        resultSet.getInt("ARCH"),
////                                        null, null, "MyLine",
////                                        resultSet.getString("TEXTBOX")));
////                            }
////
////                            else if(resultSet.getObject(1).equals("MyOval")) {
////                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
////                                        resultSet.getDouble("THICKNESS"),
////                                        resultSet.getDouble("X1"),
////                                        resultSet.getDouble("Y1"),
////                                        resultSet.getDouble("X2"),
////                                        resultSet.getDouble("Y2"),
////                                        resultSet.getBoolean("FILL"),
////                                        resultSet.getInt("ARCW"),
////                                        resultSet.getInt("ARCH"),
////                                        null, null, "MyOval",
////                                        resultSet.getString("TEXTBOX")));
////                            }
////
////                            else if(resultSet.getObject(1).equals("MyRect")) {
////                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
////                                        resultSet.getDouble("THICKNESS"),
////                                        resultSet.getDouble("X1"),
////                                        resultSet.getDouble("Y1"),
////                                        resultSet.getDouble("X2"),
////                                        resultSet.getDouble("Y2"),
////                                        resultSet.getBoolean("FILL"),
////                                        resultSet.getInt("ARCW"),
////                                        resultSet.getInt("ARCH"),
////                                        null, null, "MyRect",
////                                        resultSet.getString("TEXTBOX")));
////                            }
////
////                            else if(resultSet.getObject(1).equals("MyRoundRect")) {
////                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
////                                        resultSet.getDouble("THICKNESS"),
////                                        resultSet.getDouble("X1"),
////                                        resultSet.getDouble("Y1"),
////                                        resultSet.getDouble("X2"),
////                                        resultSet.getDouble("Y2"),
////                                        resultSet.getBoolean("FILL"),
////                                        resultSet.getInt("ARCW"),
////                                        resultSet.getInt("ARCH"),
////                                        null, null, "MyRoundRect",
////                                        resultSet.getString("TEXTBOX")));
////                            }
////
////                            else if(resultSet.getObject(1).equals("TextBox")) {
////                                completeDraws.add(new CompleteDraw(resultSet.getString("COLOR"),
////                                        resultSet.getDouble("THICKNESS"),
////                                        resultSet.getDouble("X1"),
////                                        resultSet.getDouble("Y1"),
////                                        resultSet.getDouble("X2"),
////                                        resultSet.getDouble("Y2"),
////                                        resultSet.getBoolean("FILL"),
////                                        resultSet.getInt("ARCW"),
////                                        resultSet.getInt("ARCH"),
////                                        null, null, "TextBox",
////                                        resultSet.getString("TEXTBOX")));
////                            }
////                        //}
////                    }
//
//                    //TODO: this block gives NotSerializableException
//
//            }
//            catch (SQLException | IOException | ClassNotFoundException sqlException)
//            {
//                sqlException.printStackTrace();
//                System.exit(1);
//            }
        });

        /******************************* Refresh rooms button event handler ******************************/

        refreshRooms.setOnAction(e -> {
            try {
                outQueue.put(Packet.requestRoomsNames());
            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        });

        /******************************** Create room button event handler *******************************/

        createR.setOnAction(e -> {

            TextField roomName = new TextField();
            Button btn = new Button("OK");
            Label roomNameLabel = new Label("Choose room name");
            roomNameLabel.setStyle("-fx-font-weight: bold");

            // Dialog window wrapper.
            BorderPane textBox = new BorderPane();
            textBox.setTop(roomNameLabel);
            textBox.setCenter(roomName);
            textBox.setBottom(btn);
            BorderPane.setAlignment(roomNameLabel, Pos.TOP_CENTER);
            BorderPane.setAlignment(btn, Pos.BOTTOM_CENTER);

            // Dialog window.
            final Stage dialog = new Stage();
            dialog.initModality(Modality.NONE);
            dialog.initOwner(stage);
            dialog.setTitle("Create room");
            Scene dialogScene = new Scene(textBox, 240, 100);
            dialog.setScene(dialogScene);
            dialog.show();

            btn.setOnAction(eBtn -> {

                //TODO: send a string to the server.
                boolean roomNameExists = false;
                if(roomName.getText().trim().length() == 0) {
                    displayAlert(BLANK_ROOM_TITLE, BLANK_ROOM_MSG);
                }
                else {
                    for (int i = 0; i < input.getRoomsNames().size(); i++) {
                        if (input.getRoomsNames().get(i).equals(roomName.getText())) {
                            roomNameExists = true;
                            break;
                        }
                    }
                    if(roomNameExists) {
                        displayAlert(SAME_ROOM_NAME_TITLE, SAME_ROOM_NAME_MSG);
                    }
                    else {
                        try {
                            input.setRoomName(roomName.getText());
                            outQueue.put(Packet.createRoom(roomName.getText()));
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                }
                dialog.close();
            });
        });

        /******************************** Login button event handler *******************************/

        login.setOnAction(e -> {
            Button confirm = new Button("Choose name");
            TextField nickname = new TextField();
            //PasswordField password = new PasswordField();
            Label username = new Label("Username:");
            //Label pass = new Label("Password");

            GridPane loginWindow = new GridPane();
            loginWindow.setPadding(new Insets(GRID_MENU_SPACING,GRID_MENU_SPACING,GRID_MENU_SPACING,ROOM_SPACING));
            loginWindow.setHgap(GRID_MENU_SPACING);
            loginWindow.setVgap(GRID_MENU_SPACING);

            // Arranging items on the window.
            GridPane.setConstraints(username, 0, 0);
            GridPane.setConstraints(nickname, 1, 0);
            //GridPane.setConstraints(pass, 0, 1);
            //GridPane.setConstraints(password, 1, 1);
            GridPane.setConstraints(confirm,1,2);

            loginWindow.getChildren().addAll(username, nickname, confirm);

            input.signIn = new Stage();
            input.signIn.setTitle("Nickname screen");
            input.signIn.initModality(Modality.NONE);
            input.signIn.initOwner(stage);
            Scene loginScene = new Scene(loginWindow, LOGIN_WINDOW_WIDTH, LOGIN_WINDOW_HEIGHT);
            input.signIn.setScene(loginScene);
            input.signIn.show();

            //TODO: finish identifying the user.

            confirm.setOnAction(eConfirm -> {
//                if(nickname.getText().trim().length() == 0) {
//                    displayAlert("Blank name",EMPTY_USERNAME_TEXT);
//                }
//                else {
//                    boolean nameExists = false;
//                    for (int i = 0; i < Server.handlers.size() - 1; i++) {
//                        if (.equals(nickname.getText())) {
//                            System.out.println("AAA");
//                            nameExists = true;
//                            break;
//                        }
//                    }
//                    if(nameExists) {
//                        displayAlert("Name exists", USERNAME_EXISTS);
//                    }
//                    else {
//                        try {
//                            outQueue.put(Packet.setUsername(nickname.getText()));
//                        } catch (InterruptedException exception) {
//                            exception.printStackTrace();
//                        }
//                        Text helloMsg = new Text("Hello " + nickname.getText());
//                        user = new Text(nickname.getText());
//                        helloMsg.setStyle(CssLayouts.cssBottomLayoutText);
//                        bottomM.getChildren().add(helloMsg);
//                        login.setVisible(false);
//                        signIn.close();
//                    }
//                }
                try {
                    outQueue.put(Packet.requestUsername(nickname.getText()));
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
                if(input.usernameAck) {
                    try {
                        outQueue.put(Packet.setUsername(nickname.getText()));
                    } catch (InterruptedException exception) {
                        exception.printStackTrace();
                    }
                    Text helloMsg = new Text("Hello " + nickname.getText());
                    user = new Text(nickname.getText());
                    helloMsg.setStyle(CssLayouts.cssBottomLayoutText);
                    bottomM.getChildren().add(helloMsg);
                    login.setVisible(false);
                    input.signIn.close();
                }
                else {
                    displayAlert("Name exists", USERNAME_EXISTS);
                }
            });
        });


        /******************************** Dividing the lobby scene layout into sections and creating the scene ********************************/

        BorderPane lobbyLayout = new BorderPane();
        lobbyLayout.setTop(title);
        lobbyLayout.setLeft(lobbyLeftMenu);
        lobbyLayout.setRight(info);
        lobbyLayout.setCenter(test);
        lobbyLayout.setBottom(bottomM);
        lobby = new Scene(lobbyLayout, width, height);

    /******************************** Lobby scene - End ********************************/

    /********************************** Setting up socket ************************************/
        try {
            socket = new Socket(Connection.DOMAIN, Connection.PORT);
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    /********************************** Client side ************************************/

        input = new InputHandler(socket, lobbies, stage, lobby, outQueue);
        Thread threadInputHandler = new Thread(input);
        threadInputHandler.setDaemon(true);
        threadInputHandler.start();

    /********************************** Client side ************************************/

        OutputHandler output = new OutputHandler(socket, outQueue);
        Thread threadOutputHandler = new Thread(output);
        threadOutputHandler.setDaemon(true);
        threadOutputHandler.start();

    /******************************** Setting the stage ********************************/

        stage.setTitle("Lobby");
        stage.setScene(lobby);
        stage.sizeToScene();
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());

        try {
            outQueue.put(Packet.requestRoomsNames());
            //TODO: switch the string here with a name of the user's choice.
            outQueue.put(Packet.setUsername("AAA"));
        } catch (InterruptedException interruptedException) {
            interruptedException.printStackTrace();
        }
    }


    /******************************** Functions ********************************/

    // To prevent repeated code.
    private void setLayoutWidth(Pane p, int width) {
        if(p == null) { return; }
        p.setStyle(CssLayouts.cssBorder);
        p.setMaxWidth(width);
        p.setMinWidth(width);
    }


//    private void connectToDatabase(String username, String password) {
//
//        // connect to database books and query database.
//        try (JdbcRowSet rowSet = RowSetProvider.newFactory().createJdbcRowSet()) {
//            // specify JdbcRowSet properties
//            rowSet.setUrl(DATABASE_URL);
//            rowSet.setUsername(USERNAME);
//            rowSet.setPassword(PASSWORD);
//            // Get every user from the database to check if the client's username exists.
//            rowSet.setCommand("SELECT * FROM users WHERE username = ?"); // set query
//            rowSet.setString(1, username);
//            rowSet.execute(); // execute query
//
//            // Register the new user.
//            if(!rowSet.next()) {
//                // add user to database.
//                rowSet.moveToInsertRow();
//                rowSet.updateString("username", username);
//                rowSet.updateString("password", password);
//                rowSet.insertRow();
//                isLoggedIn = true;
//                user = new Text(username);
//                input.setUser(user.getText());
//                displayAlert(regisTitle, regisText);
//            }
//            else {
//                // whiteboard.client.whiteboard.server.User exists but password is wrong.
//                if(!rowSet.getObject(2).equals(password)) {
//                    displayAlert(pWTitle, pWText);
//                }
//                // whiteboard.client.whiteboard.server.User logged in.
//                else {
//                    isLoggedIn = true;
//                    user = new Text(username);
//                    input.setUser(user.getText());
//                    displayAlert(signInTitle, signInText);
//                }
//            }
//        }
//        catch (SQLException sqlException)
//        {
//            sqlException.printStackTrace();
//            System.exit(1);
//        }
//    }

    // To prevent repeated code.
    private void displayAlert(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    //TODO: have the user pick a nickname.
    //TODO: add painting to nametag when someone's drawing.

    //TODO: Idea: keep the information about rooms in the database and change scenes acording to that.
    //MAKE A CLASS TO CALL BOARD

    //TODO: maybe make a close room button.
    //TODO: make the cursor look like a pen.
    //TODO: maybe add background image to the other menus.
}


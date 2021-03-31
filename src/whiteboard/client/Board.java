/* This class represents the lobby scene, the screen the client will see when opening the program. */

package whiteboard.client;

import javafx.application.Application;
import javafx.application.Platform;
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
import whiteboard.server.IServerHandler;

import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class Board extends Application{

    private GraphicsContext gc;

    private Scene lobby;

    private Button redo, undo, clear, backToLobby, exit;

    private Button login = new Button("Log in");

    private Slider thickness;

    private ColorPicker colorChooser;

    private InputHandler input;
    private RMIHandler rmiQueue;

    private Text topM = new Text("Welcome to Whiteboard! Draw your minds out!");

    final String EMTER_LOBBY_TEXT = "You must be logged in before entering a lobby", CREATE_ROOM_TEXT = "You must be logged in before creating a room",
            EMPTY_USERNAME_TEXT = "Username can't be blank.", USERNAME_EXISTS = "This username has been chosen already.",
            PROGRAM_EXPLAINATION = "This program is a whiteboard.\nThe whiteboard is a canvas where multiple users can draw on at the same time.\n" +
                    "First the user logs in\\registers in the database of the program, and then chooses to enter an existing lobby or create " +
                    "one of their own.\nFrom there the user enters the whiteboard where they can draw freely on the board or add shapes and text" +
                    " to it!.\nA user is also able to see other online users in the same lobby at the left side of the program.",
            BLANK_ROOM_TITLE = "Blank room name", BLANK_ROOM_MSG = "Room name can't be blank",
            SAME_ROOM_NAME_TITLE = "Room already exists", SAME_ROOM_NAME_MSG = "There's a room with that name already, please choose a different name.",
            USERNAME_TOO_LONG = "Username cannot exceed 16 characters.", ROOM_NAME_TOO_LONG = "Please choose a shorter room name.";

    private final String[] shapes = {"Brush", "Line", "Oval", "Rectangle", "Rounded Rectangle", "Text"};
    private final ComboBox<String> shapeChooser = new ComboBox<>();
    private final Stack<MyDraw> myDraws = new Stack<>();
    private final Stack<MyDraw> deletedDraws = new Stack<>();

    private boolean toFill = false, isRoundRectChosen = false;
    public boolean isLoggedIn = false;
    private Color color = Color.BLACK; // Default color is black.

    private HBox bottomM = new HBox();

    private final int CANVAS_HEIGHT = 590, CANVAS_WIDTH = 900, DEFAULT_ARC_VALUE = 10, TEXT_WRAPPING_WIDTH = 200;

    public static final String DATABASE_URL = "jdbc:sqlite:saved_drawing.db";

    private Socket socket;
    private IServerHandler stub;

    public static void main(String[] args) { launch(args); }

    @Override
    public void start(Stage stage) throws IOException {

        final int GRID_MENU_SPACING = 10, LEFT_MENU_WIDTH = 200, RIGHT_MENU_WIDTH = 300, TOP_MENU_HEIGHT = 60,
                TOOLBAR_HEIGHT = 60, DRAWING_TEXT_DIALOG_WINDOW_WIDTH = 300,
                DRAWING_TEXT_DIALOG_WINDOW_HEIGHT = 200, CHAT_MESSAGE_WRAPPING_WIDTH = 230, EXPLAIN_TEXT_WRAPPING_WIDTH = 280,
                ROOM_SPACING = 20, BOTTOM_MENU_LOBBY_LEFT = 420, BOTTOM_MENU_LOBBY_SPACING = 75, LOGIN_WINDOW_WIDTH = 250, LOGIN_WINDOW_HEIGHT = 120,
                MIN_LINE_THICKNESS = 1, MAX_LINE_THICKNESS = 15, CREATE_ROOM_WINDOW_WIDTH = 200, CREATE_ROOM_WINDOW_HEIGHT = 90,
                USERNAME_CHAR_LIM = 16, ROOM_NAME_MAX_LENGTH = 100;


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

        Text toKnow = new Text(PROGRAM_EXPLAINATION);
        toKnow.setStyle(CssLayouts.cssExplanationText);
        toKnow.setWrappingWidth(EXPLAIN_TEXT_WRAPPING_WIDTH);
        VBox info = new VBox();
        info.getChildren().add(toKnow);
        setLayoutWidth(info, RIGHT_MENU_WIDTH);
        //TODO: check out why the fuck it doesn't work as a whole string.
        info.setStyle(info.getStyle() + ";\n-fx-padding: 6 0 0 6;");

        /******************************** Center menu ********************************/

        BorderPane mainLobby = new BorderPane();
        ScrollPane lobbyCenter = new ScrollPane();
        VBox lobbies = new VBox();
//        empty1.getChildren().add(new Text("AAAAA"));
//        empty2.getChildren().add(new Text("BBBB"));
//        empty1.setStyle(CssLayouts.cssBorder);
//        empty2.setStyle(CssLayouts.cssBorder);
        mainLobby.setCenter(lobbyCenter);
//        test.setBottom(empty1);
//        test.setTop(empty2);
        lobbyCenter.setStyle(CssLayouts.cssBorder);
        lobbyCenter.setContent(lobbies);
        lobbyCenter.setVvalue(1.0);
        lobbyCenter.setFitToWidth(true);

        /******************************** Bottom menu ********************************/

        Button createR = new Button("Create room"), refreshRooms = new Button("Refresh rooms"),
        loadDrawing = new Button("Load drawing");
        bottomM.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING));
        bottomM.setSpacing(BOTTOM_MENU_LOBBY_SPACING);
        bottomM.getChildren().addAll(createR, loadDrawing, refreshRooms, login);
        bottomM.setAlignment(Pos.CENTER);
        bottomM.setStyle(CssLayouts.cssBottomLayout);

        /******************************* Load drawing button event handler ******************************/

        /* Loads the last saved drawings from this client's database. */
        loadDrawing.setOnAction(e -> {

            if(isLoggedIn) {
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
                Scene dialogScene = new Scene(textBox, CREATE_ROOM_WINDOW_WIDTH, CREATE_ROOM_WINDOW_HEIGHT);
                dialog.setScene(dialogScene);
                dialog.show();

                /* Checks for valid room name. */
                AtomicBoolean roomNameExists = new AtomicBoolean(false);
                btn.setOnAction(eBtn -> {
                    if (roomNameTextBox.getText().length() == 0) {
                        displayAlert(BLANK_ROOM_TITLE, BLANK_ROOM_MSG);
                        roomNameExists.set(true);
                        input.setRoomName(null);
                    }
                    else if(roomNameTextBox.getText().trim().length() > ROOM_NAME_MAX_LENGTH) {
                        displayAlert("", ROOM_NAME_TOO_LONG);
                    }
                    else {
                        for (int i = 0; i < input.getRoomsNames().size(); i++) {
                            if (input.getRoomsNames().get(i).equals(roomNameTextBox.getText())) {
                                roomNameExists.set(true);
                                break;
                            }
                        }
                    }
                    if (roomNameExists.get()) {
                        input.setRoomName(null);
                        displayAlert(SAME_ROOM_NAME_TITLE, SAME_ROOM_NAME_MSG);
                    } else {
                        input.setRoomName(roomNameTextBox.getText());

                        List<CompleteDraw> completeDraws;
                        //boolean tableExists = true;
                        //java.sql.Connection connection = null;
                        try (java.sql.Connection connection = DriverManager.getConnection(Board.DATABASE_URL)) {

                            //TODO: Check if the db exists, if it doesn't tell the user to jump off a 1k height bridge.
//                            File file = new File (dbName);
//
//                            if(file.exists()) //here's how to check
//                            {
//                                System.out.print("This database name already exists");
//                            }
//                            else{
//
//                                Class.forName("SQLite.JDBCDriver").newInstance();
//                                conn = DriverManager.getConnection("jdbc:sqlite:/"+ dbName);
//                                stat = conn.createStatement();
//
//                            }
                            Statement statement = connection.createStatement();
                            statement.setQueryTimeout(2);  // set timeout to 30 sec.
                            ResultSet resultSet = statement.executeQuery("SELECT * FROM save");

                            resultSet.next();
                            byte[] arr = resultSet.getBytes("DRAWING");

                            completeDraws = (List<CompleteDraw>) deserialize(arr);
                            rmiQueue.put(() -> {
                                try {
                                    stub.handleCreateRoomWithDrawings(input.getRoomName(), completeDraws); /* There once was a return here. */
                                } catch (RemoteException remoteException) {
                                    remoteException.printStackTrace();
                                }
                            });

                        } catch (SQLException | IOException | ClassNotFoundException sqlException) {
                            sqlException.printStackTrace();
                            System.exit(1);
                        }
                    }
                    dialog.close();
//                    dialog.close();
//                    List<CompleteDraw> completeDraws;
//                    //boolean tableExists = true;
//                    //java.sql.Connection connection = null;
//                    try (java.sql.Connection connection = DriverManager.getConnection(Board.DATABASE_URL)) {
//
//                        //TODO: Check if the db exists, if it doesn't tell the user to jump off a 1k height bridge.
//                        Statement statement = connection.createStatement();
//                        statement.setQueryTimeout(2);  // set timeout to 30 sec.
//                        ResultSet resultSet = statement.executeQuery("SELECT * FROM save");
//
//
//                        // get ResultSet's meta data
//                        //ResultSetMetaData metaData = resultSet.getMetaData();
//                        //int numberOfColumns = metaData.getColumnCount();
//
//                        //if(resultSet.next()) {
//                        resultSet.next();
//                        byte[] arr = resultSet.getBytes("DRAWING");
////                    InputStream is = blob.getBinaryStream();
////                    byte[] arr = IOUtils.toByteArray(is);
//                        completeDraws = (List<CompleteDraw>) deserialize(arr);
//                        try {
//                            outQueue.put(Packet.createRoomWithDrawings(input.getRoomName(), completeDraws));
//                        } catch (InterruptedException exception) {
//                            exception.printStackTrace();
//                        }
//
//                    } catch (SQLException | IOException | ClassNotFoundException sqlException) {
//                        sqlException.printStackTrace();
//                        System.exit(1);
//                    }
                });
            }
            else {
                displayAlert("", CREATE_ROOM_TEXT);
            }
        });

        /******************************* Refresh rooms button event handler ******************************/

        refreshRooms.setOnAction(e -> {
            rmiQueue.put(() -> {
                try {
                    stub.handleGetRooms(); /* There once was a return here. */
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            });
        });

        /******************************** Create room button event handler *******************************/

        createR.setOnAction(e -> {

            if(isLoggedIn) {
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
                Scene dialogScene = new Scene(textBox, CREATE_ROOM_WINDOW_WIDTH, CREATE_ROOM_WINDOW_HEIGHT);
                dialog.setScene(dialogScene);
                dialog.show();

                btn.setOnAction(eBtn -> {

                    boolean roomNameExists = false;
                    if (roomName.getText().trim().length() == 0) {
                        displayAlert(BLANK_ROOM_TITLE, BLANK_ROOM_MSG);
                    }
                    else if(roomName.getText().length() > ROOM_NAME_MAX_LENGTH) {
                        displayAlert("", ROOM_NAME_TOO_LONG);
                    }
                    else {
                        for (int i = 0; i < input.getRoomsNames().size(); i++) {
                            if (input.getRoomsNames().get(i).equals(roomName.getText())) {
                                roomNameExists = true;
                                break;
                            }
                        }
                        if (roomNameExists) {
                            displayAlert(SAME_ROOM_NAME_TITLE, SAME_ROOM_NAME_MSG);
                        } else {
                            input.setRoomName(roomName.getText());
                            rmiQueue.put(() -> {
                                try {
                                    stub.handleCreateRoom(roomName.getText()); /* There once was a return here. */
                                } catch (RemoteException remoteException) {
                                    remoteException.printStackTrace();
                                }
                            });
                        }
                    }
                    dialog.close();
                });
            }
            else {
                displayAlert("", CREATE_ROOM_TEXT);
            }
        });

        /******************************** Login button event handler *******************************/

        login.setOnAction(e -> {
            Button confirm = new Button("Choose name");
            TextField nickname = new TextField();
            Label username = new Label("Username:");

            GridPane loginWindow = new GridPane();
            loginWindow.setPadding(new Insets(GRID_MENU_SPACING,GRID_MENU_SPACING,GRID_MENU_SPACING,ROOM_SPACING));
            loginWindow.setHgap(GRID_MENU_SPACING);
            loginWindow.setVgap(GRID_MENU_SPACING);

            // Arranging items on the window.
            GridPane.setConstraints(username, 0, 0);
            GridPane.setConstraints(nickname, 1, 0);
            GridPane.setConstraints(confirm,1,2);

            loginWindow.getChildren().addAll(username, nickname, confirm);

            Stage signIn = new Stage();
            signIn.setTitle("Nickname screen");
            signIn.initModality(Modality.NONE);
            signIn.initOwner(stage);
            Scene loginScene = new Scene(loginWindow, LOGIN_WINDOW_WIDTH, LOGIN_WINDOW_HEIGHT);
            signIn.setScene(loginScene);
            signIn.show();

            /* Checks for valid nickname. */
            confirm.setOnAction(eConfirm -> {
                if(nickname.getText().trim().length() == 0) {
                    displayAlert("", EMPTY_USERNAME_TEXT);
                }
                else if(nickname.getText().trim().length() > USERNAME_CHAR_LIM) {
                    displayAlert("", USERNAME_TOO_LONG);
                }
                else {
                    input.username = nickname.getText().trim();
                    signIn.close();
                    rmiQueue.put(() -> {
                        try {
                            stub.handleRequestUsername(input.username); /* There once was a return here. */
                        } catch (RemoteException remoteException) {
                            remoteException.printStackTrace();
                        }
                    });
                }
            });
        });


        /******************************** Dividing the lobby scene layout into sections and creating the scene ********************************/

        BorderPane lobbyLayout = new BorderPane();
        lobbyLayout.setTop(title);
        lobbyLayout.setLeft(lobbyLeftMenu);
        lobbyLayout.setRight(info);
        lobbyLayout.setCenter(mainLobby);
        lobbyLayout.setBottom(bottomM);
        lobby = new Scene(lobbyLayout, width, height);

    /******************************** Lobby scene - End ********************************/

    /********************************** Setting up socket ************************************/
        try {
            //TODO: Change to RMI communication (define the 'stub')
            socket = new Socket(Connection.DOMAIN, Connection.PORT);

            int rmiRegistryPort = 0;
            DataInputStream in = new DataInputStream(socket.getInputStream());
            try {
                rmiRegistryPort = in.readInt();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            out.writeBoolean(true);
            in.close();
            out.close();
            socket.close();

            // Get the stub from the server
            Registry registry = LocateRegistry.getRegistry(Connection.DOMAIN, rmiRegistryPort);
            this.stub = (IServerHandler) registry.lookup("IServerHandler");
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        /********************************** Server side ************************************/

        rmiQueue = new RMIHandler();
        Thread threadOutputHandler = new Thread(rmiQueue);
        threadOutputHandler.setDaemon(true);
        threadOutputHandler.start();

        /********************************** Client side ************************************/

        input = new InputHandler(this, lobbies, stage, lobby, rmiQueue, stub);

        // Send this stub to the server
        {
            IClientHandler clientStub = (IClientHandler) UnicastRemoteObject.exportObject(input, 0);
            this.stub.setClientStub(clientStub);
        }

    /******************************** Setting the stage ********************************/

        stage.setTitle("Lobby");
        stage.setScene(lobby);
        stage.sizeToScene();
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());

        rmiQueue.put(() -> {
            try {
                stub.handleGetRooms();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }


    /******************************** Functions ********************************/

    private void setLayoutWidth(Pane p, int width) {
        if(p == null) { return; }
        p.setStyle(CssLayouts.cssBorder);
        p.setMaxWidth(width);
        p.setMinWidth(width);
    }

    // Displays an alert message to the user.
    public void displayAlert(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    /* Used to read the drawing array from the database. */
    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    /* Handles what the program will do after the client's name has been acknowldged or not. */
    public void handleAckUsername(boolean ack) {
        Platform.runLater(() -> {
            if (ack) {
                isLoggedIn = true;
                Text helloMsg = new Text("Hello " + input.username);
                //user = new Text(username);
                helloMsg.setStyle(CssLayouts.cssBottomLayoutText);
                helloMsg.setWrappingWidth(TEXT_WRAPPING_WIDTH);
                bottomM.getChildren().add(helloMsg);
                login.setVisible(false);
                //            input.signIn.close();
            } else {
                input.username = null;
                displayAlert("Name exists", USERNAME_EXISTS);
            }
        });
    }

    //TODO: make the cursor look like a pen.
}


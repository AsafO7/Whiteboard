package whiteboard.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import whiteboard.Connection;
import whiteboard.Packet;

import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Board extends Application{

    private List<WhiteboardRoom> rooms = Collections.synchronizedList(new ArrayList<>());

    private List<String> roomsNames = new ArrayList<>();

    private GraphicsContext gc;

    private Scene whiteboard, lobby;

    private Button redo, undo, clear, backToLobby, exit;

    private Slider thickness;

    private ColorPicker colorChooser;

    private InputHandler input;

    private Text userList = new Text("Here will be displayed the online users in the room."),
            chat = new Text("Here will be the chat"),
            topM = new Text("Welcome to Whiteboard! Draw your minds out!"),
            user = new Text("AAA");

    final String pSTitle = "Password too short", pSText = "Password must be at least 6 characters long.",
            regisTitle = "Successfully registered", regisText = "You've been successfully registered and logged in.",
            signInTitle = "Successfully logged in", signInText = "You've been successfully logged in.",
            pWTitle = "Wrong password", pWText = "You've entered a wrong password, please try again.",
            enterLobbyText = "You must be logged in before entering a lobby", emptyUsernameText = "Username can't be blank.",
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

    private String DATABASE_URL = "jdbc:postgresql:", USERNAME, PASSWORD;

    private int numOfRooms = 0;

    private Socket socket;
    private BlockingQueue<Packet> outQueue = new LinkedBlockingQueue<Packet>();

    public static void main(String[] args) { launch(args); }

    @Override
    public void init() throws Exception {
        //TODO: write client side here(connecting to a server and what to do once connected)
    }

    @Override
    public void start(Stage stage) throws IOException /* throws Exception */ {

        java.util.List<String> args = getParameters().getRaw();
        if (args.size() != 3) {
            System.err.println("The program must be given 3 arguments: Database path, username and password");
            Platform.exit();
            return;
        }
        DATABASE_URL += args.get(0);
        USERNAME = args.get(1);
        PASSWORD = args.get(2);

        final int GRID_MENU_SPACING = 10, LEFT_MENU_WIDTH = 200, RIGHT_MENU_WIDTH = 300, TOP_MENU_HEIGHT = 60,
                BOTTOM_MENU_LEFT = 220, TOOLBAR_HEIGHT = 60, TEXT_WRAPPING_WIDTH = 125, DRAWING_TEXT_DIALOG_WINDOW_WIDTH = 300,
                DRAWING_TEXT_DIALOG_WINDOW_HEIGHT = 200, CHAT_MESSAGE_WRAPPING_WIDTH = 230, EXPLAIN_TEXT_WRAPPING_WIDTH = 280,
                ROOM_SPACING = 20, BOTTOM_MENU_LOBBY_LEFT = 420, BOTTOM_MENU_LOBBY_SPACING = 150, LOGIN_WINDOW_WIDTH = 250, LOGIN_WINDOW_HEIGHT = 120,
                MIN_LINE_THICKNESS = 1, MAX_LINE_THICKNESS = 15;

//        /********************************** Initializing objects ********************************/
//
//        shapeChooser.getItems().addAll(shapes); // Adding options to the scrollbar.
//
//        Label shapeLabel = new Label("Shape:");
//        Label thicknessLabel = new Label("Thickness:");
//        shapeLabel.setStyle(CssLayouts.cssBottomLayoutText);
//        thicknessLabel.setStyle(CssLayouts.cssBottomLayoutText);
//
//        colorChooser = new ColorPicker();
//        colorChooser.setValue(color);
//        thickness = new Slider();
//        thickness.setMin(MIN_LINE_THICKNESS);
//        thickness.setMax(MAX_LINE_THICKNESS);
//        thickness.setShowTickMarks(true);
//        redo = new Button("Redo");
//        undo = new Button("Undo");
//        clear = new Button("Clear");
//        backToLobby = new Button("Back to Lobby");
//        exit = new Button("Exit");
//
//        CheckBox fillShape = new CheckBox("Fill Shape");
//        fillShape.setStyle(CssLayouts.cssBottomLayoutText);
//
//        // This line prevents text overflow.
//        userList.setWrappingWidth(TEXT_WRAPPING_WIDTH);
//
//        /******************************** Whiteboard scene ********************************/
//
//        /********************************** Building the bottom menu ********************************/
//
//        GridPane bottomMenu = new GridPane();
//        bottomMenu.setStyle(CssLayouts.cssBottomLayout);
//        bottomMenu.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, BOTTOM_MENU_LEFT));
//        bottomMenu.setVgap(GRID_MENU_SPACING);
//        bottomMenu.setHgap(GRID_MENU_SPACING);
//
//        // Adding buttons to the bottom menu.
//        Object[] bottomMenuItems = { shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness, fillShape, redo, undo, clear, backToLobby, exit};
//        Button[] bottomMenuButtons = { redo, undo, clear, backToLobby, exit };
//
//        // Arranging them in a line.
//        for(int i = 0; i < bottomMenuItems.length; i++) { GridPane.setConstraints((Node) bottomMenuItems[i], i, 0); }
//        bottomMenu.getChildren().addAll(shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness, fillShape, redo, undo, clear, backToLobby, exit);
//
//        shapeChooser.setValue(shapes[0]);
//
//        /********************************** Building the left menu - online users list ********************************/
//
//        // Online users in the room.
//        VBox leftMenu = new VBox();
//        //leftMenu.getChildren().add(userList);
//        //setLayoutWidth(leftMenu, LEFT_MENU_WIDTH);
//        leftMenu.setMaxWidth(LEFT_MENU_WIDTH);
//        leftMenu.setMinWidth(LEFT_MENU_WIDTH);
//
//        /********************************** Building the right menu - chat box ********************************/
//
//        TextField chatMsg = new TextField(); // The user will write messages in here.
//        VBox chatMsgWrapper = new VBox(), pane = new VBox();
//        ScrollPane chatWrapper = new ScrollPane();
//        chatWrapper.setContent(pane);
//        chatWrapper.setStyle("-fx-background-color: white");
//        chatMsgWrapper.setStyle(CssLayouts.cssBorder);
//        chatMsgWrapper.getChildren().add(chatMsg); // Container for the textfield.
//        BorderPane rightMenu = new BorderPane(); // Right menu wrapper.
//        rightMenu.setBottom(chatMsgWrapper);
//        rightMenu.setCenter(chatWrapper);
//        setLayoutWidth(rightMenu, RIGHT_MENU_WIDTH);
//
//        // Event handler for sending a message.
//        chatMsg.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
//            if(e.getCode() == KeyCode.ENTER) {
//                // Case of empty message.
//                if(chatMsg.getText().trim().length() == 0) { return; }
//                //TODO: display the message on chat screen then clear the text field.
//                //TODO: Make it so the message won't slide off screen(in the text box).
//                Text msg = new Text(user.getText() + ": " + chatMsg.getText());
//                msg.setStyle(CssLayouts.cssChatText);
//                msg.setWrappingWidth(CHAT_MESSAGE_WRAPPING_WIDTH);
//                pane.getChildren().add(msg);
//                chatWrapper.setVvalue(1.0); // Makes the scrollpane scroll to the bottom automatically.
//                chatMsg.clear();
//            }
//        });
//
//        /********************************** Building the top menu - The title ********************************/
//
//        HBox topMenu = new HBox();
//        topMenu.getChildren().add(topM);
//        topM.setFill(Color.GREEN);
//        topMenu.setStyle(CssLayouts.cssTopLayout);
//        topMenu.setMinHeight(TOP_MENU_HEIGHT);
//
//        /********************************** Building the canvas on which the user will draw ********************************/
//
//        HBox center = new HBox();
//        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
//        center.setStyle(CssLayouts.cssBorder);
//        center.getChildren().add(canvas);
//        gc = canvas.getGraphicsContext2D();
//
//        /********************************** Color and fill shape functionality ********************************/
//
//        /* Picking a new color */
//        colorChooser.setOnAction(e -> {
//            color = colorChooser.getValue();
//        });
//
//        /* Should the shape be filled? */
//        fillShape.setOnAction(e -> toFill = !toFill);
//
//        /********************************** Bottom menu events handler ********************************/
//
//        for (Button bottomMenuButton : bottomMenuButtons) {
//            bottomMenuButton.setOnAction(e -> {
//
//                /* redo button functionality. */
//                if(e.getSource() == redo) {
//                    /* Redrawing a drawable. */
//                    if (deletedDraws.isEmpty()) { return; }
//                    myDraws.add(deletedDraws.pop());
//                    repaint();
//                }
//
//                /* undo button functionality. */
//                if (e.getSource() == undo) {
//                    /* Deleting a drawable. */
//                    if (myDraws.isEmpty()) { return; }
//                    deletedDraws.add(myDraws.pop());
//                    repaint();
//                }
//
//                /* clear button functionality. */
//                if (e.getSource() == clear) {
//                    myDraws.clear();
//                    deletedDraws.clear();
//                    gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT);
//                }
//
//                /* backToLobby button functionality. */
//                if(e.getSource() == backToLobby) {
//                    /* Maybe switch to a scene that takes care of lobby window */
//                    leftMenu.getChildren().remove(user);
//                    stage.setScene(lobby);
//                    stage.setTitle("Lobby");
//                }
//
//                /* exit button functionality. */
//                if(e.getSource() == exit) { Platform.exit(); }
//            });
//        }
//
//        /********************************** Choosing shapes event handler ********************************/
//
//        canvas.setOnMousePressed(e -> {
//            if(shapeChooser.getValue() == null) { return; }
//
//            TextInputDialog arcH = new TextInputDialog(), arcW = new TextInputDialog();
//            /* Creating the shape to be drawn next. */
//            switch(shapeChooser.getValue()) {
//                case "Line":
//                    myDraws.add(new MyLine(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue()));
//                    isRoundRectChosen = false;
//                    break;
//                case "Oval":
//                    myDraws.add(new MyOval(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue(), toFill));
//                    isRoundRectChosen = false;
//                    break;
//                case "Rectangle":
//                    myDraws.add(new MyRect(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue(), toFill));
//                    isRoundRectChosen = false;
//                    break;
//                case "Rounded Rectangle":
//                    /* A flag so we won't get repeated dialog. */
//                    if(!isRoundRectChosen) {
//                        /* Add input box so the user will choose the arc width and arc height */
//                        arcW.setHeaderText("Please choose the arc width of the rectangle (Integer only).");
//                        arcW.show();
//                        arcH.setHeaderText("Please choose the arc width of the rectangle (Integer only).");
//                        arcH.show();
//                    }
//                    /* The user will have to draw another shape in order to change the values of the arcs. */
//                    else {
//                        int arcWidth = isNumeric(arcW.getContentText()), arcHeight = isNumeric(arcH.getContentText());
//                        myDraws.add(new MyRoundRect(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue(), toFill,arcWidth,arcHeight));
//                    }
//                    isRoundRectChosen = true;
//                    break;
//                case "Brush": // Free drawing.
//                    myDraws.add(new MyBrush(e.getX(), e.getY(), color, thickness.getValue(), toFill));
//                    break;
//                case "Text": // Displaying text on the canvas.
//                    TextArea text = new TextArea();
//                    Button btn = new Button("OK");
//
//                    // Dialog window wrapper.
//                    BorderPane textBox = new BorderPane();
//                    textBox.setCenter(text);
//                    textBox.setBottom(btn);
//                    BorderPane.setAlignment(btn, Pos.BOTTOM_CENTER);
//
//                    // Dialog window.
//                    final Stage dialog = new Stage();
//                    dialog.initModality(Modality.NONE);
//                    dialog.initOwner(stage);
//                    Scene dialogScene = new Scene(textBox, DRAWING_TEXT_DIALOG_WINDOW_WIDTH, DRAWING_TEXT_DIALOG_WINDOW_HEIGHT);
//                    dialog.setScene(dialogScene);
//                    dialog.show();
//
//                    // Button functionality - Displaying the text on the canvas.
//                    btn.setOnAction(eBtn -> {
//                        TextBox t = new TextBox(e.getX(), e.getY(), color, text.getText());
//                        myDraws.add(t);
//                        t.Draw(gc);
//                        dialog.close();
//                    });
//            }
//        });
//
//        /********************************** Drawing shapes event handler ********************************/
//
//        canvas.setOnMouseDragged(e -> {
//            if(shapeChooser.getValue() == null) { return; }
//
//            MyDraw drawable = null;
//            if(!myDraws.isEmpty()) { drawable = myDraws.peek(); }
//            MyLine line;
//            MyRect rect;
//            MyOval oval;
//            MyRoundRect rRect;
//            MyBrush brush;
//            /* Draw the shape chosen in the ComboBox list. */
//            switch(shapeChooser.getValue()) {
//                case "Line":
//                    line = (MyLine)drawable;
//                    line.setX2(e.getX());
//                    line.setY2(e.getY());
//                    break;
//                case "Oval":
//                    oval = (MyOval)drawable;
//                    oval.setWidth(e.getX() - oval.getX1());
//                    oval.setHeight(e.getY() - oval.getY1());
//                    break;
//                case "Rectangle":
//                    rect = (MyRect)drawable;
//                    rect.setWidth(e.getX() - rect.getX1());
//                    rect.setHeight(e.getY() - rect.getY1());
//                    break;
//                case "Rounded Rectangle":
//                    rRect = (MyRoundRect)drawable;
//                    rRect.setWidth(e.getX() - rRect.getX1());
//                    rRect.setHeight(e.getY() - rRect.getY1());
//                    break;
//                case "Brush":
//                    brush = (MyBrush)drawable;
//                    brush.addPoint(e.getX(), e.getY());
//                    break;
//            }
//            repaint();
//            deletedDraws.clear();
//        });
//
//        //TODO: search for paint brush icons. maybe put the picture in a .jar file.
//        //Maybe improve it.
////        Image cursor = new Image("file:\\C:\\Users\\Asaf\\Desktop\\paint-brush-icon-9032.png");
////        canvas.setOnMouseEntered(e -> {
////            canvas.setCursor(new ImageCursor(cursor));
////        });
//
//        /******************************** Dividing the board scene layout into sections and creating the scene ********************************/
//
//        BorderPane whiteboardLayout = new BorderPane();
//        whiteboardLayout.setBottom(bottomMenu);
//        whiteboardLayout.setLeft(leftMenu);
//        whiteboardLayout.setTop(topMenu);
//        whiteboardLayout.setRight(rightMenu);
//        whiteboardLayout.setCenter(center);
//
//        // This code should make the window adaptable to all screen sizes.
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight() - TOOLBAR_HEIGHT;
//
//        whiteboard = new Scene(whiteboardLayout, width, height);

        /******************************** Whiteboard scene - End ********************************/



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
//        Button b = new Button("Click to draw");
//        b.setOnAction(e -> {
//            if(isLoggedIn) {
//                //TODO: Style the text. Figure out how to change the fucking color.
////                leftMenu.setStyle(CssLayouts.cssLeftMenu + ";\n-fx-padding: 3 0 0 10;");
////                leftMenu.getChildren().add(user);
//                //WhiteboardRoom n = new WhiteboardRoom();
//                //stage.setScene(n.showBoard(stage, user, lobby));
//                stage.setScene(rooms.get(numOfRooms - 1).showBoard(stage, user, lobby)); //whiteboard);
//                stage.setTitle("Whiteboard");
//            }
//            else { displayAlert("", enterLobbyText); }
//        });

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

        Button createR = new Button("Create room"), login = new Button("Log in"), refreshRooms = new Button("Refresh rooms");
        HBox bottomM = new HBox();
        bottomM.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING));
        bottomM.setSpacing(BOTTOM_MENU_LOBBY_SPACING);
        bottomM.getChildren().addAll(createR, refreshRooms, login);
        bottomM.setAlignment(Pos.CENTER);
        bottomM.setStyle(CssLayouts.cssBottomLayout);

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

                // Dialog window wrapper.
                BorderPane textBox = new BorderPane();
                textBox.setCenter(roomName);
                textBox.setBottom(btn);
                BorderPane.setAlignment(btn, Pos.BOTTOM_CENTER);

                // Dialog window.
                final Stage dialog = new Stage();
                dialog.initModality(Modality.NONE);
                dialog.initOwner(stage);
                Scene dialogScene = new Scene(textBox, 200, 90);
                dialog.setScene(dialogScene);
                dialog.show();

                btn.setOnAction(eBtn -> {
//                    //TODO: Different rooms still have the same drawing stack, so change it.
//                    rooms.add(new WhiteboardRoom(roomName.getText()));
//                    numOfRooms++;
//                    HBox room = new HBox();
//                    room.setStyle(CssLayouts.cssBorder);
//                    room.getChildren().add(new Text(roomName.getText()));
//                    //TODO: Style to rooms differently.
//                    lobbies.getChildren().add(room);
//                    lobbies.setAlignment(Pos.CENTER);
//                    room.setOnMouseClicked(eRoom -> {
//                        stage.setScene(rooms.get(numOfRooms - 1).showBoard(stage, user, lobby));
//                    });
//                    room.setOnMouseEntered(eRoomMouse -> { lobby.setCursor(Cursor.HAND); });
//                    room.setOnMouseExited(eRoomMouse2 -> { lobby.setCursor(Cursor.DEFAULT); });
                    //TODO: send a string to the server.
                    if(roomName.getText().trim().length() == 0) {
                        displayAlert(BLANK_ROOM_TITLE, BLANK_ROOM_MSG);
                    }
                    //TODO: this is no good. talk to the server to check if this room really exists.
//                    else if(!roomsNames.isEmpty() && roomsNames.contains(roomName.getText())) {
//                        displayAlert(SAME_ROOM_NAME_TITLE, SAME_ROOM_NAME_MSG);
//                    }
                    else {
                        try {
                            input.setRoomName(roomName.getText());
                            outQueue.put(Packet.createRoom(roomName.getText()));
                            //roomsNames.add(roomName.getText());
                            //input.setHost(user.getText());
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                    }
                    dialog.close();
                });

           // else { displayAlert("", enterLobbyText); }
        });

        /******************************** Login button event handler *******************************/

//        login.setOnAction(e -> {
//            if(isLoggedIn) return;
//
//            Button confirm = new Button("Login/Register");
//            TextField nickname = new TextField();
//            PasswordField password = new PasswordField();
//            Label username = new Label("Username:");
//            Label pass = new Label("Password");
//
//            GridPane loginWindow = new GridPane();
//            loginWindow.setPadding(new Insets(GRID_MENU_SPACING,GRID_MENU_SPACING,GRID_MENU_SPACING,ROOM_SPACING));
//            loginWindow.setHgap(GRID_MENU_SPACING);
//            loginWindow.setVgap(GRID_MENU_SPACING);
//
//            // Arranging items on the window.
//            GridPane.setConstraints(username, 0, 0);
//            GridPane.setConstraints(nickname, 1, 0);
//            GridPane.setConstraints(pass, 0, 1);
//            GridPane.setConstraints(password, 1, 1);
//            GridPane.setConstraints(confirm,1,2);
//
//            loginWindow.getChildren().addAll(username, nickname, pass, password, confirm);
//
//            Stage signIn = new Stage();
//            signIn.setTitle("Login screen");
//            signIn.initModality(Modality.NONE);
//            signIn.initOwner(stage);
//            Scene loginScene = new Scene(loginWindow, LOGIN_WINDOW_WIDTH, LOGIN_WINDOW_HEIGHT);
//            signIn.setScene(loginScene);
//            signIn.show();
//
//            confirm.setOnAction(e1 -> {
//                if(nickname.getText().trim().length() == 0) {
//                    displayAlert("", emptyUsernameText);
//                }
//                // Password must have at least 6 characters.
//                else if(password.getText().length() < 6) {
//                    displayAlert(pSTitle, pSText);
//                }
//                else {
//                    //TODO: send a packet with string to the server for identification.
//                    //connectToDatabase(nickname.getText(), password.getText());
//                    if(isLoggedIn) {
//                        Text helloMsg = new Text("Hello " + nickname.getText());
//                        helloMsg.setStyle(CssLayouts.cssBottomLayoutText);
//                        bottomM.getChildren().add(helloMsg);
//                        signIn.close();
//                        login.setVisible(false);
//                        try {
//                            outQueue.put(Packet.requestRoomsNames());
//                        } catch (InterruptedException interruptedException) {
//                            interruptedException.printStackTrace();
//                        }
//                    }
//                }
//            });
//        });

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

    /* Repaints the canvas */
    private void repaint() {
        gc.clearRect(0, 0, CANVAS_WIDTH,CANVAS_HEIGHT);
        for (MyDraw myDraw : myDraws) { myDraw.Draw(gc); }
    }

    // To prevent repeated code.
    private void setLayoutWidth(Pane p, int width) {
        if(p == null) { return; }
        p.setStyle(CssLayouts.cssBorder);
        p.setMaxWidth(width);
        p.setMinWidth(width);
    }

    /* If str is a number, returns that number. Else returns default value.
       Being used to bypass the user entering invalid input to the arcWidth and arcHeight properties. */
    public int isNumeric(String str) {
        try { return Integer.parseInt(str); }
        catch (NumberFormatException e) { return DEFAULT_ARC_VALUE; }
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

    //TODO: Idea: keep the information about rooms in the database and change scenes acording to that.
    //MAKE A CLASS TO CALL BOARD

    //TODO: check why Board is still running after pressing the standard X.
    //TODO: maybe make a close room button.
    //TODO: have the server take care of database requests.
    //TODO: make the cursor look like a pen.
    //TODO: maybe add background image to the other menus.
    //TODO: make users who exited, by clicking 'X' on the window, disappear from the users list in the server
}


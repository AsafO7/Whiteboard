package whiteboard.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import whiteboard.Packet;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import java.awt.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Board extends Application{

    private GraphicsContext gc;

    private Scene whiteboard, lobby;

    private Button redo, undo, clear, backToLobby, exit;

    private Slider thickness;

    private ColorPicker colorChooser;

    private Text userList = new Text("Here will be displayed the online users in the room."),
            chat = new Text("Here will be the chat"),
            topM = new Text("Welcome to Whiteboard! Draw your minds out!"),
            user;

    final String pSTitle = "Password too short", pSText = "Password must be at least 6 characters long.",
            regisTitle = "Successfully registered", regisText = "You've been successfully registered and logged in.",
            signInTitle = "Successfully logged in", signInText = "You've been successfully logged in.",
            pWTitle = "Wrong password", pWText = "You've entered a wrong password, please try again.",
            enterLobbyText = "You must be logged in before entering a lobby", emptyUsernameText = "Username can't be blank.",
            programExplained = "This program is a whiteboard.\nThe whiteboard is a canvas where multiple users can draw on at the same time.\n" +
                    "First the user logs in\\registers in the database of the program, and then chooses to enter an existing lobby or create " +
                    "one of their own.\nFrom there the user enters the whiteboard where they can draw freely on the board or add shapes and text" +
                    " to it!.\nA user is also able to see other online users in the same lobby at the left side of the program.";

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
                ROOM_SPACING = 20, BOTTOM_MENU_LOBBY_LEFT = 420, BOTTOM_MENU_LOBBY_SPACING = 350, LOGIN_WINDOW_WIDTH = 250, LOGIN_WINDOW_HEIGHT = 120,
                MIN_LINE_THICKNESS = 1, MAX_LINE_THICKNESS = 15;

        /********************************** Initializing objects ********************************/

        shapeChooser.getItems().addAll(shapes); // Adding options to the scrollbar.

        Label shapeLabel = new Label("Shape:");
        Label thicknessLabel = new Label("Thickness:");
        shapeLabel.setStyle(CssLayouts.cssBottomLayoutText);
        thicknessLabel.setStyle(CssLayouts.cssBottomLayoutText);

        colorChooser = new ColorPicker();
        colorChooser.setValue(color);
        thickness = new Slider();
        thickness.setMin(MIN_LINE_THICKNESS);
        thickness.setMax(MAX_LINE_THICKNESS);
        thickness.setShowTickMarks(true);
        redo = new Button("Redo");
        undo = new Button("Undo");
        clear = new Button("Clear");
        backToLobby = new Button("Back to Lobby");
        exit = new Button("Exit");

        CheckBox fillShape = new CheckBox("Fill Shape");
        fillShape.setStyle(CssLayouts.cssBottomLayoutText);

        // This line prevents text overflow.
        userList.setWrappingWidth(TEXT_WRAPPING_WIDTH);

    /******************************** Whiteboard scene ********************************/

        /********************************** Building the bottom menu ********************************/

        GridPane bottomMenu = new GridPane();
        bottomMenu.setStyle(CssLayouts.cssBottomLayout);
        bottomMenu.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, BOTTOM_MENU_LEFT));
        bottomMenu.setVgap(GRID_MENU_SPACING);
        bottomMenu.setHgap(GRID_MENU_SPACING);

        // Adding buttons to the bottom menu.
        Object[] bottomMenuItems = { shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness, fillShape, redo, undo, clear, backToLobby, exit};
        Button[] bottomMenuButtons = { redo, undo, clear, backToLobby, exit };

        // Arranging them in a line.
        for(int i = 0; i < bottomMenuItems.length; i++) { GridPane.setConstraints((Node) bottomMenuItems[i], i, 0); }
        bottomMenu.getChildren().addAll(shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness, fillShape, redo, undo, clear, backToLobby, exit);

        shapeChooser.setValue(shapes[0]);

        /********************************** Building the left menu - online users list ********************************/

        // Online users in the room.
        VBox leftMenu = new VBox();
        //leftMenu.getChildren().add(userList);
        //setLayoutWidth(leftMenu, LEFT_MENU_WIDTH);
        leftMenu.setMaxWidth(LEFT_MENU_WIDTH);
        leftMenu.setMinWidth(LEFT_MENU_WIDTH);

        /********************************** Building the right menu - chat box ********************************/

        TextField chatMsg = new TextField(); // The user will write messages in here.
        VBox chatMsgWrapper = new VBox(), pane = new VBox();
        ScrollPane chatWrapper = new ScrollPane();
        chatWrapper.setContent(pane);
        chatWrapper.setStyle("-fx-background-color: white");
        chatMsgWrapper.setStyle(CssLayouts.cssBorder);
        chatMsgWrapper.getChildren().add(chatMsg); // Container for the textfield.
        BorderPane rightMenu = new BorderPane(); // Right menu wrapper.
        rightMenu.setBottom(chatMsgWrapper);
        rightMenu.setCenter(chatWrapper);
        setLayoutWidth(rightMenu, RIGHT_MENU_WIDTH);

        // Event handler for sending a message.
        chatMsg.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if(e.getCode() == KeyCode.ENTER) {
                // Case of empty message.
                if(chatMsg.getText().trim().length() == 0) { return; }
                //TODO: display the message on chat screen then clear the text field.
                //TODO: Make it so the message won't slide off screen(in the text box).
                Text msg = new Text(user.getText() + ": " + chatMsg.getText());
                msg.setStyle(CssLayouts.cssChatText);
                msg.setWrappingWidth(CHAT_MESSAGE_WRAPPING_WIDTH);
                pane.getChildren().add(msg);
                chatWrapper.setVvalue(1.0); // Makes the scrollpane scroll to the bottom automatically.
                chatMsg.clear();
            }
        });

        /********************************** Building the top menu - The title ********************************/

        HBox topMenu = new HBox();
        topMenu.getChildren().add(topM);
        topM.setFill(Color.GREEN);
        topMenu.setStyle(CssLayouts.cssTopLayout);
        topMenu.setMinHeight(TOP_MENU_HEIGHT);

        /********************************** Building the canvas on which the user will draw ********************************/

        HBox center = new HBox();
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        center.setStyle(CssLayouts.cssBorder);
        center.getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();

        /********************************** Color and fill shape functionality ********************************/

        /* Picking a new color */
        colorChooser.setOnAction(e -> {
            color = colorChooser.getValue();
            //gc.setStroke(color);
        });

        /* Should the shape be filled? */
        fillShape.setOnAction(e -> toFill = !toFill);

        /********************************** Bottom menu events handler ********************************/

        for (Button bottomMenuButton : bottomMenuButtons) {
            bottomMenuButton.setOnAction(e -> {

                /* redo button functionality. */
                if(e.getSource() == redo) {
                    /* Redrawing a drawable. */
                    if (deletedDraws.isEmpty()) { return; }
                    myDraws.add(deletedDraws.pop());
                    repaint();
                }

                /* undo button functionality. */
                if (e.getSource() == undo) {
                    /* Deleting a drawable. */
                    if (myDraws.isEmpty()) { return; }
                    deletedDraws.add(myDraws.pop());
                    repaint();
                }

                /* clear button functionality. */
                if (e.getSource() == clear) {
                    myDraws.clear();
                    deletedDraws.clear();
                    gc.clearRect(0,0,CANVAS_WIDTH,CANVAS_HEIGHT);
                }

                /* backToLobby button functionality. */
                if(e.getSource() == backToLobby) {
                    /* Maybe switch to a scene that takes care of lobby window */
                    leftMenu.getChildren().remove(user);
                    stage.setScene(lobby);
                    stage.setTitle("Lobby");
                }

                /* exit button functionality. */
                if(e.getSource() == exit) { Platform.exit(); }
            });
        }

        /********************************** Choosing shapes event handler ********************************/

        canvas.setOnMousePressed(e -> {
            if(shapeChooser.getValue() == null) { return; }

            TextInputDialog arcH = new TextInputDialog(), arcW = new TextInputDialog();
            /* Creating the shape to be drawn next. */
            switch(shapeChooser.getValue()) {
                case "Line":
                    myDraws.add(new MyLine(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue()));
                    isRoundRectChosen = false;
                    break;
                case "Oval":
                    myDraws.add(new MyOval(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue(), toFill));
                    isRoundRectChosen = false;
                    break;
                case "Rectangle":
                    myDraws.add(new MyRect(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue(), toFill));
                    isRoundRectChosen = false;
                    break;
                case "Rounded Rectangle":
                    /* A flag so we won't get repeated dialog. */
                    if(!isRoundRectChosen) {
                        /* Add input box so the user will choose the arc width and arc height */
                        arcW.setHeaderText("Please choose the arc width of the rectangle (Integer only).");
                        arcW.show();
                        arcH.setHeaderText("Please choose the arc width of the rectangle (Integer only).");
                        arcH.show();
                    }
				/* The user will have to draw another shape in order to change the values of the arcs. */
                    else {
                        int arcWidth = isNumeric(arcW.getContentText()), arcHeight = isNumeric(arcH.getContentText());
                        myDraws.add(new MyRoundRect(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue(), toFill,arcWidth,arcHeight));
                    }
                    isRoundRectChosen = true;
                    break;
                case "Brush": // Free drawing.
                    myDraws.add(new MyBrush(e.getX(), e.getY(), color, thickness.getValue(), toFill));
                    break;
                case "Text": // Displaying text on the canvas.
                    TextArea text = new TextArea();
                    Button btn = new Button("OK");

                    // Dialog window wrapper.
                    BorderPane textBox = new BorderPane();
                    textBox.setCenter(text);
                    textBox.setBottom(btn);
                    BorderPane.setAlignment(btn, Pos.BOTTOM_CENTER);

                    // Dialog window.
                    final Stage dialog = new Stage();
                    dialog.initModality(Modality.NONE);
                    dialog.initOwner(stage);
                    Scene dialogScene = new Scene(textBox, DRAWING_TEXT_DIALOG_WINDOW_WIDTH, DRAWING_TEXT_DIALOG_WINDOW_HEIGHT);
                    dialog.setScene(dialogScene);
                    dialog.show();

                    // Button functionality - Displaying the text on the canvas.
                    btn.setOnAction(eBtn -> {
                        TextBox t = new TextBox(e.getX(), e.getY(), color, text.getText());
                        myDraws.add(t);
                        t.Draw(gc);
                        dialog.close();
                    });
                }
            });

        /********************************** Drawing shapes event handler ********************************/

        canvas.setOnMouseDragged(e -> {
            if(shapeChooser.getValue() == null) { return; }

            MyDraw drawable = null;
            if(!myDraws.isEmpty()) { drawable = myDraws.peek(); }
            MyLine line;
            MyRect rect;
            MyOval oval;
            MyRoundRect rRect;
            MyBrush brush;
            /* Draw the shape chosen in the ComboBox list. */
            switch(shapeChooser.getValue()) {
                case "Line":
                    line = (MyLine)drawable;
                    line.setX2(e.getX());
                    line.setY2(e.getY());
                    break;
                case "Oval":
                    oval = (MyOval)drawable;
                    oval.setWidth(e.getX() - oval.getX1());
                    oval.setHeight(e.getY() - oval.getY1());
                    break;
                case "Rectangle":
                    rect = (MyRect)drawable;
                    rect.setWidth(e.getX() - rect.getX1());
                    rect.setHeight(e.getY() - rect.getY1());
                    break;
                case "Rounded Rectangle":
                    rRect = (MyRoundRect)drawable;
                    rRect.setWidth(e.getX() - rRect.getX1());
                    rRect.setHeight(e.getY() - rRect.getY1());
                    break;
                case "Brush":
                    brush = (MyBrush)drawable;
                    brush.addPoint(e.getX(), e.getY());
                    break;
            }
            repaint();
            deletedDraws.clear();
        });

        //TODO: search for paint brush icons. maybe put the picture in a .jar file.
        //Maybe improve it.
//        Image cursor = new Image("file:\\C:\\Users\\Asaf\\Desktop\\paint-brush-icon-9032.png");
//        canvas.setOnMouseEntered(e -> {
//            canvas.setCursor(new ImageCursor(cursor));
//        });

        /******************************** Dividing the board scene layout into sections and creating the scene ********************************/

        BorderPane whiteboardLayout = new BorderPane();
        whiteboardLayout.setBottom(bottomMenu);
        whiteboardLayout.setLeft(leftMenu);
        whiteboardLayout.setTop(topMenu);
        whiteboardLayout.setRight(rightMenu);
        whiteboardLayout.setCenter(center);

        // This code should make the window adaptable to all screen sizes.
        GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        int width = gd.getDisplayMode().getWidth();
        int height = gd.getDisplayMode().getHeight() - TOOLBAR_HEIGHT;

        whiteboard = new Scene(whiteboardLayout, width, height);

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

        VBox lobbyLeftMenu = new VBox();
        lobbyLeftMenu.setStyle(CssLayouts.cssBorder);
        lobbyLeftMenu.setMaxWidth(LEFT_MENU_WIDTH);
        lobbyLeftMenu.setMinWidth(LEFT_MENU_WIDTH);
//        Image brush = new Image("paint-1266212_640.png");
//        ImageView leftMenuImage = new ImageView(brush);
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

        //TODO: Make this code cleaner after coding lobby functionality.

        HBox l1 = new HBox(), l2 = new HBox(), l3 = new HBox();
        setLayoutWidth(l1, RIGHT_MENU_WIDTH);
        setLayoutWidth(l2, RIGHT_MENU_WIDTH);
        setLayoutWidth(l3, RIGHT_MENU_WIDTH);
        Button b = new Button("Click to draw");
        b.setOnAction(e -> {
            if(isLoggedIn) {
                //TODO: Style the text. Figure out how to change the fucking color.
                leftMenu.setStyle(CssLayouts.cssLeftMenu + ";\n-fx-padding: 3 0 0 10;");
                leftMenu.getChildren().add(user);
                stage.setScene(whiteboard);
                stage.setTitle("Whiteboard");
            }
            else { displayAlert("", enterLobbyText); }
        });

        ScrollPane lobbyCenter = new ScrollPane();
        VBox lobbies = new VBox();
        lobbyCenter.setContent(lobbies);
        lobbies.getChildren().add(b);
        lobbies.setAlignment(Pos.CENTER);


        //lobbyCenter.setPadding(new Insets(GRID_MENU_SPACING,GRID_MENU_SPACING,GRID_MENU_SPACING,ROOM_SPACING));
        //lobbyCenter.setHgap(GRID_MENU_SPACING);
        //lobbyCenter.setVgap(GRID_MENU_SPACING);

//        GridPane.setConstraints(l1, 0, 0);
//        GridPane.setConstraints(l2, 0, 1);
//        GridPane.setConstraints(l3, 0,2);
//        GridPane.setConstraints(b, 0, 3);

        //lobbyCenter.getChildren().addAll(l1, l2, l3, b);
        //lobbyCenter.setAlignment(Pos.CENTER);
        lobbies.setStyle(CssLayouts.cssBorder);

        //TODO: add event listener to lobbyCenter that when an event occurs the room list will be displayed.
        //TODO: program the communication between Board to Input to Server.

        /******************************** Bottom menu ********************************/

        Button createR = new Button("Create room");
        Button login = new Button("Log in");
        HBox bottomM = new HBox();
        bottomM.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, BOTTOM_MENU_LOBBY_LEFT));
        bottomM.setSpacing(BOTTOM_MENU_LOBBY_SPACING);
        bottomM.getChildren().addAll(createR, login);
        bottomM.setStyle(CssLayouts.cssBottomLayout);

        /******************************** Create room button event handler *******************************/

        createR.setOnAction(e -> {
            //TODO:
            /* Make sure the user is logged in before creating a room. */
            /* Make the room creator a host and transfer him to the new room. */
            TextInputDialog roomName = new TextInputDialog();
            roomName.setHeaderText("Enter room name");
            roomName.setTitle("Create new room");
            roomName.show();
            try {
                Socket socket = new Socket("localhost", 5555);
                ObjectOutputStream out = null;
                out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(roomName.getContentText());
            }
            catch (Exception exception) { exception.printStackTrace(); }
        });

        /******************************** Login button event handler *******************************/

        login.setOnAction(e -> {
            if(isLoggedIn) return;

            Button confirm = new Button("Login/Register");
            TextField nickname = new TextField();
            PasswordField password = new PasswordField();
            Label username = new Label("Username:");
            Label pass = new Label("Password");

            GridPane loginWindow = new GridPane();
            loginWindow.setPadding(new Insets(GRID_MENU_SPACING,GRID_MENU_SPACING,GRID_MENU_SPACING,ROOM_SPACING));
            loginWindow.setHgap(GRID_MENU_SPACING);
            loginWindow.setVgap(GRID_MENU_SPACING);

            // Arranging items on the window.
            GridPane.setConstraints(username, 0, 0);
            GridPane.setConstraints(nickname, 1, 0);
            GridPane.setConstraints(pass, 0, 1);
            GridPane.setConstraints(password, 1, 1);
            GridPane.setConstraints(confirm,1,2);

            loginWindow.getChildren().addAll(username, nickname, pass, password, confirm);

            Stage signIn = new Stage();
            signIn.setTitle("Login screen");
            signIn.initModality(Modality.NONE);
            signIn.initOwner(stage);
            Scene loginScene = new Scene(loginWindow, LOGIN_WINDOW_WIDTH, LOGIN_WINDOW_HEIGHT);
            signIn.setScene(loginScene);
            signIn.show();

            confirm.setOnAction(e1 -> {
                if(nickname.getText().trim().length() == 0) {
                    displayAlert("", emptyUsernameText);
                }
                // Password must have at least 6 characters.
                else if(password.getText().length() < 6) {
                    displayAlert(pSTitle, pSText);
                }
                else {
                    connectToDatabase(nickname.getText(), password.getText());
                    if(isLoggedIn) {
                        Text helloMsg = new Text("Hello " + nickname.getText());
                        helloMsg.setStyle(CssLayouts.cssBottomLayoutText);
                        bottomM.getChildren().add(helloMsg);
                        signIn.close();
                        login.setVisible(false);
                    }
                }
            });
        });

        /******************************** Dividing the lobby scene layout into sections and creating the scene ********************************/

        BorderPane lobbyLayout = new BorderPane();
        lobbyLayout.setTop(title);
        lobbyLayout.setLeft(lobbyLeftMenu);
        lobbyLayout.setRight(info);
        lobbyLayout.setCenter(lobbies);
        lobbyLayout.setBottom(bottomM);
        lobby = new Scene(lobbyLayout, width, height);

    /******************************** Lobby scene - End ********************************/

    /********************************** Client side ************************************/

//    InputHandler input = new InputHandler(lobbies);
//    Thread thread = new Thread(input);
//    thread.start();

    /******************************** Setting the stage ********************************/

        stage.setTitle("Lobby");
        stage.setScene(lobby);
        stage.sizeToScene();
        stage.show();
        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight());

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

    private void connectToDatabase(String username, String password) {

        // connect to database books and query database.
        // RowSetProvider class implements RowSetFactory which can be used to create various types of RowSets.
        try (JdbcRowSet rowSet = RowSetProvider.newFactory().createJdbcRowSet()) {
            // specify JdbcRowSet properties
            rowSet.setUrl(DATABASE_URL);
            rowSet.setUsername(USERNAME);
            rowSet.setPassword(PASSWORD);
            // Get every user from the database to check if the client's username exists.
            rowSet.setCommand("SELECT * FROM users WHERE username = ?"); // set query
            rowSet.setString(1, username);
            rowSet.execute(); // execute query

            // Register the new user.
            if(!rowSet.next()) {
                // add user to database.
                rowSet.moveToInsertRow();
                rowSet.updateString("username", username);
                rowSet.updateString("password", password);
                rowSet.insertRow();
                isLoggedIn = true;
                user = new Text(username);
                displayAlert(regisTitle, regisText);
            }
            else {
                // User exists but password is wrong.
                if(!rowSet.getObject(2).equals(password)) {
                    displayAlert(pWTitle, pWText);
                }
                // User logged in.
                else {
                    isLoggedIn = true;
                    user = new Text(username);
                    displayAlert(signInTitle, signInText);
                }
            }
        }
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
            System.exit(1);
        }
    }

    // To prevent repeated code.
    private void displayAlert(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    //TODO: check if the lobbies list is scrollable.
    //TODO: Make the brush have different thickness.
    //TODO: make the cursor look like a pen.
    //TODO: maybe add background image to the other menus.
    //TODO: Make a lobby scene.
    //TODO: backToLobby.setOnAction(); // Work on it after creating the lobby.
    //TODO: Make a functional chat box.
    //TODO: Create a server able to hold multiple rooms.
    //TODO: show the users in a room.
}


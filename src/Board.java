import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.ImageCursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;

import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import java.awt.*;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Stack;

public class Board extends Application{

    private GraphicsContext gc;

    private Scene whiteboard, lobby;

    private Button redo, undo, clear, backToLobby, exit;

    private ColorPicker colorChooser;

    private Text userList = new Text("Here will be displayed the online users in the room."),
            chat = new Text("Here will be the chat"),
            topM = new Text("Welcome to Whiteboard! Draw your minds out!");

    final String pSTitle = "Password too short", pSText = "Password must be at least 6 characters long.",
            regisTitle = "Successfully registered", regisText = "You've been successfully registered and logged in.",
            signInTitle = "Successfully logged in", signInText = "You've been successfully logged in.",
            pWTitle = "Wrong password", pWText = "You've entered a wrong password, please try again.";

    private final String[] shapes = {"Brush", "Line", "Oval", "Rectangle", "Rounded Rectangle", "Text"};
    private final ComboBox<String> shapeChooser = new ComboBox<>();
    private final Stack<MyDraw> myDraws = new Stack<>();
    private final Stack<MyDraw> deletedDraws = new Stack<>();

    private boolean toFill = false, isRoundRectChosen = false, isLoggedIn = false;
    private Color color = Color.BLACK; // Default color is black.

    private String addedLast = ""; // What was added last? Text or shape?

    // If the user isn't the host don't clear the board.
    private final boolean isHost = false;

    private final int CANVAS_HEIGHT = 590, CANVAS_WIDTH = 900;

    private String DATABASE_URL = "jdbc:postgresql:";
    private String USERNAME;
    private String PASSWORD;

    public static void main(String[] args) {
        launch(args);
    }

//    @Override
//    public void init() throws Exception {
//
//    }

    @Override
    public void start(Stage stage) /* throws Exception */ {

        java.util.List<String> args = getParameters().getRaw();
        if (args.size() != 3) {
            System.err.println("The program must be given 3 arguments: Database path, username and password");
            Platform.exit();
            return;
        }
        DATABASE_URL += args.get(0);
        USERNAME = args.get(1);
        PASSWORD = args.get(2);

        final int BOTTOM_MENU_SPACING = 10, LEFT_MENU_WIDTH = 200, RIGHT_MENU_WIDTH = 300, TOP_MENU_HEIGHT = 60,
                BOTTOM_MENU_LEFT = 320, TOOLBAR_HEIGHT = 60;

        /********************************** Initializing objects ********************************/

        shapeChooser.getItems().addAll(shapes);

        Label shapeLabel = new Label("Shape:");
        shapeLabel.setStyle(CssLayouts.cssBottomLayoutText);

        colorChooser = new ColorPicker();
        colorChooser.setValue(color);
        redo = new Button("Redo");
        undo = new Button("Undo");
        clear = new Button("Clear");
        backToLobby = new Button("Back to Lobby");
        exit = new Button("Exit");

        CheckBox fillShape = new CheckBox("Fill Shape");
        fillShape.setStyle(CssLayouts.cssBottomLayoutText);

        // This line prevents text overflow.
        userList.setWrappingWidth(125);

    /******************************** Whiteboard scene ********************************/

        /********************************** Building the bottom menu ********************************/

        GridPane bottomMenu = new GridPane();
        bottomMenu.setStyle(CssLayouts.cssBottomLayout);
        bottomMenu.setPadding(new Insets(BOTTOM_MENU_SPACING, BOTTOM_MENU_SPACING, BOTTOM_MENU_SPACING, BOTTOM_MENU_LEFT));
        bottomMenu.setVgap(BOTTOM_MENU_SPACING);
        bottomMenu.setHgap(BOTTOM_MENU_SPACING);

        // Adding buttons to the bottom menu.
        Object[] bottomMenuItems = { shapeLabel, shapeChooser, colorChooser, fillShape, redo, undo, clear, backToLobby, exit};
        Button[] bottomMenuButtons = { redo, undo, clear, backToLobby, exit };

        for(int i = 0; i < bottomMenuItems.length; i++) { GridPane.setConstraints((Node) bottomMenuItems[i], i, 0); }
        bottomMenu.getChildren().addAll(shapeLabel, shapeChooser, colorChooser, fillShape, redo, undo, clear, backToLobby, exit);

        shapeChooser.setValue(shapes[0]);

        /********************************** Building the left menu - online users list ********************************/

        // Online users in the room.
        VBox leftMenu = new VBox();
        leftMenu.getChildren().add(userList);
        setLayoutWidth(leftMenu, LEFT_MENU_WIDTH);

        /********************************** Building the right menu - chat box ********************************/

        VBox rightMenu = new VBox();
        rightMenu.getChildren().add(chat);
        setLayoutWidth(rightMenu, RIGHT_MENU_WIDTH);

        /********************************** Building the top menu - I don't know what to put here yet ********************************/

        HBox topMenu = new HBox();
        topMenu.getChildren().add(topM);
        topM.setFill(Color.GREEN);
        topMenu.setStyle(CssLayouts.cssTopLayout);
        topMenu.setMinHeight(TOP_MENU_HEIGHT);

        /********************************** Building the canvas on which the user will draw ********************************/

        HBox center = new HBox();
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        center.setStyle(CssLayouts.cssLayout + ";\n-fx-background-color: white");
        center.getChildren().add(canvas);
        gc = canvas.getGraphicsContext2D();

        /********************************** Color and fill shape functionality ********************************/

        /* Picking a new color */
        colorChooser.setOnAction(e -> {
            color = colorChooser.getValue();
            gc.setStroke(color);
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
                    myDraws.add(new MyLine(e.getX(),e.getY(),e.getX(),e.getY(),color));
                    isRoundRectChosen = false;
                    break;
                case "Oval":
                    myDraws.add(new MyOval(e.getX(),e.getY(),e.getX(),e.getY(),color,toFill));
                    isRoundRectChosen = false;
                    break;
                case "Rectangle":
                    myDraws.add(new MyRect(e.getX(),e.getY(),e.getX(),e.getY(),color,toFill));
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
                        myDraws.add(new MyRoundRect(e.getX(),e.getY(),e.getX(),e.getY(),color, toFill,arcWidth,arcHeight));
                    }
                    isRoundRectChosen = true;
                    break;
                case "Brush": // Free drawing.
                    myDraws.add(new MyBrush(e.getX(), e.getY(), color, toFill));
                    break;
                case "Text": // Displaying text on the canvas.
                    TextArea text = new TextArea();
                    Button btn = new Button("OK");

                    BorderPane textBox = new BorderPane();
                    textBox.setCenter(text);
                    textBox.setBottom(btn);
                    BorderPane.setAlignment(btn, Pos.BOTTOM_CENTER);

                    final Stage dialog = new Stage();
                    dialog.initModality(Modality.NONE);
                    dialog.initOwner(stage);
                    Scene dialogScene = new Scene(textBox, 300, 200);
                    dialog.setScene(dialogScene);
                    dialog.show();

                    btn.setOnAction(eBtn -> {
                        TextBox t = new TextBox(e.getX(), e.getY(), color, text.getText());
                        myDraws.add(t);
                        t.Draw(gc);
                        dialog.close();
                    });
                }
                if(!shapeChooser.getValue().equals("Text")) { addedLast = "Shape"; }
                else { addedLast = "Text"; }
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
//            if(!shapeChooser.getValue().equals("Brush")) { repaint(); }
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

        /******************************** Right menu - info ********************************/

        Text toKnow = new Text("Put a const String here that explains what this program is.");
        toKnow.setWrappingWidth(250);
        VBox info = new VBox();
        info.getChildren().add(toKnow);
        setLayoutWidth(info, RIGHT_MENU_WIDTH);

        /******************************** Center menu ********************************/

        Button b = new Button("Click to draw");
        b.setOnAction(e -> {
            stage.setScene(whiteboard);
           stage.setTitle("Whiteboard");
        });

        /******************************** Bottom menu ********************************/

        Button createR = new Button("Create room");
        Button login = new Button("Log in");
        HBox bottomM = new HBox();
        bottomM.setPadding(new Insets(BOTTOM_MENU_SPACING, BOTTOM_MENU_SPACING, BOTTOM_MENU_SPACING, BOTTOM_MENU_LEFT+100));
        bottomM.setSpacing(350);
        bottomM.getChildren().addAll(createR, login);
        bottomM.setStyle(CssLayouts.cssBottomLayout);

        /******************************** Login button event handler *******************************/

        login.setOnAction(e -> {
            if(isLoggedIn) return;
            Button confirm = new Button("Login/Register");
            TextField nickname = new TextField();
            PasswordField password = new PasswordField();
            Label username = new Label("Username:");
            Label pass = new Label("Password");

            GridPane loginWindow = new GridPane();
            loginWindow.setPadding(new Insets(10,10,10,20));
            loginWindow.setHgap(8);
            loginWindow.setVgap(10);

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
            Scene loginScene = new Scene(loginWindow, 250, 120);
            signIn.setScene(loginScene);
            signIn.show();

            confirm.setOnAction(e1 -> {
                if(password.getText().length() < 6) {
                    displayAlert(pSTitle, pSText);
                }
                else {
                    connectToDatabase(nickname.getText(), password.getText());
                    signIn.close();
                }
            });
        });

        /******************************** Dividing the lobby scene layout into sections and creating the scene ********************************/

        BorderPane lobbyLayout = new BorderPane();
        lobbyLayout.setTop(title);
        lobbyLayout.setRight(info);
        lobbyLayout.setCenter(b);
        lobbyLayout.setBottom(bottomM);
        lobby = new Scene(lobbyLayout, width, height);

    /******************************** Lobby scene - End ********************************/

        /******************************** Setting the stage ********************************/

        stage.setTitle("Lobby");
        stage.setScene(lobby);
        stage.show();
    }

    /******************************** Functions ********************************/

    /* Repaints the canvas */
    private void repaint() {
        gc.clearRect(0, 0, CANVAS_WIDTH,CANVAS_HEIGHT);
        for (MyDraw myDraw : myDraws) { myDraw.Draw(gc); }
    }

    private void setLayoutWidth(Pane p, int width) {
        if(p == null) { return; }
        p.setStyle(CssLayouts.cssLayout);
        p.setMaxWidth(width);
        p.setMinWidth(width);
    }

    /* If str is a number, returns that number. Else returns default value.
       Being used to bypass the user entering invalid input to the arcWidth and arcHeight properties. */
    public int isNumeric(String str) {
        try { return Integer.parseInt(str); }
        catch (NumberFormatException e) { return 10; }
    }

    private void connectToDatabase(String username, String password) {

        // connect to database books and query database
        // RowSetProvider class implements RowSetFactory which can be used to create various types of RowSets
        try (JdbcRowSet rowSet = RowSetProvider.newFactory().createJdbcRowSet()) {
            // specify JdbcRowSet properties
            rowSet.setUrl(DATABASE_URL);
            rowSet.setUsername(USERNAME);
            rowSet.setPassword(PASSWORD);
            rowSet.setCommand("SELECT * FROM users WHERE username = ?"); // set query
            rowSet.setString(1, username);
            rowSet.execute(); // execute query

            /* if username doesn't exist yet, add it to the database.
               if user exists, check if its entry matches the password. if not, tell the user he used a wrong password.
               otherwise, log it in.*/

            if(!rowSet.next()) {
                // add user to database.
                rowSet.moveToInsertRow();
                rowSet.updateString("username", username);
                rowSet.updateString("password", password);
                rowSet.insertRow();
                isLoggedIn = true;
                displayAlert(regisTitle, regisText);
                // change layout to show the user is logged in and make a boolean that indicates the user can enter a lobby.
            }
            else {
                // User exists but password is wrong.
                if(!rowSet.getObject(2).equals(password)) {
                    displayAlert(pWTitle, pWText);
                }
                // User logged in.
                else {
                    isLoggedIn = true;
                    displayAlert(signInTitle, signInText);
                    // change layout to show the user is logged in and make a boolean that indicates the user can enter a lobby.
                }
            }

//            ResultSetMetaData metaData = rowSet.getMetaData();
//            int numberOfColumns = metaData.getColumnCount();
        }
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
            System.exit(1);
        }

    }

    private void displayAlert(String title, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    //TODO: make the cursor look like a pen.
    //TODO: maybe add background image to the other menus.
    //TODO: Make a lobby scene.
    //TODO: backToLobby.setOnAction(); // Work on it after creating the lobby.
    //TODO: Register users.
    //TODO: Make a functional chat box.
    //TODO: Create a server able to hold multiple rooms.
    //TODO: show the users in a room.
}


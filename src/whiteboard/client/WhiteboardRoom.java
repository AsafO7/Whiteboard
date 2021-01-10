package whiteboard.client;/* This class represents a user and its tools (i.e drawings, nickname...) */

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.*;
import java.util.Stack;

public class WhiteboardRoom {

    private GraphicsContext gc;

    private Button redo, undo, clear, backToLobby, exit;

    private Slider thickness;

    private ColorPicker colorChooser;

    private final Text userList = new Text("Here will be displayed the online users in the room."),
            topM = new Text("Welcome to Whiteboard! Draw your minds out!");

    private final String[] shapes = {"Brush", "Line", "Oval", "Rectangle", "Rounded Rectangle", "Text"};
    private final ComboBox<String> shapeChooser = new ComboBox<>();
    private final Stack<MyDraw> myDraws = new Stack<>();
    private final Stack<MyDraw> deletedDraws = new Stack<>();

    private boolean toFill = false, isRoundRectChosen = false;
    private Color color = Color.BLACK; // Default color is black.

    // If the user isn't the host don't clear the board.
    private final boolean isHost = false;

    private final int CANVAS_HEIGHT = 590, CANVAS_WIDTH = 900;

    private String name;
    public WhiteboardRoom(String name) { this.name = name; }

    public Scene showBoard(Stage stage, Text user, Scene lobby) {

            final int GRID_MENU_SPACING = 10, LEFT_MENU_WIDTH = 200, RIGHT_MENU_WIDTH = 300, TOP_MENU_HEIGHT = 60,
                    BOTTOM_MENU_LEFT = 220, TOOLBAR_HEIGHT = 60, TEXT_WRAPPING_WIDTH = 125, DRAWING_TEXT_DIALOG_WINDOW_WIDTH = 300,
                    DRAWING_TEXT_DIALOG_WINDOW_HEIGHT = 200, CHAT_MESSAGE_WRAPPING_WIDTH = 230,
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
            Object[] bottomMenuItems = { shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness,
                    fillShape, redo, undo, clear, backToLobby, exit};
            Button[] bottomMenuButtons = { redo, undo, clear, backToLobby, exit };

            // Arranging them in a line.
            for(int i = 0; i < bottomMenuItems.length; i++) { GridPane.setConstraints((Node) bottomMenuItems[i], i, 0); }
            bottomMenu.getChildren().addAll(shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness,
                    fillShape, redo, undo, clear, backToLobby, exit);

            shapeChooser.setValue(shapes[0]);

            /********************************** Building the left menu - online users list ********************************/

            // Online users in the room.
            VBox leftMenu = new VBox();
            //leftMenu.getChildren().add(userList);
            //setLayoutWidth(leftMenu, LEFT_MENU_WIDTH);
            leftMenu.setMaxWidth(LEFT_MENU_WIDTH);
            leftMenu.setMinWidth(LEFT_MENU_WIDTH);
            leftMenu.setStyle(CssLayouts.cssLeftMenu + ";\n-fx-padding: 3 0 0 10;");
            leftMenu.getChildren().add(user);

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
                        break;
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

            //if(!myDraws.isEmpty()) { repaint(); }
        return new Scene(whiteboardLayout, width, height);
    }

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
        catch (NumberFormatException e) {
            int DEFAULT_ARC_VALUE = 10;
            return DEFAULT_ARC_VALUE; }
    }
}

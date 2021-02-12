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
import whiteboard.Packet;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.BlockingQueue;

public class WhiteboardRoom {

    private GraphicsContext gc;

    private Button redo, undo, clear, backToLobby, exit;

    private Slider thickness;

    private ColorPicker colorChooser;

    private final Text /* userList = new Text("Here will be displayed the online users in the room."),*/
            topM = new Text("Welcome to Whiteboard! Draw your minds out!");
    private List<Text> users = new ArrayList<>();

    private final String[] shapes = {"Brush", "Line", "Oval", "Rectangle", "Rounded Rectangle", "Text"};
    private final ComboBox<String> shapeChooser = new ComboBox<>();
    public final Stack<MyDraw> myDraws = new Stack<>();
    private final Stack<MyDraw> deletedDraws = new Stack<>();

    private boolean toFill = false, isRoundRectChosen = false;
    private Color color = Color.BLACK; // Default color is black.

    private VBox chatBox = new VBox(), leftMenu = new VBox();

    private InputHandler input = null;

    // If the user isn't the host don't clear the board.
    private boolean isHost = false;

    private final int CANVAS_HEIGHT = 590, CANVAS_WIDTH = 900;

    private String host;
    public WhiteboardRoom(String host, InputHandler input) {
        this.host = host;
        isHost = true;
        this.input = input;
    }

    public VBox getOnlineUsersPanel() {
        return leftMenu;
    }

    public List<Text> getOnlineUsers() { return users; }

    public Scene showBoard(Stage stage, Text user, Scene lobby, BlockingQueue<Packet> outQueue) {

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

//            users.add(user);
//            // This line prevents text overflow.
//            users.get(users.size() - 1).setWrappingWidth(TEXT_WRAPPING_WIDTH);

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
            //leftMenu = new VBox();
            leftMenu.setMaxWidth(LEFT_MENU_WIDTH);
            leftMenu.setMinWidth(LEFT_MENU_WIDTH);
            leftMenu.setStyle(CssLayouts.cssLeftMenu + ";\n-fx-padding: 3 0 0 10;");
//            for(int i = 0; i < users.size(); i++) {
//                leftMenu.getChildren().add(users.get(i));
//            }
            //leftMenu.getChildren().add(user);
            try {
                outQueue.put(Packet.requestUsersListGUI());
            } catch (Exception e) {
                e.printStackTrace();
            }

            /********************************** Building the right menu - chat box ********************************/

            TextField chatMsg = new TextField(); // The user will write messages in here.
            VBox chatMsgWrapper = new VBox();
            ScrollPane chatWrapper = new ScrollPane();
            chatWrapper.setContent(chatBox);
            chatWrapper.setStyle("-fx-background-color: white");
            chatMsgWrapper.setStyle(CssLayouts.cssBorder);
            chatMsgWrapper.setPrefHeight(20);
            chatMsgWrapper.getChildren().add(chatMsg); // Container for the textfield.
            BorderPane rightMenu = new BorderPane(); // Right menu wrapper.
            rightMenu.setBottom(chatMsgWrapper);
            rightMenu.setCenter(chatWrapper);
            setLayoutWidth(rightMenu, RIGHT_MENU_WIDTH);
            chatWrapper.setVvalue(1.0); // Makes the scrollpane scroll to the bottom automatically.

            // Event handler for sending a message.
            chatMsg.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
                if(e.getCode() == KeyCode.ENTER) {
                    // Case of empty message.
                    if(chatMsg.getText().trim().length() == 0) { return; }
                    //TODO: Make it so the message won't slide off screen(in the text box).
                    Text msg = new Text(user.getText() + ": " + chatMsg.getText());
                    msg.setStyle(CssLayouts.cssChatText);
                    msg.setWrappingWidth(CHAT_MESSAGE_WRAPPING_WIDTH);

                    try {
                        outQueue.put(Packet.sendMessage(chatMsg.getText()));
                    } catch (InterruptedException interruptedException) {
                        interruptedException.printStackTrace();
                    }

                    chatBox.getChildren().add(msg);
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
                        if(isHost) {
                            myDraws.clear();
                            deletedDraws.clear();
                            gc.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
                        }
                    }

                    /* backToLobby button functionality. */
                    if(e.getSource() == backToLobby) {
                        //leftMenu.getChildren().remove(user);
                        //users.remove(user);
                        if(isHost) { isHost = false; }
                        //TODO: make the next user in users the host.
                        chatBox.getChildren().clear();
                        try {
                            outQueue.put(Packet.requestExitRoom());
                            outQueue.put(Packet.requestRoomsNames());
                        } catch (InterruptedException interruptedException) {
                            interruptedException.printStackTrace();
                        }
                        stage.setScene(lobby);
                        stage.setTitle("Lobby");
                        myDraws.clear();
                        input.myRoom = null;
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

            canvas.setOnMouseReleased(e -> {
                //TODO: send the drawing to the server.
                //myDraws.peek();
                CompleteDraw drawing = null;
                String colorInHexa = String.format( "#%02X%02X%02X",
                        (int)( myDraws.peek().getColor().getRed() * 255 ),
                        (int)( myDraws.peek().getColor().getGreen() * 255 ),
                        (int)( myDraws.peek().getColor().getBlue() * 255 ) );
                if(myDraws.peek() instanceof MyBrush) {
                    /* String color, double thickness, double x1, double y1, double x2, double y2, boolean fill, int arcW, int arcH,
                        ArrayList<Double> xPoints, ArrayList<Double> yPoints) */
                    drawing = new CompleteDraw(colorInHexa, ((MyBrush) myDraws.peek()).getThickness(), -1, -1, -1, -1,
                            ((MyBrush) myDraws.peek()).isFill(), -1, -1, ((MyBrush) myDraws.peek()).getXPoints(),
                            ((MyBrush) myDraws.peek()).getYPoints(), /*gc,*/ "MyBrush");
                }
                else if(myDraws.peek() instanceof MyLine) {
                    drawing = new CompleteDraw(colorInHexa, ((MyLine) myDraws.peek()).getThickness(), ((MyLine) myDraws.peek()).getX1(),
                            ((MyLine) myDraws.peek()).getY1(), ((MyLine) myDraws.peek()).getX2(), ((MyLine) myDraws.peek()).getY2(),
                            false, -1, -1, null, null, /*gc,*/ "MyLine");
                }
                else if(myDraws.peek() instanceof MyOval) {
                    drawing = new CompleteDraw(colorInHexa, ((MyOval) myDraws.peek()).getThickness(), ((MyOval) myDraws.peek()).getX1(),
                            ((MyOval) myDraws.peek()).getY1(), ((MyOval) myDraws.peek()).getWidth(), ((MyOval) myDraws.peek()).getHeight(),
                            ((MyOval) myDraws.peek()).toFill(), -1, -1, null, null, /*gc,*/ "MyOval");
                }
                else if(myDraws.peek() instanceof MyRect) {
                    drawing = new CompleteDraw(colorInHexa, ((MyRect) myDraws.peek()).getThickness(), ((MyRect) myDraws.peek()).getX1(),
                            ((MyRect) myDraws.peek()).getY1(), ((MyRect) myDraws.peek()).getWidth(), ((MyRect) myDraws.peek()).getHeight(),
                            ((MyRect) myDraws.peek()).toFill(), -1, -1, null, null, /*gc,*/ "MyRect");
                }
                else if(myDraws.peek() instanceof MyRoundRect) {
                    drawing = new CompleteDraw(colorInHexa, ((MyRoundRect) myDraws.peek()).getThickness(), ((MyRoundRect) myDraws.peek()).getX1(),
                            ((MyRoundRect) myDraws.peek()).getY1(), ((MyRoundRect) myDraws.peek()).getWidth(),
                            ((MyRoundRect) myDraws.peek()).getHeight(), ((MyRoundRect) myDraws.peek()).toFill(),
                            ((MyRoundRect) myDraws.peek()).getArcWidth(), ((MyRoundRect) myDraws.peek()).getArcHeight(),
                            null, null, /*gc,*/ "MyRoundRect");
                }
                else if(myDraws.peek() instanceof TextBox) {
                    drawing = new CompleteDraw(colorInHexa, ((TextBox) myDraws.peek()).getThickness(), ((TextBox) myDraws.peek()).getX(),
                            ((TextBox) myDraws.peek()).getY(), -1, -1,
                            false, -1, -1, null, null, /*gc,*/ "TextBox");
                    drawing.setText(((TextBox) myDraws.peek()).getText());
                }
                try {
                    outQueue.put(Packet.sendNewDrawing(drawing));
                } catch (InterruptedException exception) {
                    exception.printStackTrace();
                }
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

            try {
                outQueue.put(Packet.requestCurrentDrawings());
            }
            catch (Exception e) {
                e.printStackTrace();
            }

        return new Scene(whiteboardLayout, width, height);
    }

    /* Repaints the canvas */
    public void repaint() {
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

    public VBox getChatBox() {
        return chatBox;
    }

}

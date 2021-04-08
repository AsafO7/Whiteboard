package whiteboard.client;

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
import whiteboard.server.IServerHandler;

import java.awt.*;
import java.io.*;
import java.rmi.RemoteException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/* This class represents a whiteboard room. */

public class WhiteboardRoom {

    private final RMIHandler rmiQueue;
    private final IServerHandler stub;
    private GraphicsContext gc;

    private Button redo, undo, clear, backToLobby, exit, saveDrawing;

    private Slider thickness;

    private ColorPicker colorChooser;

    private final Text topM = new Text("Welcome to Whiteboard! Draw your minds out!");

    private final String[] shapes = {"Brush", "Line", "Oval", "Rectangle", "Rounded Rectangle", "Text"};
    private final ComboBox<String> shapeChooser = new ComboBox<>();
    public final Stack<MyDraw> myDraws = new Stack<>();
    public MyDraw currDraw = null;

    private boolean toFill = false, isRoundRectChosen = false;
    private Color color = Color.BLACK; // Default color is black.

    private VBox chatBox = new VBox(), leftMenu = new VBox();

    private InputHandler input = null;

    // If the user isn't the host don't clear the board.
    private boolean isHost = false;

    // This code should make the window adaptable to all screen sizes.
    GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    int width = gd.getDisplayMode().getWidth();
    int height = gd.getDisplayMode().getHeight() - 60;

    private String host;
    public WhiteboardRoom(String host, InputHandler input, RMIHandler rmiQueue, IServerHandler stub) {
        this.host = host;
        isHost = true;
        this.input = input;
        this.rmiQueue = rmiQueue;
        this.stub = stub;
    }

    public VBox getOnlineUsersPanel() {
        return leftMenu;
    }

    public void setHost(boolean isHost) {
        this.isHost = isHost;
    }

    public Scene showBoard(Stage stage, Text user, Scene lobby) {

        final int GRID_MENU_SPACING = 10, BOTTOM_MENU_LEFT = 160,
                DRAWING_TEXT_DIALOG_WINDOW_WIDTH = 300, DRAWING_TEXT_DIALOG_WINDOW_HEIGHT = 200,
                CHAT_MESSAGE_WRAPPING_WIDTH = 230, MIN_LINE_THICKNESS = 1, MAX_LINE_THICKNESS = 15;

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
        saveDrawing = new Button("Save drawing");

        CheckBox fillShape = new CheckBox("Fill Shape");
        fillShape.setStyle(CssLayouts.cssBottomLayoutText);

        /******************************** Whiteboard scene ********************************/

        /********************************** Building the bottom menu ********************************/

        GridPane bottomMenu = new GridPane();
        bottomMenu.setStyle(CssLayouts.cssBottomLayout);
        bottomMenu.setPadding(new Insets(GRID_MENU_SPACING, GRID_MENU_SPACING, GRID_MENU_SPACING, BOTTOM_MENU_LEFT));
        bottomMenu.setVgap(GRID_MENU_SPACING);
        bottomMenu.setHgap(GRID_MENU_SPACING);
        bottomMenu.setMinHeight(height/12);
        bottomMenu.setMaxHeight(height/12);

        // Adding buttons to the bottom menu.
        Object[] bottomMenuItems = { saveDrawing, shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness,
                fillShape, redo, undo, clear, backToLobby, exit};
        Button[] bottomMenuButtons = { saveDrawing, redo, undo, clear, backToLobby, exit };

        // Arranging them in a line.
        for(int i = 0; i < bottomMenuItems.length; i++) { GridPane.setConstraints((Node) bottomMenuItems[i], i, 0); }
        bottomMenu.getChildren().addAll(saveDrawing, shapeLabel, shapeChooser, colorChooser, thicknessLabel, thickness,
                fillShape, redo, undo, clear, backToLobby, exit);

        shapeChooser.setValue(shapes[0]);

        /********************************** Building the left menu - online users list ********************************/

        leftMenu.setMaxWidth(width/6);
        leftMenu.setMinWidth(width/6);
        leftMenu.setStyle(CssLayouts.cssLeftMenu);

        rmiQueue.put(() -> {
            try {
                stub.handleGetRooms();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
        rmiQueue.put(() -> {
            try {
                stub.handleUpdateUsersListGUI();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });


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
        rightMenu.setStyle(CssLayouts.cssBorder);
        rightMenu.setMaxWidth(width/4.5);
        rightMenu.setMinWidth(width/4.5);
        chatWrapper.setVvalue(1.0); // Makes the scrollpane scroll to the bottom automatically.

        // Event handler for sending a message.
        chatMsg.addEventHandler(KeyEvent.KEY_PRESSED, e -> {
            if(e.getCode() == KeyCode.ENTER) {
                // Case of empty message.
                if(chatMsg.getText().trim().length() == 0) { return; }
                Text msg = new Text(user.getText() + ": " + chatMsg.getText());
                msg.setStyle(CssLayouts.cssChatText);
                msg.setWrappingWidth(CHAT_MESSAGE_WRAPPING_WIDTH);

                rmiQueue.put(() -> {
                    try {
                        stub.handleSendMessage(msg.getText());
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                });
                chatBox.getChildren().add(msg);
                chatMsg.clear();
            }
        });

        /********************************** Building the top menu - The title ********************************/

        HBox topMenu = new HBox();
        topMenu.getChildren().add(topM);
        topM.setFill(Color.GREEN);
        topMenu.setStyle(CssLayouts.cssTopLayout);
        topMenu.setMinHeight(height/12);
        topMenu.setMaxHeight(height/12);

        /********************************** Building the canvas on which the user will draw ********************************/

        HBox center = new HBox();
        Canvas canvas = new Canvas(width/1.575, height/1.2);
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

            saveDrawing.setOnAction(e -> {
                List<MyDraw> drawings = new ArrayList<>(this.myDraws);


                try(Connection connection = DriverManager.getConnection(Lobby.DATABASE_URL);)
                {
                    // create a database connection
                    Statement statement = connection.createStatement();
                    statement.setQueryTimeout(2);  // set timeout to 2 sec.

                    statement.executeUpdate("DROP TABLE IF EXISTS save");

                    statement.executeUpdate("CREATE TABLE save(DRAWING BLOB)");
                }
                catch(SQLException sqlException)
                {
                    System.err.println(sqlException.getMessage());
                }

                try(Connection connection = DriverManager.getConnection(Lobby.DATABASE_URL);) {

                    List<CompleteDraw> arr = new ArrayList<>();
                    for(int i = 0; i < drawings.size(); i++) {
                        arr.add(convertMyDrawToCompleteDraw(drawings.get(i)));
                    }

                    PreparedStatement statement = connection.prepareStatement("INSERT INTO save(DRAWING) VALUES(?)");
                    statement.setQueryTimeout(2);  // set timeout to 30 sec.
                    byte[] byteArr = serialize(arr);
                    ByteArrayInputStream bis = new ByteArrayInputStream(byteArr);
                    statement.setBinaryStream(1, bis, (int) byteArr.length);
                    statement.executeUpdate();
                }
                catch(SQLException | IOException sqlException)
                {
                    System.err.println(sqlException.getMessage());
                }
            });

            /* redo button functionality. */
            redo.setOnAction(e -> {
                rmiQueue.put(() -> {
                    try {
                        stub.redoDrawing();
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                });
            });

            /* undo button functionality. */
            undo.setOnAction(e -> {
                rmiQueue.put(() -> {
                    try {
                        stub.undoDrawing();
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                });
            });

            /* clear button functionality. */
            clear.setOnAction(e -> {
                if(isHost) {
                    rmiQueue.put(() -> {
                        try {
                            stub.handleClearBoard();
                        } catch (RemoteException remoteException) {
                            remoteException.printStackTrace();
                        }
                    });
                }
            });

            /* backToLobby button functionality. */
            backToLobby.setOnAction(e -> {
                chatBox.getChildren().clear();

                rmiQueue.put(() -> {
                    try {
                        stub.handleRequestExitRoom();
                        stub.handleGetRooms();
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                });

                stage.setScene(lobby);
                stage.setTitle("Lobby");
                myDraws.clear();
                input.myRoom = null;
                input.setRoomName(null);
            });

            /* exit button functionality. */
            exit.setOnAction(e -> { Platform.exit(); });

        /********************************** Choosing shapes event handler ********************************/

        canvas.setOnMousePressed(e -> {
            rmiQueue.put(() -> {
                try {
                    stub.handleChangeUsername(user.getText() + " drawing...");
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            });
            if(shapeChooser.getValue() == null) { return; }

            TextInputDialog arcH = new TextInputDialog(), arcW = new TextInputDialog();
            /* Creating the shape to be drawn next. */
            switch(shapeChooser.getValue()) {
                case "Line":
                    currDraw = new MyLine(e.getX(),e.getY(),e.getX(),e.getY(),color, thickness.getValue());
                    isRoundRectChosen = false;
                    break;
                case "Oval":
                    currDraw = new MyOval(e.getX(),e.getY(),0,0,color, thickness.getValue(), toFill);
                    isRoundRectChosen = false;
                    break;
                case "Rectangle":
                    currDraw = new MyRect(e.getX(),e.getY(),0,0,color, thickness.getValue(), toFill);
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
                        currDraw = new MyRoundRect(e.getX(),e.getY(),0,0,color, thickness.getValue(),
                                toFill,arcWidth,arcHeight);
                    }
                    isRoundRectChosen = true;
                    break;
                case "Brush": // Free drawing.
                    currDraw = new MyBrush(e.getX(), e.getY(), color, thickness.getValue(), toFill);
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
                        CompleteDraw drawing = convertMyDrawToCompleteDraw(myDraws.peek());
                        rmiQueue.put(() -> {
                            try {
                                stub.handleAddNewDrawing(drawing);
                            } catch (RemoteException remoteException) {
                                remoteException.printStackTrace();
                            }
                        });
                        repaint();
                        dialog.close();
                    });
                    break;
            }
        });

        /********************************** Drawing shapes event handler ********************************/

        canvas.setOnMouseDragged(e -> {
            if(shapeChooser.getValue() == null) { return; }

            MyDraw drawable = null;
            if(currDraw != null) { drawable = currDraw; }
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
        });

        canvas.setOnMouseReleased(e -> {
            rmiQueue.put(() -> {
                try {
                    stub.handleChangeUsername(user.getText());
                } catch (RemoteException remoteException) {
                    remoteException.printStackTrace();
                }
            });
            if (currDraw != null) {
                myDraws.add(currDraw);
                currDraw = null;
                CompleteDraw drawing = convertMyDrawToCompleteDraw(myDraws.peek());
                rmiQueue.put(() -> {
                    try {
                        stub.handleAddNewDrawing(drawing);
                    } catch (RemoteException remoteException) {
                        remoteException.printStackTrace();
                    }
                });
            }
        });

        /******************************** Dividing the board scene layout into sections and creating the scene ********************************/

        BorderPane whiteboardLayout = new BorderPane();
        whiteboardLayout.setBottom(bottomMenu);
        whiteboardLayout.setLeft(leftMenu);
        whiteboardLayout.setTop(topMenu);
        whiteboardLayout.setRight(rightMenu);
        whiteboardLayout.setCenter(center);

        rmiQueue.put(() -> {
            try {
                stub.handleRequestCurrDrawings();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });

        return new Scene(whiteboardLayout, width, height);
    }

    /*************************************** Functions ************************************************/

    /* Repaints the canvas */
    public void repaint() {
        gc.clearRect(0, 0, width/1.575, height / 1.2);
        for (MyDraw myDraw : myDraws) { myDraw.Draw(gc); }
        if (currDraw != null) { currDraw.Draw(gc); }
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

    public CompleteDraw convertMyDrawToCompleteDraw(MyDraw draw) {
        CompleteDraw drawing = null;
        String colorInHexa = String.format( "#%02X%02X%02X",
                (int)( draw.getColor().getRed() * 255 ),
                (int)( draw.getColor().getGreen() * 255 ),
                (int)( draw.getColor().getBlue() * 255 ) );
        if(draw instanceof MyBrush) {

            drawing = new CompleteDraw(colorInHexa, ((MyBrush) draw).getThickness(), -1, -1, -1, -1,
                    ((MyBrush) draw).isFill(), -1, -1, ((MyBrush) draw).getXPoints(),
                    ((MyBrush) draw).getYPoints(), "MyBrush", "");
        }
        else if(draw instanceof MyLine) {
            drawing = new CompleteDraw(colorInHexa, ((MyLine) draw).getThickness(), ((MyLine) draw).getX1(),
                    ((MyLine) draw).getY1(), ((MyLine) draw).getX2(), ((MyLine) draw).getY2(),
                    false, -1, -1, null, null, "MyLine", "");
        }
        else if(draw instanceof MyOval) {
            drawing = new CompleteDraw(colorInHexa, ((MyOval) draw).getThickness(), ((MyOval) draw).getX1(),
                    ((MyOval) draw).getY1(), ((MyOval) draw).getWidth(), ((MyOval) draw).getHeight(),
                    ((MyOval) draw).toFill(), -1, -1, null, null, "MyOval", "");
        }
        else if(draw instanceof MyRect) {
            drawing = new CompleteDraw(colorInHexa, ((MyRect) draw).getThickness(), ((MyRect) draw).getX1(),
                    ((MyRect) draw).getY1(), ((MyRect) draw).getWidth(), ((MyRect) draw).getHeight(),
                    ((MyRect) draw).toFill(), -1, -1, null, null, "MyRect", "");
        }
        else if(draw instanceof MyRoundRect) {
            drawing = new CompleteDraw(colorInHexa, ((MyRoundRect) draw).getThickness(), ((MyRoundRect) draw).getX1(),
                    ((MyRoundRect) draw).getY1(), ((MyRoundRect) draw).getWidth(),
                    ((MyRoundRect) draw).getHeight(), ((MyRoundRect) draw).toFill(),
                    ((MyRoundRect) draw).getArcWidth(), ((MyRoundRect) draw).getArcHeight(),
                    null, null, "MyRoundRect", "");
        }
        else if(draw instanceof TextBox) {
            drawing = new CompleteDraw(colorInHexa, ((TextBox) draw).getThickness(), ((TextBox) draw).getX(),
                    ((TextBox) draw).getY(), -1, -1,
                    false, -1, -1, null, null, "TextBox", ((TextBox) draw).getText());
            drawing.setText(((TextBox) draw).getText());
        }
        return drawing;
    }

    /* Serializes an object */
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

}

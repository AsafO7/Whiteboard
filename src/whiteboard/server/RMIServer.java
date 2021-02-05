package whiteboard.server;

import javafx.scene.Scene;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import whiteboard.client.RequestsHandler;
import whiteboard.client.WhiteboardRoom;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.JdbcRowSet;
import javax.sql.rowset.RowSetProvider;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RMIServer implements RequestsHandler {

    private static final long serialVersionUID = 9108779783102530646L;

    private final int REGISTERED = 1, LOGGED_IN = 2, WRONG_PASSWORD = 3, LOGGED_IN_ALREADY = 4;

    private static String DATABASE_URL = "jdbc:postgresql:", USERNAME, PASSWORD;

    private String user = "";

    private final List<WhiteboardRoom> rooms = Collections.synchronizedList(new ArrayList<>());

    private final List<String> roomsNames = new ArrayList<>(), hostsNames = new ArrayList<>(), userList = new ArrayList<>();

    int numOfRooms = 0;

    public RMIServer() throws RemoteException {
        super();
        //System.out.println("Server's ready");
    }

    public static void main(String[] args) {

        Registry registry;
        try {
            RMIServer obj = new RMIServer();

            RequestsHandler stub = (RequestsHandler) UnicastRemoteObject.exportObject(obj, 0);
            // Binding the remote object (stub) in the registry
            registry = LocateRegistry.createRegistry(1099);

            registry.rebind("ServerForSettingPrimeAttribute", stub);

            //java.util.List<String> args = getParameters().getRaw();
            if (args.length != 3) {
                System.err.println("The program must be given 3 arguments: Database path, username and password");
                System.exit(1);
                return;
            }
            DATABASE_URL += args[0];
            USERNAME = args[1];
            PASSWORD = args[2];

            System.out.println("Server's ready.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO: create a new table with the name roomName, and add a new row in the RoomsNames table with roomName.
    // This method creates a new room(table) in the database.
    @Override
    public WhiteboardRoom createLobby(String roomName) throws RemoteException {
        //WhiteboardRoom room = new WhiteboardRoom(user, roomName);
        rooms.add(new WhiteboardRoom(user, roomName));
        roomsNames.add(roomName);
        numOfRooms++;
//        try (JdbcRowSet rowSet = RowSetProvider.newFactory().createJdbcRowSet()) {
//            // specify JdbcRowSet properties
//            rowSet.setUrl(DATABASE_URL);
//            rowSet.setUsername(USERNAME);
//            rowSet.setPassword(PASSWORD);
//
////            rowSet.setCommand("CREATE TABLE " + roomName + "(" +
////                    "SHAPE VARCHAR(30),"
////                    + "COLOR VARCHAR(30),"
////                    + "THICKNESS DOUBLE PRECISION,"
////                    + "X1 DOUBLE PRECISION,"
////                    + "Y1 DOUBLE PRECISION,"
////                    + "X2 DOUBLE PRECISION,"
////                    + "Y2 DOUBLE PRECISION,"
////                    + "FILL BIT,"
////                    + "ARCW INT,"
////                    + "ARCH INT,"
////                    + "XPOINTS DOUBLE PRECISION ARRAY,"
////                    + "YPOINTS DOUBLE PRECISION ARRAY,"
////                    + "TEXT VARCHAR(512))");
////            // set query
////            rowSet.execute(); // execute query
//
//            //TODO: LEARN TO INSERT ROWS TO A TABLE
//            rowSet.setCommand("SELECT * FROM roomsnames");
//            rowSet.execute();
//            //ResultSetMetaData metaData = rowSet.getMetaData();
//            rowSet.moveToInsertRow();
//            rowSet.updateString("name", roomName);
//            rowSet.updateString("host", user);
//            rowSet.insertRow();
//
//            //room.showBoard(stage, new Text(user), lobby);
//            //return rooms.get(numOfRooms - 1);
//        }
//        //TODO: why is this crap creates the table but throws an error?
//        catch (SQLException sqlException)
//        {
//            sqlException.printStackTrace();
//        }
        return rooms.get(numOfRooms - 1);
    }

//    public WhiteboardRoom getLatestRoom() throws RemoteException {
//        return rooms.get(rooms.size() - 1);
//    }

    @Override
    public int connectToDatabase(String username, String password) throws RemoteException {
        // connect to database books and query database.
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
                user = username;
                userList.add(user);
                return REGISTERED;
            }
            else {
                // User exists but password is wrong.
                if(!rowSet.getObject(2).equals(password)) { return WRONG_PASSWORD; }
                else {
                    // User is already logged in.
                    if(userList.contains(username)) {
                        return LOGGED_IN_ALREADY;
                    }
                    else {
                        user = username;
                        userList.add(user);
                        return LOGGED_IN;
                    }
                }
            }
        }
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
            System.exit(1);
        }
        return 0;
    }

    @Override
    public List<WhiteboardRoom> refreshRooms() throws RemoteException {
        //TODO: retrieve the rooms from the database and display them on the GUI
//        try (CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet()) {
//            rowSet.setUrl(DATABASE_URL);
//            rowSet.setUsername(USERNAME);
//            rowSet.setPassword(PASSWORD);
//            // Get every created room.
//            rowSet.setCommand("Select name FROM roomsnames"); // set query
//            rowSet.execute();
//
//            ResultSetMetaData metaData = rowSet.getMetaData();
//            int numberOfColumns = metaData.getColumnCount();
//            roomsNames.clear();
//            while (rowSet.next()) {
//                for(int i = 1; i <= numberOfColumns; i++) {
//                    roomsNames.add((String) rowSet.getObject(i));
//                }
//            }
//        }
//        catch (SQLException e) {
//            e.printStackTrace();
//        }
        return rooms;
    }

    @Override
    public List<String> getHostsNames() throws RemoteException {
        try (CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet()) {
            rowSet.setUrl(DATABASE_URL);
            rowSet.setUsername(USERNAME);
            rowSet.setPassword(PASSWORD);
            // Get every user from the database to check if the client's username exists.
            rowSet.setCommand("Select host FROM roomsnames"); // set query
            rowSet.execute();

            ResultSetMetaData metaData = rowSet.getMetaData();
            int numberOfColumns = metaData.getColumnCount();
            hostsNames.clear();
            while (rowSet.next()) {
                for(int i = 1; i <= numberOfColumns; i++) {
                    hostsNames.add((String) rowSet.getObject(i));
                }
            }
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        return hostsNames;
    }

    public boolean roomNameExists(String roomName) throws RemoteException{
        try (JdbcRowSet rowSet = RowSetProvider.newFactory().createJdbcRowSet()) {
            // specify JdbcRowSet properties
            rowSet.setUrl(DATABASE_URL);
            rowSet.setUsername(USERNAME);
            rowSet.setPassword(PASSWORD);

//            rowSet.setCommand("CREATE TABLE " + roomName + "(" +
//                    "SHAPE VARCHAR(30),"
//                    + "COLOR VARCHAR(30),"
//                    + "THICKNESS DOUBLE PRECISION,"
//                    + "X1 DOUBLE PRECISION,"
//                    + "Y1 DOUBLE PRECISION,"
//                    + "X2 DOUBLE PRECISION,"
//                    + "Y2 DOUBLE PRECISION,"
//                    + "FILL BIT,"
//                    + "ARCW INT,"
//                    + "ARCH INT,"
//                    + "XPOINTS DOUBLE PRECISION ARRAY,"
//                    + "YPOINTS DOUBLE PRECISION ARRAY,"
//                    + "TEXT VARCHAR(512))");
//            // set query
//            rowSet.execute(); // execute query

            //TODO: LEARN TO INSERT ROWS TO A TABLE
            rowSet.setCommand("SELECT * FROM roomsnames WHERE name = ?");
            rowSet.setString(1, roomName);
            rowSet.execute();
            if(!rowSet.next()) {
                return false;
            }
//            ResultSetMetaData metaData = rowSet.getMetaData();
//            int numOfColumns = metaData.getColumnCount();
//            while (rowSet.next()) {
//                for (int i = 0; i < numOfColumns; i++) {
//                    System.out.println((String) rowSet.getObject(i));
//                    if (((String)rowSet.getObject(i)).equals(roomName)) {
//                        return true;
//                    }
//                }
//            }
        }
        //TODO: why is this crap creates the table but throws an error?
        catch (SQLException sqlException)
        {
            sqlException.printStackTrace();
        }
        return true;
    }

    public void displayChatMsg(Text msg, WhiteboardRoom room) throws RemoteException {
        room.getChatBox().getChildren().add(msg);
//        for(int i = 0; i < rooms.size(); i++) {
//            if(rooms.get(i).equals(room)) {
//                rooms.get(i).getChatBox().getChildren().add(msg);
//            }
//        }
    }

}

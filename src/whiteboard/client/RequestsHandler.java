package whiteboard.client;

import javafx.scene.text.Text;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface RequestsHandler extends Remote {
    /* Write here the requests(functions) signatures. */
    WhiteboardRoom createLobby(String roomName) throws RemoteException;

    int connectToDatabase(String username, String password) throws RemoteException;

    List<WhiteboardRoom> refreshRooms() throws RemoteException;

    List<String> getHostsNames() throws RemoteException;

    boolean roomNameExists(String roomName) throws RemoteException;

    void displayChatMsg(Text msg, WhiteboardRoom room) throws RemoteException;

    //WhiteboardRoom getLatestRoom() throws RemoteException;

    //void getDrawings() throws RemoteException;
}

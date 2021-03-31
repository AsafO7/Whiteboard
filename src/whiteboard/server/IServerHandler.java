package whiteboard.server;

import whiteboard.Packet;
import whiteboard.client.CompleteDraw;
import whiteboard.client.IClientHandler;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IServerHandler extends Remote {
    public void setClientStub(IClientHandler stub) throws RemoteException;

    public void handleCreateRoomWithDrawings(String name, List<CompleteDraw> drawings) throws RemoteException;

    public void handleCreateRoom(String name) throws RemoteException;

    public void handleRequestUsername(String username) throws RemoteException;

    public void handleGetRooms() throws RemoteException;

    public void handleAddUserToRoom(String roomName) throws RemoteException;

    public void handleUpdateUsersListGUI() throws RemoteException;

    public void handleSendMessage(String messageToSend) throws RemoteException;

    public void redoDrawing() throws RemoteException;

    public void undoDrawing() throws RemoteException;

    public void handleClearBoard() throws RemoteException;

    public void handleRequestExitRoom() throws RemoteException;

    public void handleChangeUsername(String userName) throws RemoteException;

    public void handleAddNewDrawing(CompleteDraw drawing) throws RemoteException;

    public void handleRequestCurrDrawings() throws RemoteException;
}

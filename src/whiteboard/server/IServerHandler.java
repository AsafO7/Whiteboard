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
}

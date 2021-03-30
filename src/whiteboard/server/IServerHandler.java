package whiteboard.server;

import whiteboard.Packet;
import whiteboard.client.CompleteDraw;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IServerHandler extends Remote {
    public Packet handleCreateRoomWithDrawings(String name, List<CompleteDraw> drawings) throws RemoteException;
    public Packet handleCreateRoom(String name) throws RemoteException;
    public Packet handleRequestUsername(String username) throws RemoteException;
    public Packet handleGetRooms() throws RemoteException;
    public Packet handleAddUserToRoom(String roomName) throws RemoteException;
}

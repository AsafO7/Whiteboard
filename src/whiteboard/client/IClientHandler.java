package whiteboard.client;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface IClientHandler extends Remote {
    public void handleAckUsername(boolean ack) throws RemoteException;
    public void updateUsersListGUI(List<String> users) throws RemoteException;
    public void updateDrawStack(List<CompleteDraw> drawings) throws RemoteException;
    public void handleAckCreateRoom(boolean ack) throws RemoteException;
    public void handleAckJoinRoom(boolean ack) throws RemoteException;
    public void handleRoomsList(List<String> roomsNames) throws RemoteException;
    public void handleReceivedMessage(String msg) throws RemoteException;
}

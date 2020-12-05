
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ServerSide extends Remote {

    void printMsg() throws RemoteException;
}

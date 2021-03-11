package TestRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IServerHandler extends Remote {
    String sayHello() throws RemoteException;
}
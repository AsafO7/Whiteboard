package TestRMI;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IClientHandler extends Remote {
    String sayHello() throws RemoteException;
}
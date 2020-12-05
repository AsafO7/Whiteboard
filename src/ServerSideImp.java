import java.rmi.RemoteException;

public class ServerSideImp implements  ServerSide {

    @Override
    public void printMsg() throws RemoteException {
        System.out.println("Hello World");
    }
}

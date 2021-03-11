package TestRMI;

public class ServerHandler implements IServerHandler {

    public ServerHandler() {}

    public String sayHello() {
        return "Hello, Client!";
    }
}
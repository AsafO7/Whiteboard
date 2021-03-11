package TestRMI;

public class ClientHandler implements IClientHandler {

    public ClientHandler() {}

    public String sayHello() {
        return "Hello, Server!";
    }
}
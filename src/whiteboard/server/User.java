package whiteboard.server;

import whiteboard.server.Handler;

public class User {
    private Handler handler;
    private String name;

    public User(Handler handler, String name) {
        this.handler = handler;
        this.name = name;
    }

    public User(String name) {
        this.name = name;
    }

    public String getName() { return this.name; }

    public Handler getHandler() { return this.handler; }
}

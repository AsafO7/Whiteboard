package whiteboard.client;

import javafx.application.Platform;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import jdk.nashorn.internal.ir.Block;
import whiteboard.Connection;
import whiteboard.Packet;
import whiteboard.SyncQueue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OutputHandler implements Runnable {

    private Socket socket;
    private BlockingQueue<Packet> outQueue;

    public OutputHandler(Socket socket, BlockingQueue<Packet> outQueue) {
        this.socket = socket;
        this.outQueue = outQueue;
    }
    @Override
    public void run() {
        ObjectOutputStream out = null;

        /* Receive input from the server here. */
        try {
            out = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                out.writeObject(outQueue.take());
            }
        }
        catch (Exception e) { e.printStackTrace(); }
        finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

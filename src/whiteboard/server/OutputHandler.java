package whiteboard.server;

import whiteboard.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;

public class OutputHandler implements Runnable {
    private final Socket socket;
    private final BlockingQueue<Packet> outQueue;

    public OutputHandler(Socket socket, BlockingQueue<Packet> outQueue) {
        this.socket = socket;
        this.outQueue = outQueue;
    }

    @Override
    public void run() {
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());

            while (true) {
                Packet packet = outQueue.take();
                out.writeObject(packet);
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

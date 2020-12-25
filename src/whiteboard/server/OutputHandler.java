package whiteboard.server;

import whiteboard.Packet;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentLinkedQueue;

public class OutputHandler implements Runnable {
    private final ObjectOutputStream out;
    private final ConcurrentLinkedQueue<Packet> outQueue;

    public OutputHandler(ObjectOutputStream out, ConcurrentLinkedQueue<Packet> outQueue) {
        this.out = out;
        this.outQueue = outQueue;
    }

    @Override
    public void run() {
        while (true) {
            Packet packet = outQueue.remove();
            try {
                out.writeObject(packet);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

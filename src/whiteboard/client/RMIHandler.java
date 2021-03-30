/* This class takes care of outputs the user receives from the server. */

package whiteboard.client;

import whiteboard.Packet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIHandler implements Runnable {

    private BlockingQueue<Callable<Packet>> outQueue = new LinkedBlockingQueue<>();
    private InputHandler inQueue;

    public void setInputHandler(InputHandler inQueue) {
        this.inQueue = inQueue;
    }

    public void put(Callable<Packet> callable) {
        try {
            outQueue.put(callable);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Callable<Packet> callable = outQueue.take();
                try {
                    Packet packet = callable.call();
                    inQueue.put(packet);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (InterruptedException exception) {
            exception.printStackTrace();
            System.exit(-1);
        }
    }
}

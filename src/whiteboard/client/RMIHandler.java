/* This class takes care of outputs the user receives from the server. */

package whiteboard.client;

import whiteboard.Packet;

import java.rmi.RemoteException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;

public class RMIHandler implements Runnable {

    private BlockingQueue<Runnable> outQueue = new LinkedBlockingQueue<>();

    public void put(Runnable runnable) {
        try {
            outQueue.put(runnable);
        } catch (InterruptedException exception) {
            exception.printStackTrace();
            System.exit(-1);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Runnable runnable = outQueue.take();
                try {
                    runnable.run();
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

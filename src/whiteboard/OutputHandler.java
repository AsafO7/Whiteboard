/* This class takes care of outputs the user receives from the server. */

package whiteboard;

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
import java.util.concurrent.LinkedBlockingQueue;

public class OutputHandler implements Runnable {

    private BlockingQueue<Runnable> outQueue = new LinkedBlockingQueue<Runnable>();

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
                runnable.run();
            }
        } catch (InterruptedException exception) {
            exception.printStackTrace();
            System.exit(-1);
        }
    }
}

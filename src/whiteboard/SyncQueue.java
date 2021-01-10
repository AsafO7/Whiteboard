package whiteboard;

import java.util.LinkedList;
import java.util.Queue;

public class SyncQueue<T> {
    private Queue<T> queue = new LinkedList<T>();
    private boolean empty = true;

    public synchronized void add(T obj) {
        queue.add(obj);
        empty = false;
        this.notify();
    }

    public synchronized T remove() {
        while (empty) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        T obj = queue.remove();
        if (queue.isEmpty()) {
            empty = true;
        }
        return obj;
    }
}

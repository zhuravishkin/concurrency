package course.concurrency.m6_testing;

import java.util.LinkedList;

public class CustomQueue<T> {

    private final int capacity;
    private final LinkedList<T> src = new LinkedList<>();
    private int size = 0;

    private Object lock = new Object();

    public CustomQueue(int capacity) {
        this.capacity = capacity;
    }

    public void enqueue(T value) {
        synchronized (lock) {
            while (size == capacity) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            src.add(value);
            size++;
            lock.notifyAll();
        }
    }

    public T dequeue() {
        synchronized (lock) {
            while (size == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            T res = src.removeFirst();
            size--;
            lock.notifyAll();
            return res;
        }
    }

    public int getSize() {
        synchronized (lock) {
            return size;
        }
    }

    public int getCapacity() {
        return capacity;
    }
}

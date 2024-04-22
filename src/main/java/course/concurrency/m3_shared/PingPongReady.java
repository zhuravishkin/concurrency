package course.concurrency.m3_shared;

public class PingPongReady {

    public static final Object lock = new Object();
    public static volatile boolean isPing = true;

    public static void ping() {
        while (true) {
            synchronized (lock) {
                while (!isPing) {
                    try {
                        lock.wait();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println("Ping");
                isPing = false;
                lock.notify();
            }
        }
    }

    public static void pong() {
        while (true) {
            synchronized (lock) {
                while (isPing) {
                    try {
                        lock.wait();
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                System.out.println("Pong");
                isPing = true;

                lock.notify();
            }
        }
    }

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> ping());
        Thread t2 = new Thread(() -> pong());
        t1.start();
        t2.start();
    }
}

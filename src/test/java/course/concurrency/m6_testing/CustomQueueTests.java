package course.concurrency.m6_testing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomQueueTests {

    @Test
    @DisplayName("Elements should be ordered correctly for one thread")
    public void elementsShouldBeOrderedOneThread() {
        int count = 5;
        CustomQueue<Integer> queue = new CustomQueue<>(count);

        for (int i = 0; i < count; i++) {
            queue.enqueue(i);
        }

        for (int i = 0; i < count; i++) {
            Integer res = queue.dequeue();
            assertEquals(i, res);
        }
    }

    @Test
    @DisplayName("All initial elements should be retrieved")
    public void elementsShouldBeRetrieved() throws InterruptedException {
        int count = 500;
        CustomQueue<Integer> queue = new CustomQueue<>(count);

        CountDownLatch latch = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(count*2);

        // enqueue
        for (int i = 0; i < count; i++) {
            final Integer element = i;
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                queue.enqueue(element);
            });
        }
        // dequeue
        ConcurrentLinkedQueue resultQueue = new ConcurrentLinkedQueue();
        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                Integer res = queue.dequeue();
                resultQueue.add(res);
            });
        }

        latch.countDown();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(count, resultQueue.size());
        for (int i = 0; i < count; i++) {
            assertTrue(resultQueue.contains(i));
        }
    }

    @Test
    @DisplayName("Full queue should block incoming requests")
    public void shouldBlockOnPut() throws InterruptedException {
        int count = 100;
        int capacity = 2;
        CustomQueue<Integer> queue = new CustomQueue<>(capacity);

        int poolSize = capacity*3;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);

        // enqueue
        for (int i = 0; i < count; i++) {
            final Integer element = i;
            executor.submit(() -> queue.enqueue(element));
        }

        assertEquals(capacity, queue.getCapacity());
        assertEquals(capacity, queue.getSize());
        assertEquals(count, executor.getTaskCount());
        // only {capacity} tasks are done, others are blocked
        assertEquals(capacity, executor.getCompletedTaskCount());

        // check if everything works as expected after blocking

        // dequeue
        ConcurrentLinkedQueue resultQueue = new ConcurrentLinkedQueue();
        for (int i = 0; i < count; i++) {
            Integer res = queue.dequeue();
            resultQueue.add(res);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(count, resultQueue.size());
        for (int i = 0; i < count; i++) {
            assertTrue(resultQueue.contains(i));
        }
    }

    @Test
    @DisplayName("Empty queue should block on dequeue")
    public void shouldBlockOnEmpty() throws InterruptedException {
        int count = 100;
        int capacity = 2;
        CustomQueue<Integer> queue = new CustomQueue<>(capacity);

        int poolSize = capacity*3;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);

        // dequeue
        ConcurrentLinkedQueue resultQueue = new ConcurrentLinkedQueue();
        for (int i = 0; i < count; i++) {
            executor.submit(() -> {
                Integer res = queue.dequeue();
                resultQueue.add(res);
            });
        }

        assertEquals(capacity, queue.getCapacity());
        assertEquals(0, queue.getSize());
        assertEquals(0, executor.getCompletedTaskCount());
        assertEquals(count, executor.getTaskCount());

        // check if everything works as expected after blocking

        // enqueue
        for (int i = 0; i < count; i++) {
            final Integer element = i;
            queue.enqueue(element);
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);

        assertEquals(count, resultQueue.size());
        for (int i = 0; i < count; i++) {
            assertTrue(resultQueue.contains(i));
        }
    }
}
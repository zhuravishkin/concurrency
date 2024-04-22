package course.concurrency.exams.auction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotifierUpdated extends Notifier {

    private ExecutorService executor = Executors.newSingleThreadExecutor();

    public void sendOutdatedMessage(Bid bid) {
        executor.submit(() -> imitateSending());
    }

    private void imitateSending() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {}
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}

package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimisticReady implements Auction {
    private final Notifier notifier;

    private final AtomicReference<Bid> latestBid;
    public AuctionOptimisticReady(Notifier notifier) {
        latestBid = new AtomicReference<>(new Bid(-1l, -1l, -1l));
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid currentBid;
        do {
            currentBid = latestBid.get();
            if (bid.getPrice() <= currentBid.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(currentBid, bid));

        notifier.sendOutdatedMessage(currentBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}

package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimisticReady implements AuctionStoppable {

    private final AtomicMarkableReference<Bid> latestBid;
    private final Notifier notifier;

    public AuctionStoppableOptimisticReady(Notifier notifier) {
        latestBid = new AtomicMarkableReference<>(new Bid(-1l, -1l, -1l), false);
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid currentBid;
        do {
            if (latestBid.isMarked()) {
                return false;
            }
            currentBid = latestBid.getReference();
            if (currentBid != null && bid.getPrice() <= currentBid.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(currentBid, bid, false, false));

        notifier.sendOutdatedMessage(currentBid);

        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        // to prevent multiple stop actions and incorrect latestBid value
        if (latestBid.isMarked()) {
            return latestBid.getReference();
        }
        Bid latest = latestBid.getReference();
        latestBid.set(latest, true);
        return latest;
    }
}

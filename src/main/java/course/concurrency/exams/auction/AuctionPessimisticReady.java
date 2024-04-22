package course.concurrency.exams.auction;

public class AuctionPessimisticReady implements Auction {

    private final Notifier notifier;

    // volatile is used to return correct getLatestBid() result.
    // Alternative: plain variable + synchronized getLatestBid
    private volatile Bid latestBid = new Bid(-1L, -1L, -1L);

    private final Object lock = new Object();

    public AuctionPessimisticReady(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        // this condition filter bids with lower price and seriously reduce contention
        if (bid.getPrice() > latestBid.getPrice()) {
            synchronized (lock) {
                // double-check is required to prevent data races
                if (bid.getPrice() > latestBid.getPrice()) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            }
        }

        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }

}

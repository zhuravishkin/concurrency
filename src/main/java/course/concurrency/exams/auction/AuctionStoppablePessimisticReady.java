package course.concurrency.exams.auction;

public class AuctionStoppablePessimisticReady implements AuctionStoppable {

    private final Notifier notifier;

    private volatile Bid latestBid = new Bid(-1L, -1L, -1L);
    private volatile boolean isOpen = true;

    private final Object lock = new Object();

    public AuctionStoppablePessimisticReady(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        // this condition filter bids with lower price and seriously reduce contention
        if (isOpen && (bid.getPrice() > latestBid.getPrice())) {
            synchronized (lock) {
                if (isOpen && (bid.getPrice() > latestBid.getPrice())) {
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

    public Bid stopAuction() {
        // these block prevents situation when synchronized block in line 19 is passed and isOpen is changed immediately
        // actually, it can be omitted in most cases as requirements are rarely so strict
        synchronized (lock) {
            isOpen = false;
            return latestBid;
        }
    }
}

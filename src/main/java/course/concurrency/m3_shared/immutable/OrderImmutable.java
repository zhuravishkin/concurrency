package course.concurrency.m3_shared.immutable;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class OrderImmutable {

    public enum Status {NEW, IN_PROGRESS, DELIVERED}

    private static final AtomicLong nextId = new AtomicLong();

    private final Long id;
    private final List<Item> items;
    private final PaymentInfo paymentInfo;
    private final boolean isPacked;
    private final Status status;

    public OrderImmutable(List<Item> items) {
        this(nextId.incrementAndGet(), items, null, false, Status.NEW);
    }

    public Long getId() {
        return id;
    }

    private OrderImmutable(Long id, List<Item> items, PaymentInfo paymentInfo, boolean isPacked, Status status) {
        this.id = id;
        this.items = Collections.unmodifiableList(items);
        this.paymentInfo = paymentInfo;
        this.isPacked = isPacked;
        this.status = status;
    }

    public OrderImmutable withStatus(Status status) {
        return new OrderImmutable(this.id, this.items, this.paymentInfo, this.isPacked, status);
    }

    public OrderImmutable withPaymentInfo(PaymentInfo paymentInfo) {
        return new OrderImmutable(this.id, this.items, paymentInfo, this.isPacked, this.status);
    }

    public OrderImmutable packed() {
        return new OrderImmutable(this.id, this.items, this.paymentInfo, true, this.status);

    }

    public boolean checkStatus() {
        if (paymentInfo != null && isPacked) {
            return true;
        }
        return false;
    }

    public List<Item> getItems() {
        return items;
    }

    public Status getStatus() {
        return status;
    }
}
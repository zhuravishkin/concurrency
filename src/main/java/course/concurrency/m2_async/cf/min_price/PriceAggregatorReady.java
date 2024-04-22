package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.stream.Collectors.toList;

public class PriceAggregatorReady extends PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private ExecutorService executor = Executors.newCachedThreadPool(); // to handle all blocking requests

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {
        List<CompletableFuture<Double>> completableFutureList =
               shopIds.stream().map(shopId ->
                       CompletableFuture.supplyAsync(
                            () -> priceRetriever.getPrice(itemId, shopId), executor)
                            .completeOnTimeout(Double.POSITIVE_INFINITY, 2900, TimeUnit.MILLISECONDS)
                            .exceptionally(ex -> Double.POSITIVE_INFINITY))
                       .collect(toList());

        CompletableFuture
                .allOf(completableFutureList.toArray(CompletableFuture[]::new))
                .join();

        return completableFutureList
                .stream()
                .mapToDouble(CompletableFuture::join)
                .filter(Double::isFinite)
                .min()
                .orElse(Double.NaN);
    }
}

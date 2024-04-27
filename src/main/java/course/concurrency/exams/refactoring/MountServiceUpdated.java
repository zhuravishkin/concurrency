package course.concurrency.exams.refactoring;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static java.util.stream.Collectors.toList;

public class MountServiceUpdated {

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;

    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit()  {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = r -> {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);

        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    public void refresh()  {
        List<Others.RouterState> cachedRecords = routerStore.getCachedRecords();
        List<UpdateTask> refreshTasks = new ArrayList<>();
        for (Others.RouterState routerState : cachedRecords) {
            String adminAddress = routerState.getAdminAddress();
            if (adminAddress == null || adminAddress.length() == 0) {
                continue;
            }
            UpdateTask task = getUpdateTask(adminAddress);
            refreshTasks.add(task);
        }
        if (!refreshTasks.isEmpty()) {
            invokeRefresh(refreshTasks);
        }
    }

    protected UpdateTask getUpdateTask(String address) {
        if (isLocalAdmin(address)) {
            return getLocalRefresher(address);
        } else {
            return new UpdateTask(new Others.MountTableManager(address), address);
        }
    }
    protected UpdateTask getLocalRefresher(String adminAddress) {
        return new UpdateTask(new Others.MountTableManager("local"), adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<UpdateTask> updateTasks) {
        List<CompletableFuture<Void>> res = updateTasks.stream().map(
                    task ->
                    CompletableFuture.runAsync(() -> task.process())
                    .completeOnTimeout(null, cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                    .exceptionally(ex -> {
                        log(ex.toString());
                        return null;
                    }
                )).collect(toList());

        CompletableFuture.allOf(res.toArray(CompletableFuture[]::new)).join();
        logResult(updateTasks);
    }

    private boolean isLocalAdmin(String adminAddress) {
        return adminAddress.contains("local");
    }

    private void logResult(List<UpdateTask> tasks) {
        int successCount = 0;
        int failureCount = 0;
        for (UpdateTask task : tasks) {
            if (task.isDone()) {
                successCount++;
            } else {
                failureCount++;
                removeFromCache(task.getAdminAddress());
            }
        }
        if (failureCount != 0) {
            log("Not all router admins updated their cache");
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}

package course.concurrency.exams.refactoring;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.stream.Collectors.toList;
import static org.mockito.Mockito.*;

public class MountTableRefresherServiceUpdatedTests {

    private MountServiceUpdated service;

    private Others.RouterStore routerStore;
    private Others.MountTableManager manager;
    private Others.LoadingCache routerClientsCache;

    @BeforeEach
    public void setUpStreams() {
        service = new MountServiceUpdated();
        service.setCacheUpdateTimeout(1000);
        routerStore = mock(Others.RouterStore.class);
        manager = mock(Others.MountTableManager.class);
        service.setRouterStore(routerStore);
        routerClientsCache = mock(Others.LoadingCache.class);
        service.setRouterClientsCache(routerClientsCache);
        // service.serviceInit(); // needed for complex class testing, not for now
    }

    @AfterEach
    public void restoreStreams() {
        // service.serviceStop();
    }

    @Test
    @DisplayName("All tasks are completed successfully")
    public void allDone() {
        MountServiceUpdated mockedService = Mockito.spy(service);
        when(manager.refresh()).thenReturn(true);

        List<String> addresses = List.of("123", "local6", "789", "local");
        List<UpdateTask> tasks = addresses.stream().map(addr -> new UpdateTask(manager, addr)).collect(toList());

        AtomicInteger taskIndex = new AtomicInteger(0);
        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Mount table entries cache refresh successCount=4,failureCount=0");
        verify(routerClientsCache, never()).invalidate(anyString());
    }

    @Test
    @DisplayName("All tasks failed")
    public void noSuccessfulTasks() {
        MountServiceUpdated mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");
        List<UpdateTask> tasks = addresses.stream().map(a -> new UpdateTask(
                manager, a
        )).collect(toList());
        AtomicInteger taskIndex = new AtomicInteger(0);
        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

        when(manager.refresh()).thenReturn(false);
        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=0,failureCount=4");
        addresses.forEach(addr -> verify(routerClientsCache).invalidate(addr));
    }

    @Test
    @DisplayName("Some tasks failed")
    public void halfSuccessedTasks() {
        MountServiceUpdated mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");

        Others.MountTableManager managerSucceed = mock(Others.MountTableManager.class);
        Others.MountTableManager managerFailed = mock(Others.MountTableManager.class);
        when(managerFailed.refresh()).thenReturn(false);
        when(managerSucceed.refresh()).thenReturn(true);

        List<UpdateTask> tasks = new ArrayList<>();
        tasks.add(new UpdateTask(managerSucceed, addresses.get(0)));
        tasks.add(new UpdateTask(managerFailed, addresses.get(1)));
        tasks.add(new UpdateTask(managerSucceed, addresses.get(2)));
        tasks.add(new UpdateTask(managerFailed, addresses.get(3)));

        AtomicInteger taskIndex = new AtomicInteger(0);
        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=2,failureCount=2");
        verify(routerClientsCache).invalidate(addresses.get(1));
        verify(routerClientsCache).invalidate(addresses.get(3));
    }

    @Test
    @DisplayName("One task completed with exception")
    public void exceptionInOneTask() {
        MountServiceUpdated mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");

        Others.MountTableManager managerSucceed = mock(Others.MountTableManager.class);
        Others.MountTableManager managerExceptioned = mock(Others.MountTableManager.class);
        when(managerExceptioned.refresh()).thenThrow(new RuntimeException());
        when(managerSucceed.refresh()).thenReturn(true);

        List<UpdateTask> tasks = new ArrayList<>();
        tasks.add(new UpdateTask(managerSucceed, addresses.get(0)));
        tasks.add(new UpdateTask(managerExceptioned, addresses.get(1)));
        tasks.add(new UpdateTask(managerSucceed, addresses.get(2)));
        tasks.add(new UpdateTask(managerSucceed, addresses.get(3)));

        AtomicInteger taskIndex = new AtomicInteger(0);
        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("java.util.concurrent.CompletionException: java.lang.RuntimeException");
        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache).invalidate(addresses.get(1));
    }

    @Test
    @DisplayName("One task exceeds timeout")
    public void oneTaskExceedTimeout() {
        MountServiceUpdated mockedService = Mockito.spy(service);

        List<String> addresses = List.of("123", "local6", "789", "local");

        Others.MountTableManager managerSucceed = mock(Others.MountTableManager.class);
        Others.MountTableManager managerTimeout = mock(Others.MountTableManager.class);
        when(managerTimeout.refresh()).thenAnswer(inv -> {Thread.sleep(2000); return true;});
        when(managerSucceed.refresh()).thenReturn(true);

        List<UpdateTask> tasks = new ArrayList<>();
        tasks.add(new UpdateTask(managerSucceed, addresses.get(0)));
        tasks.add(new UpdateTask(managerTimeout, addresses.get(1)));
        tasks.add(new UpdateTask(managerSucceed, addresses.get(2)));
        tasks.add(new UpdateTask(managerSucceed, addresses.get(3)));

        AtomicInteger taskIndex = new AtomicInteger(0);
        when(mockedService.getUpdateTask(anyString())).thenAnswer(inv -> tasks.get(taskIndex.getAndIncrement()));

        List<Others.RouterState> states = addresses.stream()
                .map(a -> new Others.RouterState(a)).collect(toList());
        when(routerStore.getCachedRecords()).thenReturn(states);

        mockedService.refresh();

        verify(mockedService).log("Not all router admins updated their cache");
        verify(mockedService).log("Mount table entries cache refresh successCount=3,failureCount=1");
        verify(routerClientsCache).invalidate(addresses.get(1));
    }

}

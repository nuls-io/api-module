package io.nuls.api.task;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ScheduleManager {

    private ScheduledThreadPoolExecutor executorService;

    private static ScheduleManager instance = new ScheduleManager();

    private ScheduleManager() {
    }

    public static ScheduleManager getInstance() {
        return instance;
    }

    public void start() {
        executorService.scheduleAtFixedRate(new SyncBlockTask(),1,10, TimeUnit.SECONDS);
    }
}

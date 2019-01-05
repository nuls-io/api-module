package io.nuls.api.task;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.mongodb.MongoDBService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduleManager {

    private ScheduledExecutorService executorService;

    @Autowired
    private SyncBlockTask syncBlockTask;
    @Autowired
    private MongoDBService mongoDBService;

    public void start() {

        mongoDBService.dropTable(MongoTableName.BLOCK_HEADER);
        executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(syncBlockTask,1,10, TimeUnit.SECONDS);
    }
}

package io.nuls.api.task;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.model.Indexes;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.mongodb.MongoDBService;
import org.bson.Document;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class ScheduleManager {

    private ScheduledExecutorService executorService;

    @Autowired
    private SyncBlockTask syncBlockTask;

    @Autowired
    private StatisticalTask statisticalTask;

    @Autowired
    private MongoDBService mongoDBService;

    public void start(boolean clearDB) {
        if (clearDB) {
            mongoDBService.dropTable(MongoTableName.NEW_INFO);
            mongoDBService.dropTable(MongoTableName.BLOCK_HEADER);
            mongoDBService.dropTable(MongoTableName.AGENT_INFO);
            mongoDBService.dropTable(MongoTableName.ALIAS_INFO);
            mongoDBService.dropTable(MongoTableName.ACCOUNT_INFO);
            mongoDBService.dropTable(MongoTableName.DEPOSIT_INFO);
            mongoDBService.dropTable(MongoTableName.TX_RELATION_INFO);
            mongoDBService.dropTable(MongoTableName.TX_INFO);
            mongoDBService.dropTable(MongoTableName.PUNISH_INFO);
            mongoDBService.dropTable(MongoTableName.UTXO_INFO);
            mongoDBService.dropTable(MongoTableName.ROUND_INFO);
            mongoDBService.dropTable(MongoTableName.ROUND_ITEM_INFO);
            mongoDBService.dropTable(MongoTableName.STATISTICAL_INFO);
            mongoDBService.dropTable(MongoTableName.ACCOUNT_TOKEN_INFO);
            mongoDBService.dropTable(MongoTableName.CONTRACT_INFO);
            mongoDBService.dropTable(MongoTableName.CONTRACT_TX_INFO);
            mongoDBService.dropTable(MongoTableName.TOKEN_TRANSFER_INFO);
            mongoDBService.dropTable(MongoTableName.CONTRACT_RESULT_INFO);
        }

        initTables();

        executorService = Executors.newScheduledThreadPool(2);
        executorService.scheduleAtFixedRate(syncBlockTask, 1, 10, TimeUnit.SECONDS);


        executorService.scheduleAtFixedRate(statisticalTask, 1, 60, TimeUnit.MINUTES);
    }

    private void initTables() {
        //判断是否需要创建collections，已存在则返回
        ListIndexesIterable<Document> indexes = mongoDBService.getIndexes(MongoTableName.TX_RELATION_INFO);
        if (null != indexes && indexes.iterator().hasNext()) {
            return;
        }
        //创建collections
        mongoDBService.createCollection(MongoTableName.TX_RELATION_INFO);

        //创建索引
        mongoDBService.createIndex(MongoTableName.TX_RELATION_INFO, Indexes.ascending("address,type"));

    }
}

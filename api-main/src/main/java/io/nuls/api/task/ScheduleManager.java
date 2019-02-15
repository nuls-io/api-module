package io.nuls.api.task;

import com.mongodb.client.ListIndexesIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Indexes;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.mongodb.MongoDBService;
import org.bson.Document;

import java.util.Iterator;
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
    private StatisticalNulsTask statisticalNulsTask;

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
            mongoDBService.dropTable(MongoTableName.COINDATA_INFO);
        }

        initTables();

        executorService = Executors.newScheduledThreadPool(3);
        executorService.scheduleAtFixedRate(syncBlockTask, 1, 10, TimeUnit.SECONDS);
        executorService.scheduleAtFixedRate(statisticalNulsTask, 1, 20, TimeUnit.MINUTES);
        executorService.scheduleAtFixedRate(statisticalTask, 1, 60, TimeUnit.MINUTES);

    }

    private void initTables() {
        //判断是否需要创建索引
        //交易关系表
        //判断是否需要创建collections，已存在则返回
        ListIndexesIterable<Document> indexes = mongoDBService.getIndexes(MongoTableName.TX_RELATION_INFO);
        MongoCursor<Document> iterator = indexes.iterator();
        if (!iterator.hasNext() || (iterator.next() != null && !iterator.hasNext())) {
            if (!iterator.hasNext()) {
                mongoDBService.createCollection(MongoTableName.TX_RELATION_INFO);
            }
            //创建索引
            mongoDBService.createIndex(MongoTableName.TX_RELATION_INFO, Indexes.ascending("type", "address", "txHash"));
        }
        //账户信息表
        indexes = mongoDBService.getIndexes(MongoTableName.ACCOUNT_INFO);
        iterator = indexes.iterator();
        if (!iterator.hasNext() || (iterator.next() != null && !iterator.hasNext())) {
            if (!iterator.hasNext()) {
                mongoDBService.createCollection(MongoTableName.ACCOUNT_INFO);
            }
            mongoDBService.createIndex(MongoTableName.ACCOUNT_INFO, Indexes.descending("totalBalance"));
        }
//        //交易表
        indexes = mongoDBService.getIndexes(MongoTableName.TX_INFO);
        iterator = indexes.iterator();
        if (!iterator.hasNext() || (iterator.next() != null && !iterator.hasNext())) {
            if (!iterator.hasNext()) {
                mongoDBService.createCollection(MongoTableName.TX_INFO);
            }
            mongoDBService.createIndex(MongoTableName.TX_INFO, Indexes.descending("height", "createTime"));
        }
        //UTXO 表
        indexes = mongoDBService.getIndexes(MongoTableName.UTXO_INFO);
        iterator = indexes.iterator();
        if (!iterator.hasNext() || (iterator.next() != null && !iterator.hasNext())) {
            if (!iterator.hasNext()) {
                mongoDBService.createCollection(MongoTableName.UTXO_INFO);
            }
            mongoDBService.createIndex(MongoTableName.UTXO_INFO, Indexes.ascending("address"));
        }
    }
}

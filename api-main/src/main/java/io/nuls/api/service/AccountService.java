package io.nuls.api.service;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AccountInfo;
import io.nuls.api.core.model.Output;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.model.TxRelationInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.api.core.util.Log;
import io.nuls.api.utils.CalcUtil;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

@Component
public class AccountService {

    private static final Map<String, AccountInfo> accountMap = new HashMap<>();

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private UTXOService utxoService;
    @Autowired
    private BlockHeaderService blockHeaderService;

    private void initCache() {
        if (!accountMap.isEmpty()) {
            return;
        }
        List<Document> documentList = mongoDBService.query(MongoTableName.ACCOUNT_INFO);
        for (Document document : documentList) {
            AccountInfo accountInfo = DocumentTransferTool.toInfo(document, "address", AccountInfo.class);
            accountMap.put(accountInfo.getAddress(), accountInfo);
        }
    }

    public void saveAccounts(Map<String, AccountInfo> accountInfoMap) {
        initCache();
        if (accountInfoMap.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AccountInfo accountInfo : accountInfoMap.values()) {
            accountMap.put(accountInfo.getAddress(), accountInfo);
            Document document = DocumentTransferTool.toDocument(accountInfo, "address");
            if (accountInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
                accountInfo.setNew(false);
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", accountInfo.getAddress()), document));
            }
        }
        mongoDBService.bulkWrite(MongoTableName.ACCOUNT_INFO, modelList);
    }

    public AccountInfo getAccountInfo(String address) {
        initCache();
        return accountMap.get(address);
//        Document document = mongoDBService.findOne(MongoTableName.ACCOUNT_INFO, Filters.eq("_id", address));
//        if (document == null) {
//            return null;
//        }
//        AccountInfo accountInfo = DocumentTransferTool.toInfo(document, "address", AccountInfo.class);
//        return accountInfo;
    }

    public PageInfo<AccountInfo> pageQuery(int pageNumber, int pageSize) {
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.ACCOUNT_INFO, pageNumber, pageSize);
        List<AccountInfo> accountInfoList = new ArrayList<>();
        long totalCount = mongoDBService.getCount(MongoTableName.ACCOUNT_INFO);
        for (Document document : docsList) {
            accountInfoList.add(DocumentTransferTool.toInfo(document, "address", AccountInfo.class));
        }
        PageInfo<AccountInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, accountInfoList);
        return pageInfo;
    }

    public PageInfo<TxRelationInfo> getAccountTxs(String address, int pageIndex, int pageSize, int type, boolean isMark) {
        Bson filter = null;
        Bson addressFilter = Filters.eq("address", address);

        if (type == 0 && isMark) {
            filter = Filters.and(addressFilter, Filters.ne("type", 1));
        } else if (type > 0) {
            filter = Filters.and(addressFilter, Filters.eq("type", type));
        } else {
            filter = addressFilter;
        }
//        long start = System.currentTimeMillis();
        long totalCount = mongoDBService.getCount(MongoTableName.TX_RELATION_INFO, filter);
//        Log.info("count use:{}ms",System.currentTimeMillis()-start);
//        start = System.currentTimeMillis();
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.TX_RELATION_INFO, filter, Sorts.descending("height", "createTime"), pageIndex, pageSize);
//        Log.info("query use:{}ms",System.currentTimeMillis()-start);
        List<TxRelationInfo> txRelationInfoList = new ArrayList<>();
        for (Document document : docsList) {
            txRelationInfoList.add(DocumentTransferTool.toInfo(document, TxRelationInfo.class));
        }
        PageInfo<TxRelationInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, txRelationInfoList);
        return pageInfo;
    }

    public PageInfo<AccountInfo> getCoinRanking(int pageIndex, int pageSize, int sortType) {
        Bson sort;
        if (sortType == 0) {
            sort = Sorts.descending("totalBalance");
        } else {
            sort = Sorts.ascending("totalBalance");
        }
        List<AccountInfo> accountInfoList = new ArrayList<>();
        Bson filter = Filters.gt("totalBalance", 0);
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.ACCOUNT_INFO, filter, sort, pageIndex, pageSize);
        long totalCount = mongoDBService.getCount(MongoTableName.ACCOUNT_INFO, filter);
        for (Document document : docsList) {
            AccountInfo accountInfo = DocumentTransferTool.toInfo(document, "address", AccountInfo.class);
//            List<Output> outputs = utxoService.getAccountUtxos(accountInfo.getAddress());
//            CalcUtil.calcBalance(accountInfo, outputs, blockHeaderService.getBestBlockHeight());
            accountInfoList.add(accountInfo);
        }
        PageInfo<AccountInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, accountInfoList);
        return pageInfo;
    }

    public long getAllAccountBalance() {
        MongoCollection<Document> collection = mongoDBService.getCollection(MongoTableName.ACCOUNT_INFO);
        AggregateIterable<Document> ai = collection.aggregate(Arrays.asList(
                Aggregates.group(null, Accumulators.sum("total", "$totalBalance"))
        ));
        MongoCursor<Document> cursor = ai.iterator();
        long totalBalance = 0;
        while (cursor.hasNext()) {
            totalBalance = cursor.next().getLong("total");
        }
        return totalBalance;
    }

    public long getAccountTotalBalance(String address) {
        AccountInfo accountInfo = getAccountInfo(address);
        if (accountInfo == null) {
            return 0;
        }
        return accountInfo.getTotalBalance();
    }

    public long getAccountLockBalance(String address) {
        return getAccountInfo(address).getTimeLock();
    }
}

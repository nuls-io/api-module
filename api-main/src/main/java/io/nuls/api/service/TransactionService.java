package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.api.core.model.TxRelationInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.*;

@Component
public class TransactionService {

    @Autowired
    private MongoDBService mongoDBService;


    public void saveTxRelationList(Set<TxRelationInfo> relationInfos) {
        if (relationInfos.isEmpty()) {
            return;
        }

        List<Document> documentList = new ArrayList<>();
        for (TxRelationInfo relationInfo : relationInfos) {
            Document document = DocumentTransferTool.toDocument(relationInfo);
            documentList.add(document);
        }

        mongoDBService.insertMany(MongoTableName.TX_RELATION_INFO, documentList);
    }


    public void saveTxList(List<TransactionInfo> txList) {
        List<Document> documentList = new ArrayList<>();
        for (TransactionInfo transactionInfo : txList) {
            transactionInfo.calcValue();
            documentList.add(transactionInfo.toDocument());
        }
        mongoDBService.insertMany(MongoTableName.TX_INFO, documentList);
    }

    public PageInfo<TransactionInfo> getTxList(int pageIndex, int pageSize, int type, boolean isHidden) {
        Bson filter = null;
        if (type > 0) {
            filter = eq("type", type);
        } else if (isHidden) {
            filter = ne("type", 1);
        }
        long totalCount = mongoDBService.getCount(MongoTableName.TX_INFO, filter);
        List<Document> docList = this.mongoDBService.pageQuery(MongoTableName.TX_INFO, filter, Sorts.descending("height", "time"), pageIndex, pageSize);
        List<TransactionInfo> txList = new ArrayList<>();
        for (Document document : docList) {
            txList.add(TransactionInfo.fromDocument(document));
        }

        PageInfo<TransactionInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, txList);
        return pageInfo;
    }

    public PageInfo<TransactionInfo> getBlockTxList(int pageIndex, int pageSize, long blockHeight, int type) {
        Bson filter = null;
        if (type == 0) {
            filter = eq("height", blockHeight);
        } else {
            filter = and(eq("type", type), eq("height", blockHeight));
        }
        long totalCount = mongoDBService.getCount(MongoTableName.TX_INFO, filter);
        List<Document> docList = this.mongoDBService.pageQuery(MongoTableName.TX_INFO, filter, Sorts.descending("height", "time"), pageIndex, pageSize);
        List<TransactionInfo> txList = new ArrayList<>();
        for (Document document : docList) {
            txList.add(TransactionInfo.fromDocument(document));
        }

        PageInfo<TransactionInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, txList);
        return pageInfo;
    }

    public TransactionInfo getTx(String hash) {
        Document document = mongoDBService.findOne(MongoTableName.TX_INFO, eq("_id", hash));
        if (null == document) {
            return null;
        }
        return TransactionInfo.fromDocument(document);
    }
}

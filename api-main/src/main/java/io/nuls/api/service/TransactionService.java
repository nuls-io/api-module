package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.api.core.model.TxRelationInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

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


    public List<TransactionInfo> getTxList(int pageIndex, int pageSize, int type, boolean includeCoinBase) {
        Bson filter1 = null;
        Bson filter2 = null;
        Bson filter = null;
        if (type > 0) {
            filter1 = eq("type", type);
        }
        if (!includeCoinBase || type == 0) {
            filter2 = Filters.ne("type", 1);
        }
        if (filter1 != null && filter2 != null) {
            filter = and(filter1, filter2);
        } else if (filter1 != null) {
            filter = filter1;
        } else if (filter2 != null) {
            filter = filter2;
        }

        List<Document> docList = this.mongoDBService.pageQuery(MongoTableName.TX_INFO, filter, Sorts.descending("height", "time"), pageIndex, pageSize);

        List<TransactionInfo> txList = new ArrayList<>();

        for (Document document : docList) {
            txList.add(DocumentTransferTool.toInfo(document, TransactionInfo.class));
        }
        return txList;
    }
}

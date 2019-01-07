package io.nuls.api.service;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.api.core.model.TxRelationInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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


}

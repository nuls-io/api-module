package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.UpdateManyModel;
import com.mongodb.client.model.WriteModel;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.DepositInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

@Component
public class DepositService {

    @Autowired
    private MongoDBService mongoDBService;


    public DepositInfo getDepositInfoByHash(String hash) {
        Document document = mongoDBService.findOne(MongoTableName.DEPOSIT_INFO, Filters.eq("_id", hash));
        if (document == null) {
            return null;
        }
        DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "txHash", DepositInfo.class);
        return depositInfo;
    }


    public List<DepositInfo> getDepositListByAgentHash(String hash) {
        List<DepositInfo> depositInfos = new ArrayList<>();
        Bson bson = Filters.and(Filters.eq("agentHash", hash), Filters.eq("deleteHash", null), Filters.eq("type", 0));
        List<Document> documentList = mongoDBService.query(MongoTableName.DEPOSIT_INFO, bson);
        if (documentList == null && documentList.isEmpty()) {
            return depositInfos;
        }
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "txHash", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        return depositInfos;
    }


    public void saveDepsoitList(List<DepositInfo> depositInfoList) {
        if (depositInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();

        for (DepositInfo depositInfo : depositInfoList) {
            Document document = DocumentTransferTool.toDocument(depositInfo, "txHash");
            document.remove("isNew");
            if (depositInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new UpdateManyModel(Filters.eq("_id", depositInfo.getTxHash()), document));
            }
        }

        mongoDBService.bulkWrite(MongoTableName.DEPOSIT_INFO, modelList);
    }
}

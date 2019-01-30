package io.nuls.api.service;

import com.mongodb.client.model.*;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.DepositInfo;
import io.nuls.api.core.model.PageInfo;
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
        Document document = mongoDBService.findOne(MongoTableName.DEPOSIT_INFO, Filters.eq("txHash", hash));
        if (document == null) {
            return null;
        }
        DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
        return depositInfo;
    }


    public DepositInfo getDepositInfoByKey(String key) {
        Document document = mongoDBService.findOne(MongoTableName.DEPOSIT_INFO, Filters.eq("_id", key));
        if (document == null) {
            return null;
        }
        DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
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
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        return depositInfos;
    }

    public PageInfo<DepositInfo> getDepositListByAgentHash(String hash, int pageIndex, int pageSize) {
        Bson bson = Filters.and(Filters.eq("agentHash", hash), Filters.eq("deleteHash", null));
        List<Document> documentList = mongoDBService.pageQuery(MongoTableName.DEPOSIT_INFO, bson, Sorts.descending("createTime"), pageIndex, pageSize);
        long totalCount = mongoDBService.getCount(MongoTableName.DEPOSIT_INFO, bson);

        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        PageInfo<DepositInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, depositInfos);
        return pageInfo;
    }

    public List<DepositInfo> getDepositListByHash(String hash) {
        Bson bson = Filters.and(Filters.eq("txHash", hash));
        List<Document> documentList = mongoDBService.query(MongoTableName.DEPOSIT_INFO, bson);

        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        return depositInfos;
    }


    public PageInfo<DepositInfo> getCancelDepositListByAgentHash(String hash, int type, int pageIndex, int pageSize) {
        Bson bson;
        if (type != 2) {
            bson = Filters.and(Filters.eq("agentHash", hash), Filters.eq("type", type));
        } else {
            bson = Filters.eq("agentHash", hash);
        }
        List<Document> documentList = mongoDBService.pageQuery(MongoTableName.DEPOSIT_INFO, bson, Sorts.descending("createTime"), pageIndex, pageSize);
        long totalCount = mongoDBService.getCount(MongoTableName.DEPOSIT_INFO, bson);

        List<DepositInfo> depositInfos = new ArrayList<>();
        for (Document document : documentList) {
            DepositInfo depositInfo = DocumentTransferTool.toInfo(document, "key", DepositInfo.class);
            depositInfos.add(depositInfo);
        }
        PageInfo<DepositInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, depositInfos);
        return pageInfo;
    }

    public void saveDepositList(List<DepositInfo> depositInfoList) {
        if (depositInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();

        for (DepositInfo depositInfo : depositInfoList) {
            Document document = DocumentTransferTool.toDocument(depositInfo, "key");
            if (depositInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", depositInfo.getKey()), document));
            }
        }

        mongoDBService.bulkWrite(MongoTableName.DEPOSIT_INFO, modelList);
    }

    public void rollbackDepoist(List<DepositInfo> depositInfoList) {
        if (depositInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (DepositInfo depositInfo : depositInfoList) {

            if (depositInfo.isNew()) {
                modelList.add(new DeleteOneModel<>(Filters.eq("_id", depositInfo.getKey())));
            } else {
                Document document = DocumentTransferTool.toDocument(depositInfo);
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", depositInfo.getKey()), document));
            }
        }

        mongoDBService.bulkWrite(MongoTableName.DEPOSIT_INFO, modelList);
    }

    public List<DepositInfo> getDepositList(long startHeight) {
        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.eq("type", 0), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));

        List<Document> list = this.mongoDBService.query(MongoTableName.DEPOSIT_INFO, bson);
        List<DepositInfo> resultList = new ArrayList<>();
        for (Document document : list) {
            resultList.add(DocumentTransferTool.toInfo(document, "key", DepositInfo.class));
        }

        return resultList;
    }
}

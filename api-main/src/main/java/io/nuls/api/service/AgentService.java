package io.nuls.api.service;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.client.model.*;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AgentInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

@Component
public class AgentService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AliasService aliasService;


    public AgentInfo getAgentByPackingAddress(String packingAddress) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("packingAddress", packingAddress));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, AgentInfo.class);
        return agentInfo;
    }

    public AgentInfo getAgentByAgentAddress(String agentAddress) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("agentAddress", agentAddress));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, AgentInfo.class);
        return agentInfo;
    }

    public AgentInfo getAgentByAgentId(String agentId) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("_id", agentId));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        return agentInfo;
    }

    public AgentInfo getAgentByAgentHash(String agentHash) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("txHash", agentHash));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        return agentInfo;
    }

    /**
     * @param address agentAddress or packingAddress
     * @return
     */
    public AgentInfo getAgentByAddress(String address) {
        Bson bson = Filters.or(Filters.eq("agentAddress", address), Filters.eq("packingAddress", address));
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, bson);
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, AgentInfo.class);
        return agentInfo;
    }

    public void saveAgentList(List<AgentInfo> agentInfoList) {
        if (agentInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfoList) {
            Document document = DocumentTransferTool.toDocument(agentInfo, "agentId");

            if (agentInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", agentInfo.getAgentId()), document));
            }
        }
        mongoDBService.bulkWrite(MongoTableName.AGENT_INFO, modelList);
    }

    public List<AgentInfo> getAgentList(long startHeight) {
        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));

        List<Document> list = this.mongoDBService.query(MongoTableName.AGENT_INFO, bson);
        List<AgentInfo> resultList = new ArrayList<>();
        for (Document document : list) {
            resultList.add(DocumentTransferTool.toInfo(document, AgentInfo.class));
        }

        return resultList;
    }
}

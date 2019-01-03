package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AgentInfo;
import io.nuls.api.core.model.AliasInfo;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

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
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agent_id", AgentInfo.class);
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

    public void saveAgent(TransactionInfo txInfo) {
        AgentInfo agentInfo = (AgentInfo) txInfo.getTxData();
        //查询agent节点是否设置过别名
        AliasInfo aliasInfo = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (aliasInfo != null) {
            agentInfo.setAgentAlias(aliasInfo.getAlias());
        }
        Document document = DocumentTransferTool.toDocument(agentInfo, "agentId");
        mongoDBService.insertOne(MongoTableName.AGENT_INFO, document);
    }
}

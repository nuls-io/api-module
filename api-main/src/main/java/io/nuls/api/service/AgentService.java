package io.nuls.api.service;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.AccountInfo;
import io.nuls.api.core.model.AgentInfo;
import io.nuls.api.core.model.AliasInfo;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.core.model.Agent;
import io.nuls.sdk.core.model.Alias;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class AgentService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AliasService aliasService;
    @Autowired
    private AccountService accountService;

    public AgentInfo getAgentByPackingAddress(String packingAddress) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("packingAddress", packingAddress));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (alias != null) {
            agentInfo.setAgentAlias(alias.getAlias());
        }
        return agentInfo;
    }

    public AgentInfo getAgentByAgentAddress(String agentAddress) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("agentAddress", agentAddress));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (alias != null) {
            agentInfo.setAgentAlias(alias.getAlias());
        }
        return agentInfo;
    }

    public AgentInfo getAgentByAgentId(String agentId) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("_id", agentId));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (alias != null) {
            agentInfo.setAgentAlias(alias.getAlias());
        }
        return agentInfo;
    }

    public AgentInfo getAgentByAgentHash(String agentHash) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("txHash", agentHash));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (alias != null) {
            agentInfo.setAgentAlias(alias.getAlias());
        }
        return agentInfo;
    }

    public AgentInfo getAgentByDeleteHash(String deleteHash) {
        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("deleteHash", deleteHash));
        if (document == null) {
            return null;
        }
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (alias != null) {
            agentInfo.setAgentAlias(alias.getAlias());
        }
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
        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (alias != null) {
            agentInfo.setAgentAlias(alias.getAlias());
        }
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
            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
            AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
            if (alias != null) {
                agentInfo.setAgentAlias(alias.getAlias());
            }
            resultList.add(agentInfo);
        }

        return resultList;
    }

    public PageInfo<AgentInfo> getAgentList(int type, int pageNumber, int pageSize) {
        Bson filter = null;
        if (type == 1) {
            filter = Filters.nin("agentAddress", ApiContext.DEVELOPER_NODE_ADDRESS.addAll(ApiContext.AMBASSADOR_NODE_ADDRESS));
        } else if (type == 2) {
            filter = Filters.in("agentAddress", ApiContext.DEVELOPER_NODE_ADDRESS);
        } else if (type == 3) {
            filter = Filters.in("agentAddress", ApiContext.AMBASSADOR_NODE_ADDRESS);
        }
        long totalCount = this.mongoDBService.getCount(MongoTableName.AGENT_INFO, filter);
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.AGENT_INFO, filter, Sorts.descending("createTime"), pageNumber, pageSize);
        List<AgentInfo> agentInfoList = new ArrayList<>();
        for (Document document : docsList) {
            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "agentId", AgentInfo.class);
            AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
            if (alias != null) {
                agentInfo.setAgentAlias(alias.getAlias());
            }
            agentInfoList.add(agentInfo);
        }
        PageInfo<AgentInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, agentInfoList);
        return pageInfo;
    }

    public long agentsCount(long startHeight) {
        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
        return this.mongoDBService.getCount(MongoTableName.AGENT_INFO, bson);
    }

    public long getConsensusCoinTotal() {
        MongoCollection<Document> collection = mongoDBService.getCollection(MongoTableName.AGENT_INFO);
        AggregateIterable<Document> ai = collection.aggregate(Arrays.asList(
                Aggregates.group(null, Accumulators.sum("deposit", "$deposit"), Accumulators.sum("totalDeposit", "$totalDeposit"))
        ));
        MongoCursor<Document> cursor = ai.iterator();
        long totalBalance = 0;
        while (cursor.hasNext()) {
            Document document = cursor.next();
            totalBalance += document.getLong("deposit");
            totalBalance += document.getLong("totalDeposit");
        }
        return totalBalance;
    }
}

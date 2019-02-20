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

import java.util.*;

@Component
public class AgentService {
    //todo
    private static final Map<String, AgentInfo> agentMap = new HashMap<>();

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AliasService aliasService;
    @Autowired
    private AccountService accountService;

    private void initCache() {
        if (!agentMap.isEmpty()) {
            return;
        }
        List<Document> list = mongoDBService.query(MongoTableName.AGENT_INFO);
        for (Document doc : list) {
            AgentInfo info = DocumentTransferTool.toInfo(doc, "txHash", AgentInfo.class);
            agentMap.put(info.getTxHash(), info);
        }
    }

    public AgentInfo getAgentByPackingAddress(String packingAddress) {
        initCache();
        Collection<AgentInfo> agentInfos = agentMap.values();
        AgentInfo info = null;
        for (AgentInfo agent : agentInfos) {
            if (!agent.getPackingAddress().equals(packingAddress)) {
                continue;
            }
            if (null == info || agent.getCreateTime() > info.getCreateTime()) {
                info = agent;
            }
        }
        return info;
//        Bson filter = Filters.and(Filters.eq("packingAddress", packingAddress));
//        List<Document> list = mongoDBService.query(MongoTableName.AGENT_INFO, filter, Sorts.descending("createTime"));
//        if (list == null || list.isEmpty()) {
//            return null;
//        }
//        Document document = list.get(0);
//        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
//        if (alias != null) {
//            agentInfo.setAgentAlias(alias.getAlias());
//        }
//        return agentInfo;
    }

    public AgentInfo getAgentByAgentAddress(String agentAddress) {
        initCache();
        Collection<AgentInfo> agentInfos = agentMap.values();
        AgentInfo info = null;
        for (AgentInfo agent : agentInfos) {
            if (!agent.getAgentAddress().equals(agentAddress)) {
                continue;
            }
            if (null == info || agent.getCreateTime() > info.getCreateTime()) {
                info = agent;
            }
        }
        return info;
//        List<Document> list = mongoDBService.query(MongoTableName.AGENT_INFO, Filters.eq("agentAddress", agentAddress), Sorts.descending("createTime"));
//        if (list == null || list.isEmpty()) {
//            return null;
//        }
//        Document document = list.get(0);
//        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
//        if (alias != null) {
//            agentInfo.setAgentAlias(alias.getAlias());
//        }
//        return agentInfo;
    }

    public AgentInfo getAgentByAgentHash(String agentHash) {
        initCache();
        return agentMap.get(agentHash);
//        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("_id", agentHash));
//        if (document == null) {
//            return null;
//        }
//        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
//        if (alias != null) {
//            agentInfo.setAgentAlias(alias.getAlias());
//        }
//        return agentInfo;
    }

    public AgentInfo getAgentByDeleteHash(String deleteHash) {
        initCache();
        Collection<AgentInfo> agentInfos = agentMap.values();
        AgentInfo info = null;
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash().equals(deleteHash)) {
                info = agent;
                break;
            }
        }
        return info;
//        Document document = mongoDBService.findOne(MongoTableName.AGENT_INFO, Filters.eq("deleteHash", deleteHash));
//        if (document == null) {
//            return null;
//        }
//        AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//        AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
//        if (alias != null) {
//            agentInfo.setAgentAlias(alias.getAlias());
//        }
//        return agentInfo;
    }


    public void saveAgentList(List<AgentInfo> agentInfoList) {
        initCache();
        if (agentInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfoList) {
            Document document = DocumentTransferTool.toDocument(agentInfo, "txHash");
            agentMap.put(agentInfo.getTxHash(), agentInfo);
            if (agentInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", agentInfo.getTxHash()), document));
            }
        }
        mongoDBService.bulkWrite(MongoTableName.AGENT_INFO, modelList);
    }

    public void rollbackAgentList(List<AgentInfo> agentInfoList) {
        initCache();
        if (agentInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AgentInfo agentInfo : agentInfoList) {
            if (agentInfo.isNew()) {
                modelList.add(new DeleteOneModel(Filters.eq("_id", agentInfo.getTxHash())));
                agentMap.remove(agentInfo.getTxHash());
            } else {
                Document document = DocumentTransferTool.toDocument(agentInfo, "txHash");
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", agentInfo.getTxHash()), document));
                agentMap.put(agentInfo.getTxHash(), agentInfo);
            }
        }
        mongoDBService.bulkWrite(MongoTableName.AGENT_INFO, modelList);
    }

    public List<AgentInfo> getAgentList(long startHeight) {
        initCache();
        Collection<AgentInfo> agentInfos = agentMap.values();
        List<AgentInfo> resultList = new ArrayList<>();
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash() != null && agent.getDeleteHeight() <= startHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startHeight) {
                continue;
            }
            resultList.add(agent);
        }

//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//
//        List<Document> list = this.mongoDBService.query(MongoTableName.AGENT_INFO, bson);

//        for (Document document : list) {
//            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
//            AliasInfo alias = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
//            if (alias != null) {
//                agentInfo.setAgentAlias(alias.getAlias());
//            }
//            resultList.add(agentInfo);
//        }

        return resultList;
    }

    public PageInfo<AgentInfo> getAgentList(int type, int pageNumber, int pageSize) {
        Bson filter = null;
        Bson deleteFilter = Filters.eq("deleteHeight", 0);
        if (type == 1) {
            filter = Filters.and(Filters.nin("agentAddress", ApiContext.DEVELOPER_NODE_ADDRESS.addAll(ApiContext.AMBASSADOR_NODE_ADDRESS), deleteFilter));
        } else if (type == 2) {
            filter = Filters.and(Filters.in("agentAddress", ApiContext.DEVELOPER_NODE_ADDRESS, deleteFilter));
        } else if (type == 3) {
            filter = Filters.and(Filters.in("agentAddress", ApiContext.AMBASSADOR_NODE_ADDRESS, deleteFilter));
        } else {
            filter = deleteFilter;
        }
        long totalCount = this.mongoDBService.getCount(MongoTableName.AGENT_INFO, filter);
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.AGENT_INFO, filter, Sorts.descending("createTime"), pageNumber, pageSize);
        List<AgentInfo> agentInfoList = new ArrayList<>();
        for (Document document : docsList) {
            AgentInfo agentInfo = DocumentTransferTool.toInfo(document, "txHash", AgentInfo.class);
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
        initCache();
        Collection<AgentInfo> agentInfos = agentMap.values();
        long count = 0;
        for (AgentInfo agent : agentInfos) {
            if (agent.getDeleteHash() != null && agent.getDeleteHeight() <= startHeight) {
                continue;
            }
            if (agent.getBlockHeight() > startHeight) {
                continue;
            }
            count++;
        }
        return count;
//        Bson bson = Filters.and(Filters.lte("blockHeight", startHeight), Filters.or(Filters.eq("deleteHeight", 0), Filters.gt("deleteHeight", startHeight)));
//        return this.mongoDBService.getCount(MongoTableName.AGENT_INFO, bson);
    }

    public long getConsensusCoinTotal() {
        initCache();
        Document filter = new Document();
        filter.put("deleteHeight", 0);

        Document match = new Document("$match", filter);
        MongoCollection<Document> collection = mongoDBService.getCollection(MongoTableName.AGENT_INFO);
        AggregateIterable<Document> ai = collection.aggregate(Arrays.asList(
                match,
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

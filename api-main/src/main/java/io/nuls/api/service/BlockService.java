package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class BlockService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AgentService agentService;

    /**
     * 获取本地存储的最新区块头
     *
     * @return BlockHeaderInfo 最新的区块头
     */
    public BlockHeaderInfo getBestBlockHeader() {
        Document document = mongoDBService.findOne(MongoTableName.NEW_INFO, Filters.eq("_id", MongoTableName.BEST_BLOCK_HEIGHT));
        if (document == null) {
            return null;
        }
        return getBlockHeaderInfoByHeight(document.getLong(MongoTableName.BEST_BLOCK_HEIGHT));
    }

    /**
     * 按照高度获取区块头
     *
     * @param height 高度
     * @return BlockHeaderInfo
     */
    public BlockHeaderInfo getBlockHeaderInfoByHeight(long height) {
        Document document = mongoDBService.findOne(MongoTableName.BLOCK_HEADER, Filters.eq("_id", height));
        if (document == null) {
            return null;
        }
        return DocumentTransferTool.toInfo(document, "height", BlockHeaderInfo.class);
    }

    /**
     * 存区块头
     *
     * @param blockHeaderInfo 区块头
     */
    public void saveBLockHeaderInfo(BlockHeaderInfo blockHeaderInfo) {
        Document document = DocumentTransferTool.toDocument(blockHeaderInfo, "height");
        mongoDBService.insertOne(MongoTableName.BLOCK_HEADER, document);
    }

    /**
     * 存储最新区块信息
     * 1. 存储最新的区块头信息
     * 2. 存储交易和地址的关系信息
     * 3. 存储交易
     * 4. 根据交易更新各个地址的余额
     *
     * @param blockInfo 完整的区块信息
     * @return boolean 是否保存成功
     */
    public boolean saveNewBlock(BlockInfo blockInfo) {
        saveNewHeightInfo(blockInfo.getBlockHeader().getHeight());

        BlockHeaderInfo headerInfo = blockInfo.getBlockHeader();
        //根据区块头的打包地址，查询打包节点的节点信息，做关联存储使用
        AgentInfo agentInfo = agentService.getAgentByPackingAddress(headerInfo.getPackingAddress());
        if (agentInfo != null) {
            //若查询不到agent节点信息时，说明可能是种子节点打包的块，
            //则创建一个新的AgentInfo对象，临时使用
            agentInfo = new AgentInfo();
            agentInfo.setPackingAddress(blockInfo.getBlockHeader().getPackingAddress());
        }

        headerInfo.setByAgentInfo(agentInfo);
        saveBLockHeaderInfo(headerInfo);


        //处理交易
        processTransactions(blockInfo.getTxs());


        return false;
    }


    private void processTransactions(List<TransactionInfo> txs) {
        //记录交易和账户地址的关系
        Set<TxRelationInfo> txRelationInfoList = new HashSet<>();
        TxRelationInfo txRelationInfo;
        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            if (tx.getFroms() != null) {
//                for()
//                txRelationInfoList.add(new TxRelationInfo())
            }
        }
    }


    /**
     * 保存最新的高度信息
     *
     * @param newHeight 最新高度
     */
    private void saveNewHeightInfo(long newHeight) {
        Bson query = Filters.eq("_id", MongoTableName.BEST_BLOCK_HEIGHT);
        Document document = mongoDBService.findOne(MongoTableName.NEW_INFO, query);
        if (document == null) {
            document = new Document();
            document.append("_id", MongoTableName.BEST_BLOCK_HEIGHT).append("height", newHeight).append("finish", false);
            mongoDBService.insertOne(MongoTableName.NEW_INFO, document);
        } else {
            document.put("height", newHeight);
            document.put("finish", false);
            mongoDBService.update(MongoTableName.NEW_INFO, query, document);
        }
    }
}

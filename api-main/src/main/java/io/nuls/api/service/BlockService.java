package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;

import java.util.HashSet;
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
     * @return BlockRelationInfo 最新的区块关系记录
     */
    public BlockHeaderInfo getBestBlockHeader() {
        Document document = mongoDBService.findOne(MongoTableName.NEW_INFO, Filters.eq("_id", MongoTableName.BEST_BLOCK_HEIGHT));
        if (document == null) {
            return null;
        }
        return getBlockHeaderInfoByHeight(document.getLong(MongoTableName.BEST_BLOCK_HEIGHT));
    }

    public BlockHeaderInfo getBlockHeaderInfoByHeight(long height) {
        Document document = mongoDBService.findOne(MongoTableName.BLOCK_HEADER, Filters.eq("_id", height));
        if (document == null) {
            return null;
        }
        return DocumentTransferTool.toInfo(document, "height", BlockHeaderInfo.class);
    }

    public void saveBLockHeaderInfo(BlockHeaderInfo blockHeaderInfo) {
        Document document = DocumentTransferTool.toDocument(blockHeaderInfo, "height");
        mongoDBService.insertOne(MongoTableName.BLOCK_HEADER, document);
    }

    /**
     * 存储最新区块信息
     * 1. 存储最新的区块信息
     * 2. 存储交易和地址的关系信息
     * 3. 存储交易
     * 4. 根据交易更新各个地址的余额
     *
     * @param blockInfo 完整的区块信息
     * @return boolean 是否保存成功
     */
    public boolean saveNewBlock(BlockInfo blockInfo) {
        Document document = new Document();
        document.put("bestBlockHeight", blockInfo.getBlockHeader().getHeight());
        mongoDBService.insertOne("bestBlockHeight", document);

        BlockHeaderInfo headerInfo = blockInfo.getBlockHeader();
        //根据区块头的打包地址，查询打包节点的节点信息，做关联存储使用
        AgentInfo agentInfo = agentService.getAgentByPackingAddress(blockInfo.getBlockHeader().getPackingAddress());
        if (agentInfo != null) {
            //若查询不到agent节点信息时，说明可能是种子节点打包的块，
            //则创建一个新的AgentInfo对象，临时使用
            agentInfo = new AgentInfo();
            agentInfo.setPackingAddress(blockInfo.getBlockHeader().getPackingAddress());
        }
//        blockRelationInfo.setAgentInfo(agentInfo);
//        saveBLockRelationInfo(blockRelationInfo);


        //处理交易
        //记录交易和账户地址的关系
        Set<TxRelationInfo> txRelationInfoList = new HashSet<>();
        TxRelationInfo txRelationInfo;
        for (int i = 0; i < blockInfo.getTxs().size(); i++) {
            TransactionInfo tx = blockInfo.getTxs().get(i);
            if (tx.getFroms() != null) {

//                txRelationInfoList.add(new TxRelationInfo())
            }
        }


        return false;
    }

}

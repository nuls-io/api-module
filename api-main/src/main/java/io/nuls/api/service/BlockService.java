package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.core.contast.TransactionConstant;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;

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
        processTransactions(blockInfo.getTxs(), agentInfo);


        return false;
    }

    //记录每个区块打包交易的所有已花费(input)
    List<Input> inputList = new ArrayList<>();
    //记录每个区块打包交易的所有新增未花费(output)
    Map<String, Output> outputMap = new HashMap<>();
    //记录每个区块交易和账户地址的关系
    Set<TxRelationInfo> txRelationInfoSet = new HashSet<>();
    //记录每个区块打包交易涉及到的账户的余额变动
    Map<String, AccountInfo> accountInfoMap = new HashMap<>();

    private void processTransactions(List<TransactionInfo> txs, AgentInfo agentInfo) {
        clear();

        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            tx.setByAgentInfo(agentInfo);

            processWithTxInputOutput(tx);

            if (tx.getType() == TransactionConstant.TX_TYPE_COINBASE) {
                processCoinBaseTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_TRANSFER) {

            }
        }
    }

    /**
     * 处理交易的input和output
     * 1. 首先根据每个input和output，添加交易和账户地址的关系，
     * 因为考虑到可能有同一个地址分别在交易的froms和to里出现，因此用txRelationInfoSet集合，再添加关系记录的时候自动去重
     * 2. 每个inputs都要加入到inputList集合里，在存储的时候，都要去删除数据库对应的utxo
     * 每个output都要加入到outputList集合里，在存储的时候，都要作为新的utxo存储到数据库
     *
     * @param tx
     */
    private void processWithTxInputOutput(TransactionInfo tx) {
        if (tx.getFroms() != null) {
            for (Input input : tx.getFroms()) {
                txRelationInfoSet.add(new TxRelationInfo(input.getAddress(), tx));
                //这里需要特殊处理，由于一个区块打包的多个交易中，可能存在下一个交易用到了上一个交易新生成的utxo
                //因此在这里添加进入inputList之前提前判断是否outputMap里已有对应的output，有就直接删除
                //没有才添加进入集合
                if (outputMap.containsKey(input.getKey())) {
                    outputMap.remove(input.getKey());
                } else {
                    inputList.add(input);
                }
            }
        }
        if (tx.getTos() != null) {
            for (Output output : tx.getTos()) {
                txRelationInfoSet.add(new TxRelationInfo(output.getAddress(), tx));
                outputMap.put(output.getKey(), output);
            }
        }
    }


    private void processCoinBaseTx(TransactionInfo tx) {
        for (Output output : tx.getTos()) {
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
        }
    }


    private AccountInfo queryAccountInfo(String address) {
        AccountInfo accountInfo = accountInfoMap.get(address);
        if(accountInfo == null) {
            accountInfo = null;
        }

        return accountInfo;
    }


    private void clear() {
        inputList.clear();
        outputMap.clear();
        txRelationInfoSet.clear();
        accountInfoMap.clear();

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

    /**
     * 回滚都是从最后保存的一个区块开始
     *
     * @return boolean
     */
    public boolean rollbackBlock() {
        return false;
    }
}

package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
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
    @Autowired
    private AccountService accountService;
    @Autowired
    private AliasService aliasService;
    @Autowired
    private DepositService depositService;
    @Autowired
    private BlockHeaderService blockHeaderService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private UTXOService utxoService;
    @Autowired
    private PunishService punishService;

    //记录每个区块打包交易的所有已花费(input)
    private List<Input> inputList = new ArrayList<>();
    //记录每个区块打包交易的所有新增未花费(output)
    private Map<String, Output> outputMap = new HashMap<>();
    //记录每个区块交易和账户地址的关系
    private Set<TxRelationInfo> txRelationInfoSet = new HashSet<>();
    //记录每个区块打包交易涉及到的账户的余额变动
    private Map<String, AccountInfo> accountInfoMap = new HashMap<>();
    //记录每个区块设置别名信息
    private List<AliasInfo> aliasInfoList = new ArrayList<>();
    //记录每个区块代理节点的变化
    private List<AgentInfo> agentInfoList = new ArrayList<>();
    //记录每个区块委托共识的信息
    private List<DepositInfo> depositInfoList = new ArrayList<>();
    //记录每个区块的红黄牌信息
    private List<PunishLog> punishLogList = new ArrayList<>();

    /**
     * 存储最新区块信息
     * 1. 存储最新的区块头信息
     * 2. 存储交易和地址的关系信息
     * 3. 存储交易
     * 4. 根据交易更新各个地址的余额
     * 5. 处理与存储交易的业务数据
     *
     * @param blockInfo 完整的区块信息
     * @return boolean 是否保存成功
     */
    public boolean saveNewBlock(BlockInfo blockInfo) {
        clear();

        BlockHeaderInfo headerInfo = blockInfo.getBlockHeader();

        AgentInfo agentInfo;
        if (headerInfo.isSeedPacked()) {
            //如果是种子节点打包的区块，则创建一个新的AgentInfo对象，临时使用
            agentInfo = new AgentInfo();
            agentInfo.setPackingAddress(headerInfo.getPackingAddress());
            agentInfo.setAgentId(headerInfo.getPackingAddress());
        } else {
            //根据区块头的打包地址，查询打包节点的节点信息，做关联存储使用
            agentInfo = agentService.getAgentByPackingAddress(headerInfo.getPackingAddress());
        }

        headerInfo.setByAgentInfo(agentInfo);

        //处理交易
        processTransactions(blockInfo.getTxs(), agentInfo, headerInfo.getHeight());

        save(headerInfo, agentInfo);
        return true;
    }


    private void processTransactions(List<TransactionInfo> txs, AgentInfo agentInfo, long blockHeight) {
        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            tx.setByAgentInfo(agentInfo);
            processWithTxInputOutput(tx);

            if (tx.getType() == TransactionConstant.TX_TYPE_COINBASE) {
                processCoinBaseTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_TRANSFER) {
                processTransferTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_ALIAS) {
                processAliasTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
                processCreateAgentTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_JOIN_CONSENSUS) {
                processDepositTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CANCEL_DEPOSIT) {
                processCancelDepositTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_STOP_AGENT) {
                processStopAgentTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
                processYellowPunishTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                processRedPunishTx(tx, blockHeight);
            }
            //todo 剩余的智能合约解析，等前面的数据解析没问题之后 ，再继续
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

    private void processCoinBaseTx(TransactionInfo tx, long blockHeight) {
        for (Output output : tx.getTos()) {
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn() + output.getValue());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setHeight(blockHeight);
        }
    }

    private void processTransferTx(TransactionInfo tx, long blockHeight) {
        Set<String> addressSet = new HashSet<>();
        for (Input input : tx.getFroms()) {
            AccountInfo accountInfo = queryAccountInfo(input.getAddress());
            accountInfo.setTotalOut(accountInfo.getTotalOut() + input.getValue());
            accountInfo.setHeight(blockHeight);
            if (!addressSet.contains(input.getAddress())) {
                addressSet.add(input.getAddress());
                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            }
        }

        for (Output output : tx.getTos()) {
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn() + output.getValue());
            accountInfo.setHeight(blockHeight);
            if (!addressSet.contains(output.getAddress())) {
                addressSet.add(output.getAddress());
                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            }
        }
    }

    private void processAliasTx(TransactionInfo tx, long blockHeight) {
        Set<String> addressSet = new HashSet<>();
        for (Input input : tx.getFroms()) {
            AccountInfo accountInfo = queryAccountInfo(input.getAddress());
            accountInfo.setTotalOut(accountInfo.getTotalOut() + input.getValue());
            accountInfo.setHeight(blockHeight);
            if (!addressSet.contains(input.getAddress())) {
                addressSet.add(input.getAddress());
                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            }
        }

        for (Output output : tx.getTos()) {
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn() + output.getValue());
            accountInfo.setHeight(blockHeight);
            if (!addressSet.contains(output.getAddress())) {
                addressSet.add(output.getAddress());
                accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            }
        }

        aliasInfoList.add((AliasInfo) tx.getTxData());
    }

    private void processCreateAgentTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setHeight(blockHeight);

        AgentInfo agentInfo = (AgentInfo) tx.getTxData();
        agentInfo.setBlockHeight(blockHeight);
        //查询agent节点是否设置过别名
        AliasInfo aliasInfo = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (aliasInfo != null) {
            agentInfo.setAgentAlias(aliasInfo.getAlias());
        }
        agentInfoList.add((AgentInfo) tx.getTxData());
    }

    private void processDepositTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setHeight(blockHeight);

        DepositInfo depositInfo = (DepositInfo) tx.getTxData();
        depositInfo.setNew(true);
        depositInfoList.add(depositInfo);
    }

    private void processCancelDepositTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setHeight(blockHeight);

        //查询委托记录，生成对应的取消委托信息
        DepositInfo cancelInfo = (DepositInfo) tx.getTxData();
        DepositInfo depositInfo = depositService.getDepositInfoByHash(cancelInfo.getTxHash());
        depositInfo.setDeleteHash(cancelInfo.getTxHash());
        depositInfo.setBlockHeight(blockHeight);
        cancelInfo.copyInfoWithDeposit(depositInfo);
        cancelInfo.setNew(true);
        depositInfoList.add(depositInfo);
        depositInfoList.add(cancelInfo);
    }

    private void processStopAgentTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setHeight(blockHeight);

        //查询所有当前节点下的委托，生成取消委托记录
        AgentInfo agentInfo = (AgentInfo) tx.getTxData();
        List<DepositInfo> depositInfos = depositService.getDepositListByAgentHash(agentInfo.getTxHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo depositInfo : depositInfos) {
                depositInfo.setBlockHeight(blockHeight);
                depositInfo.setDeleteHash(tx.getHash());
                depositInfoList.add(depositInfo);

                DepositInfo cancelDeposit = new DepositInfo();
                cancelDeposit.setNew(true);
                cancelDeposit.setType(NulsConstant.CANCEL_CONSENSUS);
                cancelDeposit.copyInfoWithDeposit(depositInfo);
                cancelDeposit.setTxHash(tx.getHash());
                cancelDeposit.setBlockHeight(blockHeight);
                cancelDeposit.setFee(0L);
                cancelDeposit.setCreateTime(tx.getCreateTime());
                depositInfoList.add(cancelDeposit);
            }
        }
    }


    public void processYellowPunishTx(TransactionInfo tx) {
        for (TxData txData : tx.getTxDataList()) {
            punishLogList.add((PunishLog) txData);
        }
    }

    public void processRedPunishTx(TransactionInfo tx, long blockHeight) {
        PunishLog redPunish = (PunishLog) tx.getTxData();
        punishLogList.add(redPunish);
        //根据红牌找到被惩罚的节点
        AgentInfo agentInfo = agentService.getAgentByAgentAddress(redPunish.getAddress());
        //根据节点找到委托列表
        List<DepositInfo> depositInfos = depositService.getDepositListByAgentHash(agentInfo.getTxHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo depositInfo : depositInfos) {
                depositInfo.setBlockHeight(blockHeight);
                depositInfo.setDeleteHash(tx.getHash());
                depositInfoList.add(depositInfo);

                DepositInfo cancelDeposit = new DepositInfo();
                cancelDeposit.setNew(true);
                cancelDeposit.setType(NulsConstant.CANCEL_CONSENSUS);
                cancelDeposit.copyInfoWithDeposit(depositInfo);
                cancelDeposit.setTxHash(tx.getHash());
                cancelDeposit.setBlockHeight(blockHeight);
                cancelDeposit.setFee(0L);
                cancelDeposit.setCreateTime(tx.getCreateTime());
                depositInfoList.add(cancelDeposit);
            }
        }
    }


    private AccountInfo queryAccountInfo(String address) {
        AccountInfo accountInfo = accountInfoMap.get(address);
        if (accountInfo == null) {
            accountInfo = accountService.getAccountInfoByAddress(address);
        }
        if (accountInfo == null) {
            accountInfo = new AccountInfo(address);
        }
        accountInfoMap.put(address, accountInfo);
        return accountInfo;
    }

    /**
     * 解析区块和所有交易后，将数据存储到数据库中
     */
    public void save(BlockHeaderInfo blockHeaderInfo, AgentInfo agentInfo) {
        saveNewHeightInfo(blockHeaderInfo.getHeight());
        blockHeaderService.saveBLockHeaderInfo(blockHeaderInfo);
        //如果区块非种子节点地址打包，则需要修改打包节点的奖励统计，放在agentInfoList里一并处理
        if (!blockHeaderInfo.isSeedPacked()) {
            agentInfoList.add(agentInfo);
        }

        //存储交易和地址关系记录
        transactionService.saveTxRelationList(txRelationInfoSet);
        //根据input和output更新utxo表
        utxoService.saveWithInputOutput(inputList, outputMap);
        //修改账户信息表
        accountService.saveAccounts(accountInfoMap);
        //存储别名记录
        aliasService.saveAliasList(aliasInfoList);
        //存储共识节点列表
        agentService.saveAgentList(agentInfoList);
        //存储委托/取消委托记录
        depositService.saveDepsoitList(depositInfoList);
        //存储红黄牌惩罚记录
        punishService.savePunishList(punishLogList);

        //todo 存储完成后记得修改new_info最新高度 isFinish = true;
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


    private void clear() {
        inputList.clear();
        outputMap.clear();
        txRelationInfoSet.clear();
        accountInfoMap.clear();
        aliasInfoList.clear();
        agentInfoList.clear();
        depositInfoList.clear();
        punishLogList.clear();
    }
}

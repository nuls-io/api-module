package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.Log;
import io.nuls.api.utils.RoundManager;
import io.nuls.sdk.core.contast.TransactionConstant;
import io.nuls.sdk.core.model.transaction.CoinBaseTransaction;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigInteger;
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
    @Autowired
    private RoundManager roundManager;
    @Autowired
    private WalletRPCHandler rpcHandler;
    @Autowired
    private NRC20Sever nrc20Sever;

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
    //记录每个区块新创建的智能合约信息
    private List<ContractInfo> contractInfoList = new ArrayList<>();
    //记录每个区块智能合约相关的账户token信息
    private List<AccountTokenInfo> tokenInfoList = new ArrayList<>();
    //记录智能合约相关的交易信息
    private List<ContractTxInfo> contractTxInfoList = new ArrayList<>();

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

    boolean hasContract = false;

    public boolean saveNewBlock(BlockInfo blockInfo) {
        clear();
        long time1, time2;
        time1 = System.currentTimeMillis();

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

        calcCommissionReward(agentInfo, blockInfo.getTxs().get(0));

        agentInfo.setTotalPackingCount(agentInfo.getTotalPackingCount() + 1);
        agentInfo.setLastRewardHeight(headerInfo.getHeight());
        agentInfo.setVersion(headerInfo.getAgentVersion());
        headerInfo.setByAgentInfo(agentInfo);

        //处理交易
        processTransactions(blockInfo.getTxs(), agentInfo, headerInfo.getHeight());

        processRoundData(blockInfo);

        save(blockInfo, agentInfo);
        time2 = System.currentTimeMillis();
        Log.info("-----------------height:" + blockInfo.getBlockHeader().getHeight() + ", tx:" + blockInfo.getTxs().size() + ", use:" + (time2 - time1) + "ms");
        ApiContext.bestHeight = headerInfo.getHeight();
        return true;
    }

    private void calcCommissionReward(AgentInfo agentInfo, TransactionInfo transactionInfo) {
        List<Output> list = transactionInfo.getTos();
        long agentReward = 0L, other = 0L;
        for (Output output : list) {
            if (output.getAddress().equals(agentInfo.getRewardAddress())) {
                agentReward += output.getValue();
            } else {
                other += output.getValue();
            }
        }
        agentInfo.setTotalReward(agentInfo.getTotalReward() + agentReward);
        long value = other * agentInfo.getCommissionRate() / (100 - agentInfo.getCommissionRate());
        agentInfo.setCommissionReward(agentInfo.getCommissionReward() + value);
    }


    private void processRoundData(BlockInfo blockInfo) {
        roundManager.process(blockInfo);
    }


    private void processTransactions(List<TransactionInfo> txs, AgentInfo agentInfo, long blockHeight) {
        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            if (tx.getType() > 10) {
                hasContract = true;
            }

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
                processYellowPunishTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                processRedPunishTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CREATE_CONTRACT) {
                processCreateContract(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CALL_CONTRACT) {

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
                // txRelationInfoSet.add(new TxRelationInfo(input.getAddress(), tx));
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
                outputMap.put(output.getKey(), output);
            }
        }
    }

    private void processCoinBaseTx(TransactionInfo tx, long blockHeight) {
        if (tx.getTos() == null || tx.getTos().isEmpty()) {
            return;
        }
        for (Output output : tx.getTos()) {
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn() + output.getValue());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setTotalBalance(accountInfo.getTotalBalance() + output.getValue());
            accountInfo.setHeight(blockHeight);
            txRelationInfoSet.add(new TxRelationInfo(output.getAddress(), tx, output.getValue(), accountInfo.getTotalBalance()));
        }
    }

    /**
     * 处理转账交易对应的账户信息，由于转账交易可能是多对多转账，也有可能一个地址有多条input记录，
     * 因此需要先针对这个交易的每个地址记录总账，然后修改accountInfo的数据
     *
     * @param tx
     * @param blockHeight
     */
    private void processTransferTx(TransactionInfo tx, long blockHeight) {
        Map<String, Long> addressMap = new HashMap<>();
        Long value;
        for (Input input : tx.getFroms()) {
            value = addressMap.get(input.getAddress()) == null ? 0L : addressMap.get(input.getAddress());
            value -= input.getValue();
            addressMap.put(input.getAddress(), value);
        }

        for (Output output : tx.getTos()) {
            value = addressMap.get(output.getAddress()) == null ? 0L : addressMap.get(output.getAddress());
            value += output.getValue();
            addressMap.put(output.getAddress(), value);
        }

        for (Map.Entry<String, Long> entry : addressMap.entrySet()) {
            AccountInfo accountInfo = queryAccountInfo(entry.getKey());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setHeight(blockHeight);
            value = entry.getValue();
            if (value > 0) {
                accountInfo.setTotalIn(accountInfo.getTotalIn() + value);
            } else {
                accountInfo.setTotalOut(accountInfo.getTotalOut() + Math.abs(value));
            }
            accountInfo.setTotalBalance(accountInfo.getTotalBalance() + value);
            txRelationInfoSet.add(new TxRelationInfo(entry.getKey(), tx, value, accountInfo.getTotalBalance()));
        }
    }

    /**
     * 别名处理accountInfo和txRelationInfo与转账交易类似
     *
     * @param tx
     * @param blockHeight
     */
    private void processAliasTx(TransactionInfo tx, long blockHeight) {
        Map<String, Long> addressMap = new HashMap<>();
        Long value;
        for (Input input : tx.getFroms()) {
            value = addressMap.get(input.getAddress()) == null ? 0L : addressMap.get(input.getAddress());
            value -= input.getValue();
            addressMap.put(input.getAddress(), value);
        }

        for (Output output : tx.getTos()) {
            value = addressMap.get(output.getAddress()) == null ? 0L : addressMap.get(output.getAddress());
            value += output.getValue();
            addressMap.put(output.getAddress(), value);
        }

        for (Map.Entry<String, Long> entry : addressMap.entrySet()) {
            AccountInfo accountInfo = queryAccountInfo(entry.getKey());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setHeight(blockHeight);
            value = entry.getValue();
            if (value > 0) {
                accountInfo.setTotalIn(accountInfo.getTotalIn() + value);
            } else {
                accountInfo.setTotalOut(accountInfo.getTotalOut() + Math.abs(value));
            }
            accountInfo.setTotalBalance(accountInfo.getTotalBalance() + value);
            txRelationInfoSet.add(new TxRelationInfo(entry.getKey(), tx, value, accountInfo.getTotalBalance()));
        }
        aliasInfoList.add((AliasInfo) tx.getTxData());
    }

    /**
     * 共识交易因为是单个账户锁定金额，因此处理方式要改为锁定金额的处理
     *
     * @param tx
     * @param blockHeight
     */
    private void processCreateAgentTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() - tx.getFee());
        accountInfo.setHeight(blockHeight);

        //记录交易的锁定金额
        long lockValue = tx.getTos().get(0).getValue();
        txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, lockValue, accountInfo.getTotalBalance()));

        AgentInfo agentInfo = (AgentInfo) tx.getTxData();
        agentInfo.setNew(true);
        agentInfo.setBlockHeight(blockHeight);
        //查询agent节点是否设置过别名
        AliasInfo aliasInfo = aliasService.getAliasByAddress(agentInfo.getAgentAddress());
        if (aliasInfo != null) {
            agentInfo.setAgentAlias(aliasInfo.getAlias());
        }
        agentInfoList.add((AgentInfo) tx.getTxData());
    }

    /**
     * 参与共识与创建节点交易类似
     *
     * @param tx
     * @param blockHeight
     */
    private void processDepositTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() - tx.getFee());
        accountInfo.setHeight(blockHeight);

        //记录交易的锁定金额
        long lockValue = tx.getTos().get(0).getValue();
        txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, lockValue, accountInfo.getTotalBalance()));

        DepositInfo depositInfo = (DepositInfo) tx.getTxData();
        depositInfo.setNew(true);
        depositInfoList.add(depositInfo);
    }

    private void processCancelDepositTx(TransactionInfo tx, long blockHeight) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() - tx.getFee());
        accountInfo.setHeight(blockHeight);

        //记录交易的解锁金额
        long lockValue = tx.getTos().get(0).getValue();
        txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, lockValue, accountInfo.getTotalBalance()));

        //查询委托记录，生成对应的取消委托信息
        DepositInfo cancelInfo = (DepositInfo) tx.getTxData();
        DepositInfo depositInfo = depositService.getDepositInfoByHash(cancelInfo.getTxHash());
        depositInfo.setDeleteHash(cancelInfo.getTxHash());
        depositInfo.setDeleteHeight(cancelInfo.getBlockHeight());
        cancelInfo.copyInfoWithDeposit(depositInfo);
        cancelInfo.setTxHash(tx.getHash());
        cancelInfo.setNew(true);
        depositInfoList.add(depositInfo);
        depositInfoList.add(cancelInfo);
    }

    private void processStopAgentTx(TransactionInfo tx, long blockHeight) {
        for (int i = 0; i < tx.getTos().size(); i++) {
            Output output = tx.getTos().get(i);
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setHeight(blockHeight);
            if (i == 0) {
                accountInfo.setTotalBalance(accountInfo.getTotalBalance() - tx.getFee());
            }
            txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, output.getValue(), accountInfo.getTotalBalance()));
        }

        //查询所有当前节点下的委托，生成取消委托记录
        AgentInfo agentInfo = (AgentInfo) tx.getTxData();
        agentInfo = agentService.getAgentByAgentId(agentInfo.getTxHash().substring(agentInfo.getTxHash().length() - 8));
        agentInfo.setDeleteHash(tx.getHash());
        agentInfo.setDeleteHeight(tx.getHeight());
        agentInfoList.add(agentInfo);

        //根据节点找到委托列表
        List<DepositInfo> depositInfos = depositService.getDepositListByAgentHash(agentInfo.getTxHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo depositInfo : depositInfos) {
                depositInfo.setDeleteHash(tx.getHash());
                depositInfo.setDeleteHeight(tx.getHeight());
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


    public void processYellowPunishTx(TransactionInfo tx, long blockHeight) {
        for (TxData txData : tx.getTxDataList()) {
            PunishLog punishLog = (PunishLog) txData;
            punishLogList.add(punishLog);
            AccountInfo accountInfo = queryAccountInfo(punishLog.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setHeight(blockHeight);
            txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, 0, accountInfo.getTotalBalance()));
        }
    }

    public void processRedPunishTx(TransactionInfo tx, long blockHeight) {
        PunishLog redPunish = (PunishLog) tx.getTxData();
        punishLogList.add(redPunish);

        for (int i = 0; i < tx.getTos().size(); i++) {
            Output output = tx.getTos().get(i);
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() + 1);
            accountInfo.setHeight(blockHeight);
            txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, output.getValue(), accountInfo.getTotalBalance()));
        }

        //根据红牌找到被惩罚的节点
        AgentInfo agentInfo = agentService.getAgentByAgentAddress(redPunish.getAddress());
        agentInfo.setDeleteHash(tx.getHash());
        agentInfo.setDeleteHeight(tx.getHeight());
        agentInfoList.add(agentInfo);
        //根据节点找到委托列表
        List<DepositInfo> depositInfos = depositService.getDepositListByAgentHash(agentInfo.getTxHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo depositInfo : depositInfos) {
                depositInfo.setDeleteHash(tx.getHash());
                depositInfo.setDeleteHeight(tx.getHeight());
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

    private void processCreateContract(TransactionInfo tx, long blockHeight) {
        //首先查询合约交易执行结果
        RpcClientResult<ContractResultInfo> clientResult = rpcHandler.getContractResult(tx.getHash());
        if (clientResult.isSuccess() == false) {
            throw new RuntimeException(clientResult.getMsg());
        }
        //执行结果为失败时，直接返回
        ContractResultInfo resultInfo = clientResult.getData();
        if (!resultInfo.getSuccess()) {
            return;
        }

        ContractCreateInfo contractCreateInfo = (ContractCreateInfo) tx.getTxData();
        RpcClientResult<ContractInfo> contractInfoResult = rpcHandler.getContractInfo(contractCreateInfo.getContractAddress());
        if (contractInfoResult.isSuccess() == false) {
            throw new RuntimeException(clientResult.getMsg());
        }
        ContractInfo contractInfo = contractInfoResult.getData();
        contractInfoList.add(contractInfo);

        createContractTxInfo(tx, blockHeight, contractInfo.getContractAddress());
        //如果是NRC20合约，还需要创建合约token地址
        if (contractInfo.getIsNrc20() == 1) {
            processNrc20ForAccount(contractInfo.getCreater(), contractInfo.getSymbol(), contractInfo.getTotalSupply());
        }
    }

    private void createContractTxInfo(TransactionInfo tx, long blockHeight, String contractAddress) {
        ContractTxInfo contractTxInfo = new ContractTxInfo();
        contractTxInfo.setTxHash(tx.getHash());
        contractTxInfo.setBlockHeight(blockHeight);
        contractTxInfo.setContractAddress(contractAddress);
        contractTxInfo.setTime(tx.getCreateTime());
        contractTxInfo.setType(tx.getType());
        contractTxInfo.setFee(tx.getFee());

        contractTxInfoList.add(contractTxInfo);
    }

    private void processNrc20ForAccount(String address, String symbol, BigInteger value) {
        AccountTokenInfo tokenInfo = nrc20Sever.getAccountTokenInfo(address, symbol);
        if (tokenInfo == null) {
            AccountInfo accountInfo = queryAccountInfo(address);
            accountInfo.getTokens().add(symbol);

            tokenInfo = new AccountTokenInfo(address, symbol);
        }
        tokenInfo.setBalance(tokenInfo.getBalance().add(value));
        if (tokenInfo.getBalance().compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("data error: " + address + " token[" + symbol + "] balance < 0");
        }
        tokenInfoList.add(tokenInfo);
    }


    private void processCallContract(TransactionInfo tx) {
        //首先查询合约交易执行结果
        RpcClientResult<ContractResultInfo> clientResult = rpcHandler.getContractResult(tx.getHash());
        if (clientResult.isSuccess() == false) {
            throw new RuntimeException(clientResult.getMsg());
        }
        //执行结果为失败时，直接返回
        ContractResultInfo resultInfo = clientResult.getData();
        if (!resultInfo.getSuccess()) {
            return;
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
    public void save(BlockInfo blockInfo, AgentInfo agentInfo) {
        if (hasContract) {
            return;
        }
        saveNewHeightInfo(blockInfo.getBlockHeader().getHeight());
        blockHeaderService.saveBLockHeaderInfo(blockInfo.getBlockHeader());
        //如果区块非种子节点地址打包，则需要修改打包节点的奖励统计，放在agentInfoList里一并处理
        if (!blockInfo.getBlockHeader().isSeedPacked()) {
            agentInfoList.add(agentInfo);
        }
        //存储交易记录
        transactionService.saveTxList(blockInfo.getTxs());
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
        depositService.saveDepositList(depositInfoList);
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
        contractInfoList.clear();
        tokenInfoList.clear();
        contractTxInfoList.clear();
    }
}

package io.nuls.api.service;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.Log;
import io.nuls.api.utils.RoundManager;
import io.nuls.sdk.core.contast.TransactionConstant;
import io.nuls.sdk.core.utils.JSONUtils;
import io.nuls.sdk.core.utils.StringUtils;
import org.bson.Document;

import java.math.BigInteger;
import java.util.*;

@Component
public class RollbackService {
    @Autowired
    private WalletRPCHandler rpcHandler;
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
    private TokenService tokenService;
    @Autowired
    private ContractService contractService;

    //记录每个区块打包交易的所有已花费(input)
    private List<Input> inputList = new ArrayList<>();
    //记录每个区块打包交易的所有新增未花费(output)
    private Map<String, Output> outputMap = new HashMap<>();
    //记录每个区块打包交易涉及到的账户的余额变动
    private Map<String, AccountInfo> accountInfoMap = new HashMap<>();
    //记录每个区块设置别名信息
    private List<AliasInfo> aliasInfoList = new ArrayList<>();
    //记录每个区块代理节点的变化
    private List<AgentInfo> agentInfoList = new ArrayList<>();
    //记录每个区块委托共识的信息
    private List<DepositInfo> depositInfoList = new ArrayList<>();

    //记录每个区块新创建的智能合约信息
    private Map<String, ContractInfo> contractInfoMap = new HashMap<>();
    //记录智能合约的执行结果信息
    private List<ContractResultInfo> contractResultList = new ArrayList<>();
    //记录每个区块智能合约相关的账户token信息
    private Map<String, AccountTokenInfo> accountTokenMap = new HashMap<>();
    //记录智能合约相关的交易信息
    private List<ContractTxInfo> contractTxInfoList = new ArrayList<>();

    private List<String> punishTxHashList = new ArrayList<>();

    private List<String> contractTxHashList = new ArrayList<>();

    private List<String> tokenTransferHashList = new ArrayList<>();

    /**
     * 回滚区块和区块内的所有交易和交易产生的数据
     */
    private long time1;
    int i = 0;

    public boolean rollbackBlock(long blockHeight) throws Exception {
        clear();
        time1 = System.currentTimeMillis();
        BlockInfo blockInfo = queryBlock(blockHeight);
        Log.info("-----------------queryBlock: use:" + (System.currentTimeMillis() - time1) + "ms");


        if (blockInfo == null) {
            rollbackComplete();
            return true;
        }
        time1 = System.currentTimeMillis();
        findAddProcessAgentOfBlock(blockInfo);
        Log.info("-----------------findAddProcessAgentOfBlock: use:" + (System.currentTimeMillis() - time1) + "ms");

        time1 = System.currentTimeMillis();
        processTxs(blockInfo.getTxs());
        Log.info("-----------------processTxs: use:" + (System.currentTimeMillis() - time1) + "ms");
        roundManager.rollback(blockInfo);
        time1 = System.currentTimeMillis();
        save(blockInfo);
        Log.info("-----------------save: use:" + (System.currentTimeMillis() - time1) + "ms");
//        if (i % 1000 == 0) {
//            Log.info("-----------------height:" + blockInfo.getBlockHeader().getHeight() + ", tx:" + blockInfo.getTxs().size() + ", use:" + (System.currentTimeMillis() - time1) + "ms");
//            time1 = System.currentTimeMillis();
//        }
        i++;
        ApiContext.bestHeight = blockHeight - 1;
        return true;
    }

    /**
     * 查找当前出块节点并处理相关信息
     *
     * @return
     */
    private AgentInfo findAddProcessAgentOfBlock(BlockInfo blockInfo) {
        BlockHeaderInfo headerInfo = blockInfo.getBlockHeader();
        AgentInfo agentInfo;
        if (headerInfo.isSeedPacked()) {
            //如果是种子节点打包的区块，则创建一个新的AgentInfo对象，临时使用
            agentInfo = new AgentInfo();
            agentInfo.setPackingAddress(headerInfo.getPackingAddress());
            agentInfo.setAgentId(headerInfo.getPackingAddress());
            agentInfo.setRewardAddress(agentInfo.getPackingAddress());
        } else {
            //根据区块头的打包地址，查询打包节点的节点信息，做关联存储使用
            agentInfo = queryAgentInfo(headerInfo.getPackingAddress(), 3);
        }

        agentInfo.setTotalPackingCount(agentInfo.getTotalPackingCount() - 1);
        agentInfo.setLastRewardHeight(headerInfo.getHeight() - 1);
        agentInfo.setVersion(headerInfo.getAgentVersion());

        if (!blockInfo.getTxs().isEmpty()) {
            calcCommissionReward(agentInfo, blockInfo.getTxs().get(0));
        }

        return agentInfo;
    }

    /**
     * 计算每个区块的佣金提成比例
     *
     * @param agentInfo
     * @param coinBaseTx
     */
    private void calcCommissionReward(AgentInfo agentInfo, TransactionInfo coinBaseTx) {
        List<Output> list = coinBaseTx.getTos();
        long agentReward = 0L, other = 0L;
        for (Output output : list) {
            if (output.getAddress().equals(agentInfo.getRewardAddress())) {
                agentReward += output.getValue();
            } else {
                other += output.getValue();
            }
        }

        agentInfo.setTotalReward(agentInfo.getTotalReward() - agentReward - other);
        agentInfo.setAgentReward(agentInfo.getAgentReward() - agentReward);
        agentInfo.setCommissionReward(agentInfo.getCommissionReward() - other);
    }

    private void processTxs(List<TransactionInfo> txs) throws Exception {
        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            for (Output output : tx.getTos()) {
                output.setTxHash(tx.getHash());
                outputMap.put(output.getKey(), output);
            }
        }

        for (int i = 0; i < txs.size(); i++) {
            TransactionInfo tx = txs.get(i);
            processTxInputOutput(tx);

            if (tx.getType() == TransactionConstant.TX_TYPE_COINBASE) {
                processCoinBaseTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_TRANSFER) {
                processTransferTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_ALIAS) {
                processAliasTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
                processCreateAgentTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_JOIN_CONSENSUS) {
                processDepositTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CANCEL_DEPOSIT) {
                processCancelDepositTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_STOP_AGENT) {
                processStopAgentTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
                processYellowPunishTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                processRedPunishTx(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CREATE_CONTRACT) {
                processCreateContract(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CALL_CONTRACT) {
                processCallContract(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_DELETE_CONTRACT) {
                processDeleteContract(tx);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CONTRACT_TRANSFER) {
                processContractTransfer(tx);
            }
        }
    }

    private void processTxInputOutput(TransactionInfo tx) {
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

    private void processCoinBaseTx(TransactionInfo tx) {
        if (tx.getTos() == null || tx.getTos().isEmpty()) {
            return;
        }
        for (Output output : tx.getTos()) {
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTotalIn(accountInfo.getTotalIn() - output.getValue());
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
            accountInfo.setTotalBalance(accountInfo.getTotalBalance() - output.getValue());
        }
    }

    private void processTransferTx(TransactionInfo tx) {
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
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
            value = entry.getValue();
            if (value > 0) {
                accountInfo.setTotalIn(accountInfo.getTotalIn() - value);
            } else {
                accountInfo.setTotalOut(accountInfo.getTotalOut() - Math.abs(value));
            }
            accountInfo.setTotalBalance(accountInfo.getTotalBalance() - value);
        }
    }

    private void processAliasTx(TransactionInfo tx) {
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
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
            value = entry.getValue();
            if (value > 0) {
                accountInfo.setTotalIn(accountInfo.getTotalIn() - value);
            } else {
                accountInfo.setTotalOut(accountInfo.getTotalOut() - Math.abs(value));
            }
            accountInfo.setTotalBalance(accountInfo.getTotalBalance() - value);
        }
        AliasInfo aliasInfo = aliasService.getAliasByAddress(tx.getFroms().get(0).getAddress());
        if (aliasInfo != null) {
            AccountInfo accountInfo = queryAccountInfo(aliasInfo.getAddress());
            accountInfo.setAlias(null);
            aliasInfoList.add(aliasInfo);
        }
    }

    private void processCreateAgentTx(TransactionInfo tx) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() - tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() + tx.getFee());
        //查找到代理节点，设置isNew = true，最后做存储的时候删除
        AgentInfo agentInfo = queryAgentInfo(tx.getHash(), 1);
        agentInfo.setNew(true);
    }

    private void processDepositTx(TransactionInfo tx) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() - tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() + tx.getFee());
        //查找到委托记录，设置isNew = true，最后做存储的时候删除
        DepositInfo depositInfo = depositService.getDepositInfoByKey(tx.getHash() + accountInfo.getAddress());
        depositInfo.setNew(true);
        depositInfoList.add(depositInfo);
        AgentInfo agentInfo = queryAgentInfo(depositInfo.getAgentHash(), 1);
        agentInfo.setTotalDeposit(agentInfo.getTotalDeposit() - depositInfo.getAmount());
        agentInfo.setNew(false);
        if (agentInfo.getTotalDeposit() < 0) {
            throw new RuntimeException("data error: agent[" + agentInfo.getTxHash() + "] totalDeposit < 0");
        }
    }

    private void processCancelDepositTx(TransactionInfo tx) {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() - tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() + tx.getFee());

        //查询取消委托记录，再根据deleteHash反向查到委托记录
        DepositInfo cancelInfo = depositService.getDepositInfoByHash(tx.getHash());
        DepositInfo depositInfo = depositService.getDepositInfoByKey(cancelInfo.getDeleteKey());
        depositInfo.setDeleteKey(null);
        depositInfo.setDeleteHeight(0);
        cancelInfo.setNew(true);
        depositInfoList.add(depositInfo);
        depositInfoList.add(cancelInfo);

        AgentInfo agentInfo = queryAgentInfo(depositInfo.getAgentHash(), 1);
        agentInfo.setTotalDeposit(agentInfo.getTotalDeposit() + depositInfo.getAmount());
        agentInfo.setNew(false);
    }

    private void processStopAgentTx(TransactionInfo tx) {
        for (int i = 0; i < tx.getTos().size(); i++) {
            Output output = tx.getTos().get(i);
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
            if (i == 0) {
                accountInfo.setTotalBalance(accountInfo.getTotalBalance() + tx.getFee());
            }
        }

        AgentInfo agentInfo = queryAgentInfo(tx.getHash(), 4);
        agentInfo.setDeleteHash(null);
        agentInfo.setDeleteHeight(0);
        agentInfo.setStatus(1);
        agentInfo.setNew(false);
        //根据交易hash查询所有取消委托的记录
        List<DepositInfo> depositInfos = depositService.getDepositListByHash(tx.getHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo cancelDeposit : depositInfos) {
                //需要删除的数据
                cancelDeposit.setNew(true);

                DepositInfo depositInfo = depositService.getDepositInfoByKey(cancelDeposit.getDeleteKey());
                depositInfo.setDeleteHeight(0);
                depositInfo.setDeleteKey(null);

                depositInfoList.add(cancelDeposit);
                depositInfoList.add(depositInfo);

                agentInfo.setTotalDeposit(agentInfo.getTotalDeposit() + depositInfo.getAmount());
            }
        }
    }

    private void processYellowPunishTx(TransactionInfo tx) {
        List<TxData> logList = punishService.getYellowPunishLog(tx.getHash());
        Set<String> addressSet = new HashSet<>();
        for (TxData txData : logList) {
            PunishLog punishLog = (PunishLog) txData;
            addressSet.add(punishLog.getAddress());
        }

        for (String address : addressSet) {
            AccountInfo accountInfo = queryAccountInfo(address);
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }

        punishTxHashList.add(tx.getHash());
    }

    public void processRedPunishTx(TransactionInfo tx) {
        punishTxHashList.add(tx.getHash());

        for (int i = 0; i < tx.getTos().size(); i++) {
            Output output = tx.getTos().get(i);
            AccountInfo accountInfo = queryAccountInfo(output.getAddress());
            accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        }

        PunishLog redPunish = punishService.getRedPunishLog(tx.getHash());
        //根据红牌找到被惩罚的节点
        AgentInfo agentInfo = queryAgentInfo(redPunish.getAddress(), 2);
        agentInfo.setDeleteHash(null);
        agentInfo.setDeleteHeight(0);
        agentInfo.setStatus(1);
        agentInfo.setNew(false);
        //根据交易hash查询所有取消委托的记录
        List<DepositInfo> depositInfos = depositService.getDepositListByHash(tx.getHash());
        if (!depositInfos.isEmpty()) {
            for (DepositInfo cancelDeposit : depositInfos) {
                cancelDeposit.setNew(true);

                DepositInfo depositInfo = depositService.getDepositInfoByKey(cancelDeposit.getDeleteKey());
                depositInfo.setDeleteHeight(0);
                depositInfo.setDeleteKey(null);

                depositInfoList.add(cancelDeposit);
                depositInfoList.add(depositInfo);

                agentInfo.setTotalDeposit(agentInfo.getTotalDeposit() + depositInfo.getAmount());
            }
        }
    }

    private void processCreateContract(TransactionInfo tx) throws Exception {
        ContractInfo contractInfo = contractService.getContractInfoByHash(tx.getHash());
        contractInfo = queryContractInfo(contractInfo.getContractAddress());
        contractInfo.setNew(false);


        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() - tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() + tx.getFee());

        contractTxHashList.add(tx.getHash());
        ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());
        if (contractInfo.getIsNrc20() == 1 && resultInfo.getSuccess()) {
            processTokenTransfers(resultInfo.getTokenTransfers(), tx);
        }
    }

    private void processCallContract(TransactionInfo tx) throws Exception {
        processTransferTx(tx);

        contractTxHashList.add(tx.getHash());
        ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());
        ContractInfo contractInfo = queryContractInfo(resultInfo.getContractAddress());
        contractInfo.setTxCount(contractInfo.getTxCount() - 1);

        if (resultInfo.getSuccess()) {
            processTokenTransfers(resultInfo.getTokenTransfers(), tx);
        }
    }

    /**
     * 处理Nrc20合约相关转账的记录
     *
     * @param tokenTransfers
     */
    private void processTokenTransfers(List<TokenTransfer> tokenTransfers, TransactionInfo tx) throws Exception {
        if (tokenTransfers == null || tokenTransfers.isEmpty()) {
            return;
        }
        tokenTransferHashList.add(tx.getHash());
        for (int i = 0; i < tokenTransfers.size(); i++) {
            TokenTransfer tokenTransfer = tokenTransfers.get(i);

            ContractInfo contractInfo = queryContractInfo(tokenTransfer.getContractAddress());
            contractInfo.setTransferCount(contractInfo.getTransferCount() - 1);

            if (tokenTransfer.getFromAddress() != null) {
                processNrc20ForAccount(contractInfo, tokenTransfer.getFromAddress(), new BigInteger(tokenTransfer.getValue()), 1);
            }

            processNrc20ForAccount(contractInfo, tokenTransfer.getToAddress(), new BigInteger(tokenTransfer.getValue()), -1);
        }
    }

    private AccountTokenInfo processNrc20ForAccount(ContractInfo contractInfo, String address, BigInteger value, int type) {
        AccountTokenInfo tokenInfo = queryAccountTokenInfo(address + contractInfo.getContractAddress());
        BigInteger balanceValue = new BigInteger(tokenInfo.getBalance());
        if (type == 1) {
            balanceValue = balanceValue.add(value);
        } else {
            balanceValue = balanceValue.subtract(value);
        }

        if (balanceValue.compareTo(BigInteger.ZERO) < 0) {
            throw new RuntimeException("data error: " + address + " token[" + contractInfo.getSymbol() + "] balance < 0");
        }
        tokenInfo.setBalance(balanceValue.toString());
        if (!accountTokenMap.containsKey(tokenInfo.getKey())) {
            accountTokenMap.put(tokenInfo.getKey(), tokenInfo);
        }

        return tokenInfo;
    }

    private void processDeleteContract(TransactionInfo tx) throws Exception {
        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() - 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() - tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() + tx.getFee());

        //首先查询合约交易执行结果
        ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());

        ContractInfo contractInfo = queryContractInfo(resultInfo.getContractAddress());
        contractInfo.setTxCount(contractInfo.getTxCount() - 1);

        contractTxHashList.add(tx.getHash());

        if (resultInfo.getSuccess()) {
            contractInfo.setStatus(NulsConstant.CONTRACT_STATUS_NORMAL);
        }
    }

    private void processContractTransfer(TransactionInfo tx) {
        processTransferTx(tx);
    }

    /**
     * 回滚的时候数据库存储处理是正常的同步区块的逆向操作
     *
     * @param blockInfo
     * @throws Exception
     */
    public void save(BlockInfo blockInfo) throws Exception {
        Document document = blockHeaderService.getBestBlockHeightInfo();
        time1 = System.currentTimeMillis();
        long height = blockInfo.getBlockHeader().getHeight();
        if (document.getBoolean("finish")) {
            accountService.saveAccounts(accountInfoMap, 0);
            blockHeaderService.updateStep(height,50);
            document.put("step", 50);
        }
        Log.info("-----------------saveAccounts: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        if (document.getInteger("step") == 50) {
            tokenService.saveAccountTokens(accountTokenMap);
            blockHeaderService.updateStep(height,40);
            document.put("step", 40);
        }
        Log.info("-----------------saveAccountTokens: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();


        if (document.getInteger("step") == 40) {
            contractService.rollbackContractInfos(contractInfoMap);
            blockHeaderService.updateStep(height,30);
            document.put("step", 30);
        }
        Log.info("-----------------rollbackContractInfos: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();


        if (document.getInteger("step") == 30) {
            agentService.rollbackAgentList(agentInfoList);
            blockHeaderService.updateStep(height,20);
            document.put("step", 20);
        }
        Log.info("-----------------rollbackAgentList: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();

        if (document.getInteger("step") == 20) {
            utxoService.rollbackOutputs(inputList, outputMap);
            blockHeaderService.updateStep(height,10);
            document.put("step", 10);
        }
        Log.info("-----------------rollbackOutputs: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();


        //回滾token转账信息
        tokenService.rollbackTokenTransfers(tokenTransferHashList, height);
        Log.info("-----------------rollbackTokenTransfers: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();

        //回滾智能合約交易
        contractService.rollbackContractTxInfos(contractTxHashList);
        Log.info("-----------------rollbackContractTxInfos: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滾合约执行结果记录
        contractService.rollbackContractResults(contractTxHashList);
        Log.info("-----------------rollbackContractResults: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();

        //回滚委托记录
        depositService.rollbackDepoist(depositInfoList);
        Log.info("-----------------rollbackDepoist: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滚惩罚记录
        punishService.rollbackPunishLog(punishTxHashList, height);
        Log.info("-----------------rollbackPunishLog: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滚别名记录
        aliasService.rollbackAliasList(aliasInfoList);
        Log.info("-----------------rollbackAliasList: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滚交易关系记录
        transactionService.rollbackTxRelationList(blockInfo.getBlockHeader().getTxHashList());
        Log.info("-----------------rollbackTxRelationList: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滚coinData记录
        utxoService.rollbackCoinDatas(blockInfo.getBlockHeader().getTxHashList());
        Log.info("-----------------rollbackCoinDatas: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滚交易记录
        transactionService.rollbackTxList(blockInfo.getBlockHeader().getTxHashList());
        Log.info("-----------------rollbackTxList: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
        //回滚区块信息
        blockHeaderService.deleteBlockHeader(height);
        Log.info("-----------------deleteBlockHeader: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();

        rollbackComplete();
        Log.info("-----------------rollbackComplete: use:" + (System.currentTimeMillis() - time1) + "ms");
        time1 = System.currentTimeMillis();
    }


    private void rollbackComplete() {
        blockHeaderService.rollbackComplete();
    }

    private AccountInfo queryAccountInfo(String address) {
        AccountInfo accountInfo = accountInfoMap.get(address);
        if (accountInfo == null) {
            accountInfo = accountService.getAccountInfo(address);
        }
        if (accountInfo == null) {
            accountInfo = new AccountInfo(address);
        }
        accountInfoMap.put(address, accountInfo);
        return accountInfo;
    }

    private ContractInfo queryContractInfo(String contractAddress) throws Exception {
        ContractInfo contractInfo = contractInfoMap.get(contractAddress);
        if (contractInfo == null) {
            contractInfo = contractService.getContractInfo(contractAddress);
            contractInfoMap.put(contractInfo.getContractAddress(), contractInfo);
        }
        return contractInfo;
    }

    private AccountTokenInfo queryAccountTokenInfo(String key) {
        AccountTokenInfo accountTokenInfo = accountTokenMap.get(key);
        if (accountTokenInfo == null) {
            accountTokenInfo = tokenService.getAccountTokenInfo(key);
        }
        return accountTokenInfo;
    }

    private AgentInfo queryAgentInfo(String key, int type) {
        AgentInfo agentInfo;
        for (int i = 0; i < agentInfoList.size(); i++) {
            agentInfo = agentInfoList.get(i);

            if (type == 1 && agentInfo.getTxHash().equals(key)) {
                return agentInfo;
            } else if (type == 2 && agentInfo.getAgentAddress().equals(key)) {
                return agentInfo;
            } else if (type == 3 && agentInfo.getPackingAddress().equals(key)) {
                return agentInfo;
            } else if (type == 4 && key.equals(agentInfo.getDeleteHash())) {
                return agentInfo;
            }
        }
        if (type == 1) {
            agentInfo = agentService.getAgentByAgentHash(key);
        } else if (type == 2) {
            agentInfo = agentService.getAgentByAgentAddress(key);
        } else if (type == 3) {
            agentInfo = agentService.getAgentByPackingAddress(key);
        } else {
            agentInfo = agentService.getAgentByDeleteHash(key);
        }
        if (agentInfo != null) {
            agentInfoList.add(agentInfo);
        }
        return agentInfo;
    }

    private BlockInfo queryBlock(long height) throws Exception {
        BlockHeaderInfo headerInfo = blockHeaderService.getBlockHeaderInfoByHeight(height);
        if (headerInfo == null) {
            return null;
        }
        BlockInfo blockInfo = new BlockInfo();
        blockInfo.setBlockHeader(headerInfo);

        List<TransactionInfo> txList = new ArrayList<>();
        for (int i = 0; i < headerInfo.getTxHashList().size(); i++) {
            TransactionInfo tx = transactionService.getTx(headerInfo.getTxHashList().get(i));
            if (tx != null) {
                findTxCoinData(tx);
                txList.add(tx);
            }
        }
        blockInfo.setTxs(txList);
        return blockInfo;
    }

    private void findTxCoinData(TransactionInfo tx) throws Exception {
        TxCoinData coinData = utxoService.getTxCoinData(tx.getHash());
        List<Input> inputs = new ArrayList<>();
        List<Output> outputs = new ArrayList<>();
        if (coinData != null) {
            if (StringUtils.isNotBlank(coinData.getInputsJson())) {
                inputs = JSONUtils.json2list(coinData.getInputsJson(), Input.class);
            }
            if (StringUtils.isNotBlank(coinData.getOutputsJson())) {
                outputs = JSONUtils.json2list(coinData.getOutputsJson(), Output.class);
            }
        }
        tx.setFroms(inputs);
        tx.setTos(outputs);
    }

    private void clear() {
        inputList.clear();
        outputMap.clear();
        punishTxHashList.clear();
        accountInfoMap.clear();
        aliasInfoList.clear();
        agentInfoList.clear();
        depositInfoList.clear();
        contractInfoMap.clear();
        contractResultList.clear();
        accountTokenMap.clear();
        contractTxInfoList.clear();
        contractTxHashList.clear();
        tokenTransferHashList.clear();
    }
}

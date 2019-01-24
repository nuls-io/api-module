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
import io.nuls.sdk.core.utils.DoubleUtils;

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
    private TokenService tokenService;
    @Autowired
    private ContractService contractService;

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
    private Map<String, ContractInfo> contractInfoMap = new HashMap<>();
    //记录智能合约的执行结果信息
    private List<ContractResultInfo> contractResultList = new ArrayList<>();
    //记录每个区块智能合约相关的账户token信息
    private Map<String, AccountTokenInfo> accountTokenMap = new HashMap<>();
    //记录智能合约相关的交易信息
    private List<ContractTxInfo> contractTxInfoList = new ArrayList<>();
    //记录合约转账信息
    private List<TokenTransfer> tokenTransferList = new ArrayList<>();

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

    private long time1;
    int i = 0;

    public boolean saveNewBlock(BlockInfo blockInfo) throws Exception {
        clear();
        if (time1 == 0) {
            time1 = System.currentTimeMillis();
        }
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

        if (i % 1000 == 0) {
            Log.info("-----------------height:" + blockInfo.getBlockHeader().getHeight() + ", tx:" + blockInfo.getTxs().size() + ", use:" + (System.currentTimeMillis() - time1) + "ms");
            time1 = System.currentTimeMillis();
        }
        i++;
        ApiContext.bestHeight = headerInfo.getHeight();
        return true;
    }

    private void calcCommissionReward(AgentInfo agentInfo, TransactionInfo transactionInfo) {
        List<Output> list = transactionInfo.getTos();
        if (null == list) {
            agentInfo.setTotalReward(0);
            agentInfo.setCommissionReward(0);
            return;
        }
        long agentReward = 0L, other = 0L;
        for (Output output : list) {
            if (output.getAddress().equals(agentInfo.getRewardAddress())) {
                agentReward += output.getValue();
            } else {
                other += output.getValue();
            }
        }
        List<DepositInfo> depositInfos = depositService.getDepositListByAgentHash(agentInfo.getTxHash());
        long totalDeposit = 0;
        for (DepositInfo depositInfo : depositInfos) {
            totalDeposit += depositInfo.getAmount();
        }
        agentInfo.setTotalDeposit(totalDeposit);
        agentInfo.setTotalReward(agentInfo.getTotalReward() + agentReward);
        if (agentInfo.getCommissionRate() < 100) {
            long value = other * agentInfo.getCommissionRate() / (100 - agentInfo.getCommissionRate());
            agentInfo.setCommissionReward(agentInfo.getCommissionReward() + value);
        } else {

            agentInfo.setCommissionReward((long) (agentReward * DoubleUtils.div(agentInfo.getDeposit(), agentInfo.getDeposit() + agentInfo.getTotalDeposit())));
        }
    }


    private void processRoundData(BlockInfo blockInfo) {
        roundManager.process(blockInfo);
    }


    private void processTransactions(List<TransactionInfo> txs, AgentInfo agentInfo, long blockHeight) throws Exception {
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
                processYellowPunishTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                processRedPunishTx(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CREATE_CONTRACT) {
                processCreateContract(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_CALL_CONTRACT) {
                processCallContract(tx, blockHeight);
            } else if (tx.getType() == TransactionConstant.TX_TYPE_DELETE_CONTRACT) {

            } else if (tx.getType() == TransactionConstant.TX_TYPE_CONTRACT_TRANSFER) {
                processContractTransfer(tx, blockHeight);
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
        AliasInfo aliasInfo = (AliasInfo) tx.getTxData();
        AccountInfo accountInfo = queryAccountInfo(aliasInfo.getAddress());
        if (accountInfo != null) {
            accountInfo.setAlias(aliasInfo.getAlias());
        }
        aliasInfoList.add(aliasInfo);
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

    private void processCreateContract(TransactionInfo tx, long blockHeight) throws Exception {
        ContractInfo contractInfo = (ContractInfo) tx.getTxData();
        contractInfo.setTxCount(1);
        contractInfo.setNew(true);
        contractInfo.setRemark(tx.getRemark());

        AccountInfo accountInfo = queryAccountInfo(tx.getFroms().get(0).getAddress());
        accountInfo.setTxCount(accountInfo.getTxCount() + 1);
        accountInfo.setTotalOut(accountInfo.getTotalOut() + tx.getFee());
        accountInfo.setTotalBalance(accountInfo.getTotalBalance() - tx.getFee());
        accountInfo.setHeight(blockHeight);
        txRelationInfoSet.add(new TxRelationInfo(accountInfo.getAddress(), tx, 0, accountInfo.getTotalBalance()));

        //首先查询合约交易执行结果
        RpcClientResult<ContractResultInfo> clientResult1 = rpcHandler.getContractResult(tx.getHash());
        ContractResultInfo resultInfo = clientResult1.getData();
        contractResultList.add(resultInfo);
        contractInfo.setSuccess(resultInfo.getSuccess());
        contractInfoMap.put(contractInfo.getContractAddress(), contractInfo);
        createContractTxInfo(tx, contractInfo, blockHeight);

        if (!resultInfo.getSuccess()) {
            contractInfo.setErrorMsg(resultInfo.getErrorMessage());
            contractInfo.setStatus(-1);
        } else {
            //如果是NRC20合约，还需要处理相关账户的token信息
            if (contractInfo.getIsNrc20() == 1) {
                processTokenTransfers(resultInfo.getTokenTransfers(), tx);
            }
        }
    }

    /**
     * @param tx
     * @param contractInfo
     * @param blockHeight
     */
    private void createContractTxInfo(TransactionInfo tx, ContractInfo contractInfo, long blockHeight) {
        ContractTxInfo contractTxInfo = new ContractTxInfo();
        contractTxInfo.setTxHash(tx.getHash());
        contractTxInfo.setBlockHeight(blockHeight);
        contractTxInfo.setContractAddress(contractInfo.getContractAddress());
        contractTxInfo.setTime(tx.getCreateTime());
        contractTxInfo.setType(tx.getType());
        contractTxInfo.setFee(tx.getFee());

        contractTxInfoList.add(contractTxInfo);
    }

    /**
     * 处理Nrc20合约相关的地址的token余额
     *
     * @param contractInfo
     * @param value
     */
    private AccountTokenInfo processNrc20ForAccount(ContractInfo contractInfo, String address, BigInteger value, int type) {
        AccountTokenInfo tokenInfo = queryAccountTokenInfo(address + contractInfo.getContractAddress());
        BigInteger balanceValue;
        if (tokenInfo == null) {
            AccountInfo accountInfo = queryAccountInfo(address);
            accountInfo.getTokens().add(contractInfo.getContractAddress() + "," + contractInfo.getSymbol());

            tokenInfo = new AccountTokenInfo(address, contractInfo.getContractAddress(), contractInfo.getTokenName(), contractInfo.getSymbol(), contractInfo.getDecimals());
        }
        balanceValue = new BigInteger(tokenInfo.getBalance());
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

    /**
     * 处理Nrc20合约相关转账的记录
     *
     * @param tokenTransfers
     */
    private void processTokenTransfers(List<TokenTransfer> tokenTransfers, TransactionInfo tx) throws Exception {
        if (tokenTransfers == null || tokenTransfers.isEmpty()) {
            return;
        }
        AccountTokenInfo tokenInfo;
        for (int i = 0; i < tokenTransfers.size(); i++) {
            TokenTransfer tokenTransfer = tokenTransfers.get(i);
            tokenTransfer.setTxHash(tx.getHash());
            tokenTransfer.setHeight(tx.getHeight());
            tokenTransfer.setTime(tx.getCreateTime());

            ContractInfo contractInfo = queryContractInfo(tokenTransfer.getContractAddress());
            if (contractInfo.getOwners().contains(tokenTransfer.getToAddress())) {
                contractInfo.getOwners().add(tokenTransfer.getToAddress());
            }
            contractInfo.setTransferCount(contractInfo.getTransferCount() + 1);

            if (tokenTransfer.getFromAddress() != null) {
                tokenInfo = processNrc20ForAccount(contractInfo, tokenTransfer.getFromAddress(), new BigInteger(tokenTransfer.getValue()), -1);
                tokenTransfer.setFromBalance(tokenInfo.getBalance());
            }

            tokenInfo = processNrc20ForAccount(contractInfo, tokenTransfer.getToAddress(), new BigInteger(tokenTransfer.getValue()), 1);
            tokenTransfer.setToBalance(tokenInfo.getBalance());

            tokenTransferList.add(tokenTransfer);
        }
    }


    private void processCallContract(TransactionInfo tx, long blockHeight) throws Exception {
        processTransferTx(tx, blockHeight);
        //首先查询合约交易执行结果
        RpcClientResult<ContractResultInfo> clientResult = rpcHandler.getContractResult(tx.getHash());
        if (clientResult.isSuccess() == false) {
            throw new RuntimeException(clientResult.getMsg());
        }

        ContractResultInfo resultInfo = clientResult.getData();
        ContractInfo contractInfo = queryContractInfo(resultInfo.getContractAddress());
        contractInfo.setTxCount(contractInfo.getTxCount() + 1);

        contractResultList.add(resultInfo);
        createContractTxInfo(tx, contractInfo, blockHeight);

        if (resultInfo.getSuccess()) {
            processTokenTransfers(resultInfo.getTokenTransfers(), tx);
        }
    }


    private void processContractTransfer(TransactionInfo tx, long blockHeight) throws Exception {
        processTransferTx(tx, blockHeight);
        ContractTransferInfo transferInfo = (ContractTransferInfo) tx.getTxData();
        ContractInfo contractInfo = queryContractInfo(transferInfo.getContractAddress());
        contractInfo.setTxCount(contractInfo.getTxCount() + 1);
    }


    /**
     * 解析区块和所有交易后，将数据存储到数据库中
     */
    public void save(BlockInfo blockInfo, AgentInfo agentInfo) throws Exception {
        //如果区块非种子节点地址打包，则需要修改打包节点的奖励统计，放在agentInfoList里一并处理
        if (!blockInfo.getBlockHeader().isSeedPacked()) {
            agentInfoList.add(agentInfo);
        }

        blockHeaderService.saveNewHeightInfo(blockInfo.getBlockHeader().getHeight());
        //存储区块头信息
        blockHeaderService.saveBLockHeaderInfo(blockInfo.getBlockHeader());
        //存储交易记录
        transactionService.saveTxList(blockInfo.getTxs());
        //存储交易和地址关系记录
        transactionService.saveTxRelationList(txRelationInfoSet);
        //根据input和output更新utxo表
        utxoService.saveWithInputOutput(inputList, outputMap);
        //存储别名记录
        aliasService.saveAliasList(aliasInfoList);
        //存储共识节点列表
        agentService.saveAgentList(agentInfoList);
        //存储委托/取消委托记录
        depositService.saveDepositList(depositInfoList);
        //存储红黄牌惩罚记录
        punishService.savePunishList(punishLogList);
        //存储智能合约交易关系记录
        contractService.saveContractTxInfos(contractTxInfoList);
        //存入智能合约执行结果记录
        contractService.saveContractResults(contractResultList);
        //存储token转账信息
        tokenService.saveTokenTransfers(tokenTransferList);

        /*
            涉及到统计类的表放在最后来存储，便于回滚
            凡是统计类的表，都加上一个height字段，说明是统计到什么地方
         */
        //修改账户信息表
        accountService.saveAccounts(accountInfoMap);
        //存储智能合约记录
        contractService.saveContractInfos(contractInfoMap);
        //存储账户token信息
        tokenService.saveAccountTokens(accountTokenMap);
        //完成解析
        blockHeaderService.syncComplete(blockInfo.getBlockHeader().getHeight());
    }


    /**
     * 回滚都是从最后保存的一个区块开始
     *
     * @return boolean
     */
    public boolean rollbackBlock() {
        return false;
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

    private AccountTokenInfo queryAccountTokenInfo(String key) {
        AccountTokenInfo accountTokenInfo = accountTokenMap.get(key);
        if (accountTokenInfo == null) {
            accountTokenInfo = tokenService.getAccountTokenInfo(key);
        }
        return accountTokenInfo;
    }

    private ContractInfo queryContractInfo(String contractAddress) throws Exception {
        ContractInfo contractInfo = contractInfoMap.get(contractAddress);
        if (contractInfo == null) {
            contractInfo = contractService.getContractInfo(contractAddress);
        }
        contractInfoMap.put(contractInfo.getContractAddress(), contractInfo);
        return contractInfo;
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
        contractInfoMap.clear();
        contractResultList.clear();
        accountTokenMap.clear();
        contractTxInfoList.clear();
        tokenTransferList.clear();
    }
}

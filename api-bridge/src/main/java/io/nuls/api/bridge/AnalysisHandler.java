/*
 *
 * MIT License
 *
 * Copyright (c) 2017-2019 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 *
 */
package io.nuls.api.bridge;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.*;
import io.nuls.sdk.core.contast.TransactionConstant;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.exception.NulsException;
import io.nuls.sdk.core.model.*;
import io.nuls.sdk.core.model.transaction.*;
import io.nuls.sdk.core.script.TransactionSignature;
import io.nuls.sdk.core.utils.AddressTool;
import io.nuls.sdk.core.utils.NulsByteBuffer;
import io.nuls.sdk.core.utils.VarInt;
import org.spongycastle.util.Arrays;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 区块数据解析处理器
 */
@Component
public class AnalysisHandler {

    @Autowired
    private WalletRPCHandler rpcHandler;

    public BlockHeaderInfo toBlockHeader(Map<String, Object> map) {
        BlockHeaderInfo info = new BlockHeaderInfo();
        info.setHash((String) map.get("hash"));
        info.setTxCount((Integer) map.get("txCount"));
        info.setSize((Integer) map.get("size"));
        info.setMerkleHash((String) map.get("merkleHash"));
        info.setPreHash((String) map.get("preHash"));
        info.setHeight(Long.parseLong(map.get("height").toString()));
        info.setPackingIndexOfRound((Integer) map.get("packingIndexOfRound"));
        info.setRoundStartTime((Long) map.get("roundStartTime"));
        info.setReward(Long.parseLong(map.get("reward").toString()));
        info.setRoundIndex(Long.parseLong(map.get("roundIndex").toString()));
        info.setTotalFee(Long.parseLong(map.get("fee").toString()));
        info.setCreateTime(Long.parseLong(map.get("time").toString()));
        info.setPackingAddress((String) map.get("packingAddress"));
        info.setScriptSign((String) map.get("scriptSig"));
        return info;
    }

    public BlockInfo toBlock(Block block) throws Exception {
        BlockInfo blockInfo = new BlockInfo();
        BlockHeaderInfo blockHeader = toBlockHeader(block.getHeader());
        blockInfo.setTxs(toTxs(block.getTxs(), blockHeader));
        //计算coinbase奖励
        blockHeader.setReward(calcCoinBaseReward(blockInfo.getTxs().get(0)));
        //计算总手续费
        blockHeader.setTotalFee(calcFee(blockInfo.getTxs()));

        List<String> txHashList = new ArrayList<>();
        for (int i = 0; i < block.getTxs().size(); i++) {
            txHashList.add(blockInfo.getTxs().get(i).getHash());
        }
        blockHeader.setTxHashList(txHashList);
        blockInfo.setBlockHeader(blockHeader);
        return blockInfo;
    }

    private BlockHeaderInfo toBlockHeader(BlockHeader blockHeader) throws Exception {
        BlockHeaderInfo info = new BlockHeaderInfo();
        info.setHash(blockHeader.getHash().getDigestHex());
        info.setHeight(blockHeader.getHeight());
        info.setPreHash(blockHeader.getPreHash().getDigestHex());
        info.setMerkleHash(blockHeader.getMerkleHash().getDigestHex());
        info.setSize(blockHeader.getSize());
        info.setScriptSign(Hex.encode(blockHeader.getBlockSignature().serialize()));
        info.setTxCount(Long.valueOf(blockHeader.getTxCount()).intValue());
        BlockExtendsData extendsData = new BlockExtendsData(blockHeader.getExtend());
        info.setRoundIndex(extendsData.getRoundIndex());
        info.setPackingIndexOfRound(extendsData.getPackingIndexOfRound());
        info.setCreateTime(blockHeader.getTime());
        info.setPackingAddress(AddressTool.getStringAddressByBytes(blockHeader.getPackingAddress()));
        info.setRoundStartTime(extendsData.getRoundStartTime());
        info.setAgentVersion(extendsData.getCurrentVersion() == null ? 1 : extendsData.getCurrentVersion());
        //是否是种子节点打包的区块
        if (ApiContext.SEED_NODE_ADDRESS.contains(info.getPackingAddress()) || info.getHeight() == 0) {
            info.setSeedPacked(true);
        }
        return info;
    }

    private List<TransactionInfo> toTxs(List<Transaction> txList, BlockHeaderInfo blockHeader) throws Exception {
        List<TransactionInfo> txs = new ArrayList<>();
        for (int i = 0; i < txList.size(); i++) {
            TransactionInfo txInfo = toTransaction(txList.get(i));
            if (txInfo.getFroms() != null && !txInfo.getFroms().isEmpty()) {
                if (txInfo.getFroms().get(0).getAddress() == null) {
                    txInfo.setFroms(rpcHandler.queryTxInput(txInfo.getHash()));
                }
            }
            if (txInfo.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                PunishLog punishLog = (PunishLog) txInfo.getTxData();
                punishLog.setRoundIndex(blockHeader.getRoundIndex());
                punishLog.setIndex(blockHeader.getPackingIndexOfRound());
            } else if (txInfo.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
                for (TxData txData : txInfo.getTxDataList()) {
                    PunishLog punishLog = (PunishLog) txData;
                    punishLog.setRoundIndex(blockHeader.getRoundIndex());
                    punishLog.setIndex(blockHeader.getPackingIndexOfRound());
                }
            }
            txs.add(txInfo);
        }
        return txs;
    }

    public TransactionInfo toTransaction(Transaction tx) throws Exception {
        TransactionInfo info = new TransactionInfo();
        info.setHash(tx.getHash().getDigestHex());
        info.setHeight(tx.getBlockHeight());
        info.setFee(tx.getFee().getValue());
        info.setType(tx.getType());
        info.setSize(tx.getSize());
        info.setCreateTime(tx.getTime());
        if (tx.getTxData() != null) {
            info.setTxDataHex(Hex.encode(tx.getTxData().serialize()));
        }
        if (tx.getRemark() != null) {
            try {
                info.setRemark(new String(tx.getRemark(), NulsConstant.DEFAULT_ENCODING));
            } catch (UnsupportedEncodingException e) {
                info.setRemark(Hex.encode(tx.getRemark()));
            }
        }
        info.setFroms(toInputs(tx.getCoinData(), tx));
        info.setTos(toOutputs(tx.getCoinData(), info.getHash()));

        if (info.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
            info.setTxDataList(toTxDataList(tx));
        } else {
            info.setTxData(toTxData(tx));
        }
        return info;
    }

    private List<Input> toInputs(CoinData coinData, Transaction tx) throws NulsException {
        if (coinData == null || coinData.getFrom() == null || coinData.getFrom().isEmpty()) {
            return null;
        }

        String address = null;
        //当交易的签名只有一个时，从签名里取出地址，赋值到每一个input上
        if (tx.getTransactionSignature() != null) {
            TransactionSignature signature = new TransactionSignature();
            signature.parse(new NulsByteBuffer(tx.getTransactionSignature()));
            if (signature.getP2PHKSignatures() != null && signature.getP2PHKSignatures().size() == 1) {
                byte[] addressBytes = AddressTool.getAddress(signature.getP2PHKSignatures().get(0).getPublicKey());
                address = AddressTool.getStringAddressByBytes(addressBytes);
            }
        }

        List<Input> inputs = new ArrayList<>();
        Input input;
        for (Coin coin : coinData.getFrom()) {
            input = new Input();
            input.setKey(Hex.encode(coin.getOwner()));
            input.setValue(coin.getNa().getValue());
            input.setAddress(address);
            inputs.add(input);
        }
        return inputs;
    }


    private List<Output> toOutputs(CoinData coinData, String txHash) {
        if (coinData == null || coinData.getTo() == null || coinData.getTo().isEmpty()) {
            return null;
        }
        List<Output> outPuts = new ArrayList<>();
        Output outPut;
        byte[] txHashBytes = Hex.decode(txHash);
        Coin coin;
        for (int i = 0; i < coinData.getTo().size(); i++) {
            coin = coinData.getTo().get(i);
            outPut = new Output();
            outPut.setKey(Hex.encode(Arrays.concatenate(txHashBytes, new VarInt(i).encode())));
            outPut.setAddress(AddressTool.getStringAddressByBytes(coin.getAddress()));
            outPut.setLockTime(coin.getLockTime());
            outPut.setValue(coin.getNa().getValue());

            outPuts.add(outPut);
        }

        return outPuts;
    }

    private TxData toTxData(Transaction tx) {
        if (tx.getType() == TransactionConstant.TX_TYPE_ALIAS) {
            return toAlias(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_REGISTER_AGENT) {
            return toAgent(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_JOIN_CONSENSUS) {
            return toDeposit(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CANCEL_DEPOSIT) {
            return toCancelDeposit(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_STOP_AGENT) {
            return toStopAgent(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
            return toRedPublishLog(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CREATE_CONTRACT) {
            return toContractCreateInfo(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CALL_CONTRACT) {
            return toContractCallInfo(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_DELETE_CONTRACT) {
            return toContractDeleteInfo(tx);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CONTRACT_TRANSFER) {
            return toContractTransferInfo(tx);
        }
        return null;
    }

    private List<TxData> toTxDataList(Transaction tx) {
        if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
            YellowPunishTransaction yellowPunishTx = (YellowPunishTransaction) tx;
            return toYellowPunishLog(yellowPunishTx);
        }
        return null;
    }

    private TxData toAlias(Transaction tx) {
        AliasTransaction aliasTx = (AliasTransaction) tx;
        Alias model = aliasTx.getTxData();
        AliasInfo info = new AliasInfo();
        info.setAddress(AddressTool.getStringAddressByBytes(model.getAddress()));
        info.setAlias(model.getAlias());
        return info;
    }

    private TxData toAgent(Transaction tx) {
        CreateAgentTransaction agentTransaction = (CreateAgentTransaction) tx;
        Agent model = agentTransaction.getTxData();

        AgentInfo info = new AgentInfo();
        info.setTxHash(tx.getHash().getDigestHex());
        info.setAgentId(info.getTxHash().substring(info.getTxHash().length() - 8));
        info.setAgentAddress(AddressTool.getStringAddressByBytes(model.getAgentAddress()));
        info.setPackingAddress(AddressTool.getStringAddressByBytes(model.getPackingAddress()));
        info.setRewardAddress(AddressTool.getStringAddressByBytes(model.getRewardAddress()));
        info.setDeposit(model.getDeposit().getValue());
        info.setBlockHeight(tx.getBlockHeight());
        info.setCommissionRate(Double.valueOf(model.getCommissionRate()).intValue());
        info.setStatus(model.getStatus());
        info.setDepositCount(model.getMemberCount());
        info.setTotalDeposit(model.getTotalDeposit());
        info.setCreditValue(model.getCreditVal());
        info.setCreateTime(tx.getTime());
        info.setNew(true);
        return info;
    }

    private DepositInfo toDeposit(Transaction tx) {
        DepositTransaction depositTx = (DepositTransaction) tx;
        Deposit deposit = depositTx.getTxData();

        DepositInfo info = new DepositInfo();
        info.setTxHash(tx.getHash().getDigestHex());
        info.setAmount(deposit.getDeposit().getValue());
        info.setAgentHash(deposit.getAgentHash().getDigestHex());
        info.setAddress(AddressTool.getStringAddressByBytes(deposit.getAddress()));
        info.setTxHash(tx.getHash().getDigestHex());
        info.setCreateTime(tx.getTime());
        info.setBlockHeight(tx.getBlockHeight());
        info.setFee(tx.getFee().getValue());
        info.setKey(info.getTxHash() + info.getAddress());
        return info;
    }

    private DepositInfo toCancelDeposit(Transaction tx) {
        CancelDepositTransaction cancelDepositTx = (CancelDepositTransaction) tx;
        CancelDeposit cancelDeposit = cancelDepositTx.getTxData();
        DepositInfo deposit = new DepositInfo();
        deposit.setTxHash(cancelDeposit.getJoinTxHash().getDigestHex());
        deposit.setFee(tx.getFee().getValue());
        deposit.setCreateTime(tx.getTime());
        deposit.setType(NulsConstant.CANCEL_CONSENSUS);
        return deposit;
    }

    private AgentInfo toStopAgent(Transaction tx) {
        StopAgentTransaction stopAgentTx = (StopAgentTransaction) tx;
        StopAgent stopAgent = stopAgentTx.getTxData();
        AgentInfo agentNode = new AgentInfo();
        agentNode.setTxHash(stopAgent.getCreateTxHash().getDigestHex());
        return agentNode;
    }

    private List<TxData> toYellowPunishLog(YellowPunishTransaction tx) {
        YellowPunishData model = tx.getTxData();
        List<TxData> logList = new ArrayList<>();
        for (byte[] address : model.getAddressList()) {
            PunishLog log = new PunishLog();
            log.setTxHash(tx.getHash().getDigestHex());
            log.setAddress(AddressTool.getStringAddressByBytes(address));
            log.setBlockHeight(tx.getBlockHeight());
            log.setTime(tx.getTime());
            log.setType(NulsConstant.PUBLISH_YELLOW);
            log.setReason("No packaged blocks");
            logList.add(log);
        }
        return logList;
    }

    private PunishLog toRedPublishLog(Transaction tx) {
        RedPunishTransaction redPunishTx = (RedPunishTransaction) tx;
        RedPunishData model = redPunishTx.getTxData();

        PunishLog punishLog = new PunishLog();
        punishLog.setTxHash(tx.getHash().getDigestHex());
        punishLog.setType(NulsConstant.PUTLISH_RED);
        punishLog.setAddress(AddressTool.getStringAddressByBytes(model.getAddress()));
        if (model.getReasonCode() == NulsConstant.TRY_FORK) {
            punishLog.setReason("Trying to bifurcate many times");
        } else if (model.getReasonCode() == NulsConstant.DOUBLE_SPEND) {
            punishLog.setReason("double-send tx in the block");
        } else if (model.getReasonCode() == NulsConstant.TOO_MUCH_YELLOW_PUNISH) {
            punishLog.setReason("too much yellow publish");
        }

        punishLog.setBlockHeight(tx.getBlockHeight());
        punishLog.setTime(tx.getTime());
        return punishLog;

    }

    private ContractInfo toContractCreateInfo(Transaction tx) {
        CreateContractTransaction createContractTx = (CreateContractTransaction) tx;
        CreateContractData model = createContractTx.getTxData();
        ContractInfo contractInfo = new ContractInfo();
        contractInfo.setCreateTxHash(tx.getHash().getDigestHex());
        contractInfo.setContractAddress(AddressTool.getStringAddressByBytes(model.getContractAddress()));
        contractInfo.setBlockHeight(tx.getBlockHeight());
        contractInfo.setCreateTime(tx.getTime());

        RpcClientResult<ContractInfo> clientResult1 = rpcHandler.getContractInfo(contractInfo);
        if (!clientResult1.isSuccess()) {
            if (clientResult1.getCode() == NulsConstant.CONTRACT_NOT_EXIST) {
                return contractInfo;
            } else {
                throw new RuntimeException(clientResult1.getMsg());
            }
        }
        return clientResult1.getData();
    }

    private ContractCallInfo toContractCallInfo(Transaction tx) {
        CallContractTransaction callContractTx = (CallContractTransaction) tx;
        CallContractData contractData = callContractTx.getTxData();

        ContractCallInfo callInfo = new ContractCallInfo();
        callInfo.setCreater(AddressTool.getStringAddressByBytes(contractData.getSender()));
        callInfo.setContractAddress(AddressTool.getStringAddressByBytes(contractData.getContractAddress()));
        callInfo.setGasLimit(contractData.getGasLimit());
        callInfo.setPrice(contractData.getPrice());
        callInfo.setMethodName(contractData.getMethodName());
        callInfo.setMethodDesc(contractData.getMethodDesc());
        String args = "";
        String[][] arrays = contractData.getArgs();
        if (arrays != null) {
            for (String[] arg : arrays) {
                if (arg != null) {
                    for (String s : arg) {
                        args = args + s + ",";
                    }
                }
            }
        }
        callInfo.setArgs(args);
        return callInfo;
    }

    private ContractDeleteInfo toContractDeleteInfo(Transaction tx) {
        DeleteContractTransaction deleteContractTx = (DeleteContractTransaction) tx;
        DeleteContractData model = deleteContractTx.getTxData();
        ContractDeleteInfo info = new ContractDeleteInfo();
        info.setTxHash(tx.getHash().getDigestHex());
        info.setCreater(AddressTool.getStringAddressByBytes(model.getSender()));
        info.setContractAddress(AddressTool.getStringAddressByBytes(model.getContractAddress()));
        return info;
    }

    private ContractTransferInfo toContractTransferInfo(Transaction tx) {
        ContractTransferTransaction contractTransferTx = (ContractTransferTransaction) tx;
        ContractTransferData model = contractTransferTx.getTxData();
        ContractTransferInfo info = new ContractTransferInfo();
        info.setContractAddress(AddressTool.getStringAddressByBytes(model.getContractAddress()));
        info.setOrginTxHash(model.getOrginTxHash().getDigestHex());
        return info;

    }

    public ContractResultInfo toContractResult(String hash, Map<String, Object> map) {
        ContractResultInfo resultInfo = new ContractResultInfo();
        map = (Map<String, Object>) map.get("data");
        if (map != null) {
            resultInfo.setTxHash(hash);
            resultInfo.setErrorMessage((String) map.get("errorMessage"));
            resultInfo.setSuccess((Boolean) map.get("success"));
            resultInfo.setResult((String) map.get("result"));
            resultInfo.setContractAddress((String) map.get("contractAddress"));

            resultInfo.setGasUsed(map.get("gasUsed") != null ? Long.parseLong(map.get("gasUsed").toString()) : 0);
            resultInfo.setGasLimit(map.get("gasLimit") != null ? Long.parseLong(map.get("gasLimit").toString()) : 0);
            resultInfo.setPrice(map.get("price") != null ? Long.parseLong(map.get("price").toString()) : 0);
            resultInfo.setTotalFee(map.get("totalFee") != null ? Long.parseLong(map.get("totalFee").toString()) : 0);
            resultInfo.setTxSizeFee(map.get("txSizeFee") != null ? Long.parseLong(map.get("txSizeFee").toString()) : 0);
            resultInfo.setActualContractFee(map.get("actualContractFee") != null ? Long.parseLong(map.get("actualContractFee").toString()) : 0);
            resultInfo.setRefundFee(map.get("refundFee") != null ? Long.parseLong(map.get("refundFee").toString()) : 0);
            resultInfo.setTxValue(map.get("value") != null ? Long.parseLong(map.get("value").toString()) : 0);
            resultInfo.setBalance(map.get("balance") != null ? Long.parseLong(map.get("balance").toString()) : 0);
            resultInfo.setNonce(map.get("nonce") != null ? Long.parseLong(map.get("nonce").toString()) : 0);
            resultInfo.setRemark(map.get("remark") != null ? (String) map.get("remark") : "");

            resultInfo.setTokenName((String) map.get("name"));
            resultInfo.setSymbol((String) map.get("symbol"));
            resultInfo.setDecimals(map.get("decimals") != null ? Long.parseLong(map.get("decimals").toString()) : 0);

            List transfers = (List) map.get("transfers");
            if (transfers != null && transfers.size() > 0) {
                resultInfo.setNulsTransfers(toNulsTransfer(transfers));
            }

            List listTokenTransfers = (List) map.get("tokenTransfers");
            if (listTokenTransfers != null && listTokenTransfers.size() > 0) {
                resultInfo.setTokenTransfers(toTokenTransfers(listTokenTransfers));
            }
        }
        return resultInfo;
    }

    public List<NulsTransfer> toNulsTransfer(List<Map<String, Object>> mapList) {
        List<NulsTransfer> nulsTransfers = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            NulsTransfer nulsTransfer = new NulsTransfer();
            nulsTransfer.setTxHash((String) map.get("orginTxHash"));
            nulsTransfer.setFromAddress((String) map.get("from"));
            nulsTransfer.setToAddress((String) map.get("to"));
            nulsTransfer.setValue(Long.parseLong(map.get("value").toString()));

            nulsTransfers.add(nulsTransfer);
        }
        return nulsTransfers;
    }

    public List<TokenTransfer> toTokenTransfers(List<Map<String, Object>> mapList) {
        List<TokenTransfer> tokenTransfers = new ArrayList<>();
        for (Map<String, Object> map : mapList) {
            TokenTransfer tokenTransfer = new TokenTransfer();
            tokenTransfer.setContractAddress((String) map.get("contractAddress"));
            tokenTransfer.setFromAddress((String) map.get("from"));
            tokenTransfer.setToAddress((String) map.get("to"));
            tokenTransfer.setValue((String) map.get("value"));
            tokenTransfer.setSymbol((String) map.get("symbol"));
            tokenTransfer.setName((String) map.get("name"));
            tokenTransfer.setDecimals((Integer) map.get("decimals"));
            tokenTransfers.add(tokenTransfer);
        }
        return tokenTransfers;
    }


    public ContractInfo toContractInfo(ContractInfo contractInfo, Map<String, Object> map) {
        contractInfo.setCreateTxHash((String) map.get("createTxHash"));
        contractInfo.setContractAddress((String) map.get("address"));
        contractInfo.setCreater((String) map.get("creater"));
        contractInfo.setCreateTime(Long.parseLong(map.get("createTime").toString()));
        contractInfo.setBlockHeight(Long.parseLong(map.get("blockHeight").toString()));
        contractInfo.setIsNrc20(Boolean.parseBoolean(map.get("isNrc20").toString()) ? 1 : 0);
        //如果是NRC20需要解析代币信息
        if (Boolean.parseBoolean(map.get("isNrc20").toString())) {
            contractInfo.setTokenName((String) map.get("nrc20TokenName"));
            contractInfo.setSymbol((String) map.get("nrc20TokenSymbol"));
            contractInfo.setDecimals(Integer.parseInt(map.get("decimals").toString()));
            contractInfo.setTotalSupply((map.get("totalSupply").toString()));
        }
        contractInfo.setStatus(0);
        List<Map<String, Object>> methodMap = (List<Map<String, Object>>) map.get("method");
        List<ContractMethod> methodList = new ArrayList<>();
        for (Map<String, Object> map1 : methodMap) {
            ContractMethod method = new ContractMethod();
            method.setName((String) map1.get("name"));
            String returnArg = (String) map1.get("returnArg");
            method.setReturnType(returnArg);

            List<Map<String, Object>> argsList = (List<Map<String, Object>>) map1.get("args");
            List<String> paramList = new ArrayList<>();
            for (Map<String, Object> arg : argsList) {
                paramList.add((String) arg.get("name"));
            }
            method.setParams(paramList);
            methodList.add(method);
        }
        contractInfo.setMethods(methodList);
        return contractInfo;
    }

    public AgentInfo toAgentInfo(Map<String, Object> map) {
        AgentInfo agentInfo = new AgentInfo();
        agentInfo.setTxHash((String) map.get("agentHash"));
        agentInfo.setCreditValue((Double) map.get("creditVal"));
        agentInfo.setDepositCount((Integer) map.get("memberCount"));
        return agentInfo;
    }

    /**
     * 计算每个区块的coinbase奖励
     *
     * @param coinBaseTx coinbase交易
     * @return
     */
    private Long calcCoinBaseReward(TransactionInfo coinBaseTx) {
        long reward = 0;
        if (coinBaseTx.getTos() == null) {
            return 0L;
        }

        for (Output outPut : coinBaseTx.getTos()) {
            reward += outPut.getValue();
        }

        return reward;
    }

    private Long calcFee(List<TransactionInfo> txs) {
        long fee = 0;
        for (int i = 1; i < txs.size(); i++) {
            fee += txs.get(i).getFee();
        }
        return fee;
    }

}

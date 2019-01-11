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

import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.AgentInfo;
import io.nuls.api.core.model.AliasInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.DepositInfo;
import io.nuls.api.core.model.*;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.sdk.core.contast.TransactionConstant;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.exception.NulsException;
import io.nuls.sdk.core.model.*;
import io.nuls.sdk.core.model.transaction.*;
import io.nuls.sdk.core.script.TransactionSignature;
import io.nuls.sdk.core.utils.AddressTool;
import io.nuls.sdk.core.utils.JSONUtils;
import io.nuls.sdk.core.utils.NulsByteBuffer;
import io.nuls.sdk.core.utils.VarInt;
import org.spongycastle.util.Arrays;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 区块数据解析处理器
 */
public class AnalysisHandler {

    public static BlockHeaderInfo toBlockHeader(Map<String, Object> map) {
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

    public static BlockInfo toBlock(Block block) throws Exception {
        BlockInfo blockInfo = new BlockInfo();

        blockInfo.setTxs(toTxs(block.getTxs()));
        BlockHeaderInfo blockHeader = toBlockHeader(block.getHeader());
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

    private static BlockHeaderInfo toBlockHeader(BlockHeader blockHeader) throws Exception {
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
        //是否是种子节点打包的区块
        if (NulsConstant.SEED_NODE_ADDRESS.contains(info.getPackingAddress())) {
            info.setSeedPacked(true);
        }
        return info;
    }

    private static List<TransactionInfo> toTxs(List<Transaction> txList) throws Exception {
        List<TransactionInfo> txs = new ArrayList<>();
        for (int i = 0; i < txList.size(); i++) {
            txs.add(toTransaction(txList.get(i)));
        }
        return txs;
    }

    public static TransactionInfo toTransaction(Transaction tx) throws Exception {
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
            info.setRemark(Hex.encode(tx.getRemark()));
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

    private static List<Input> toInputs(CoinData coinData, Transaction tx) throws NulsException {
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


    private static List<Output> toOutputs(CoinData coinData, String txHash) {
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

    private static TxData toTxData(Transaction tx) throws Exception {
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

    private static List<TxData> toTxDataList(Transaction tx) {
        if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
            YellowPunishTransaction yellowPunishTx = (YellowPunishTransaction) tx;
            return toYellowPunishLog(yellowPunishTx);
        }
        return null;
    }

    private static TxData toAlias(Transaction tx) {
        AliasTransaction aliasTx = (AliasTransaction) tx;
        Alias model = aliasTx.getTxData();
        AliasInfo info = new AliasInfo();
        info.setAddress(AddressTool.getStringAddressByBytes(model.getAddress()));
        info.setAlias(model.getAlias());
        return info;
    }

    private static TxData toAgent(Transaction tx) {
        CreateAgentTransaction agentTransaction = (CreateAgentTransaction) tx;
        Agent model = agentTransaction.getTxData();

        AgentInfo info = new AgentInfo();
        info.setTxHash(tx.getHash().getDigestHex());
        info.setAgentId(info.getTxHash().substring(info.getTxHash().length() - 8));
        info.setAgentAddress(AddressTool.getStringAddressByBytes(model.getAgentAddress()));
        info.setPackingAddress(AddressTool.getStringAddressByBytes(model.getPackingAddress()));
        info.setRewardAddress(AddressTool.getStringAddressByBytes(model.getRewardAddress()));
        info.setDeposit(model.getDeposit().getValue());
        info.setCommissionRate(Double.valueOf(model.getCommissionRate()).intValue());
        info.setBlockHeight(tx.getBlockHeight());
        info.setStatus(model.getStatus());
        info.setDepositCount(model.getMemberCount());
        info.setTotalDeposit(model.getTotalDeposit());
        info.setCreditValue(model.getCreditVal());
        info.setCreateTime(tx.getTime());
        info.setTxHash(tx.getHash().getDigestHex());
        info.setNew(true);
        return info;
    }

    private static DepositInfo toDeposit(Transaction tx) {
        DepositTransaction depositTx = (DepositTransaction) tx;
        Deposit deposit = depositTx.getTxData();

        DepositInfo info = new DepositInfo();
        info.setTxHash(tx.getHash().getDigestHex());
        info.setAmount(deposit.getDeposit().getValue());
        info.setAgentHash(deposit.getAgentHash().getDigestHex());
        info.setAddress(AddressTool.getStringAddressByBytes(deposit.getAddress()));
        info.setTxHash(tx.getHash().getDigestHex());
        info.setBlockHeight(tx.getBlockHeight());
        info.setCreateTime(tx.getTime());
        info.setFee(tx.getFee().getValue());
        return info;
    }

    private static DepositInfo toCancelDeposit(Transaction tx) {
        CancelDepositTransaction cancelDepositTx = (CancelDepositTransaction) tx;
        CancelDeposit cancelDeposit = cancelDepositTx.getTxData();
        DepositInfo deposit = new DepositInfo();
        deposit.setTxHash(cancelDeposit.getJoinTxHash().getDigestHex());
        deposit.setFee(tx.getFee().getValue());
        deposit.setBlockHeight(tx.getBlockHeight());
        deposit.setCreateTime(tx.getTime());
        deposit.setType(NulsConstant.CANCEL_CONSENSUS);
        return deposit;
    }

    private static AgentInfo toStopAgent(Transaction tx) {
        StopAgentTransaction stopAgentTx = (StopAgentTransaction) tx;
        StopAgent stopAgent = stopAgentTx.getTxData();
        AgentInfo agentNode = new AgentInfo();
        agentNode.setTxHash(stopAgent.getCreateTxHash().getDigestHex());
        return agentNode;
    }

    private static List<TxData> toYellowPunishLog(YellowPunishTransaction tx) {
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

    private static PunishLog toRedPublishLog(Transaction tx) {
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


    private static ContractCreateInfo toContractCreateInfo(Transaction tx) throws Exception {
        CreateContractTransaction createContractTx = (CreateContractTransaction) tx;
        CreateContractData model = createContractTx.getTxData();
        ContractCreateInfo contractInfo = new ContractCreateInfo();

        contractInfo.setCreater(AddressTool.getStringAddressByBytes(model.getSender()));
        contractInfo.setContractAddress(AddressTool.getStringAddressByBytes(model.getContractAddress()));
        contractInfo.setContractCode(Hex.encode(model.getCode()));
        contractInfo.setGasLimit(model.getGasLimit());
        contractInfo.setPrice(model.getPrice());
        contractInfo.setArgs(JSONUtils.obj2json(model.getArgs()));

        return contractInfo;
    }

    private static ContractCallInfo toContractCallInfo(Transaction tx) {
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

    private static ContractDeleteInfo toContractDeleteInfo(Transaction tx) {
        DeleteContractTransaction deleteContractTx = (DeleteContractTransaction) tx;
        DeleteContractData model = deleteContractTx.getTxData();
        ContractDeleteInfo info = new ContractDeleteInfo();
        info.setCreater(AddressTool.getStringAddressByBytes(model.getSender()));
        info.setContractAddress(AddressTool.getStringAddressByBytes(model.getContractAddress()));
        return info;
    }

    private static ContractTransferInfo toContractTransferInfo(Transaction tx) {
        ContractTransferTransaction contractTransferTx = (ContractTransferTransaction) tx;
        ContractTransferData model = contractTransferTx.getTxData();
        ContractTransferInfo info = new ContractTransferInfo();
        info.setContractAddress(AddressTool.getStringAddressByBytes(model.getContractAddress()));
        info.setOrginTxHash(model.getOrginTxHash().getDigestHex());
        return info;

    }

    public static ContractResultInfo toContractResult(Map<String, Object> map) throws Exception {
        ContractResultInfo resultInfo = new ContractResultInfo();
        map = (Map<String, Object>) map.get("data");
        if (map != null) {
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

            ArrayList listEvents = (ArrayList) map.get("events");
            if (listEvents != null && listEvents.size() > 0) {
                resultInfo.setEvents(JSONUtils.obj2json(listEvents));
            }
            ArrayList listTransfers = (ArrayList) map.get("transfers");
            if (listTransfers != null && listTransfers.size() > 0) {
                resultInfo.setTransfers(JSONUtils.obj2json(listTransfers));
            }
            ArrayList listTokenTransfers = (ArrayList) map.get("tokenTransfers");
            if (listTokenTransfers != null && listTokenTransfers.size() > 0) {
                resultInfo.setTokenTransfers(JSONUtils.obj2json(listTokenTransfers));
            }
        }
        return resultInfo;
    }


    public static ContractInfo toContractInfo(Map<String, Object> map) throws Exception {
        ContractInfo contractInfo = new ContractInfo();
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
            contractInfo.setDecimals(Long.parseLong(map.get("decimals").toString()));
            contractInfo.setTotalSupply(map.get("totalSupply").toString());
        }
        contractInfo.setStatus(0);
        contractInfo.setMethods(JSONUtils.obj2json(map.get("method")));
        return contractInfo;
    }

    /**
     * 计算每个区块的coinbase奖励
     *
     * @param coinBaseTx coinbase交易
     * @return
     */
    private static Long calcCoinBaseReward(TransactionInfo coinBaseTx) {
        long reward = 0;
        if (coinBaseTx.getTos() == null) {
            return 0L;
        }

        for (Output outPut : coinBaseTx.getTos()) {
            reward += outPut.getValue();
        }

        return reward;
    }


    private static Long calcFee(List<TransactionInfo> txs) {
        long fee = 0;
        for (int i = 1; i < txs.size(); i++) {
            fee += txs.get(i).getFee();
        }
        return fee;
    }

}

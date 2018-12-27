/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2019 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *  *
 *
 */
package io.nuls.api.bridge;

import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.Agent;
import io.nuls.api.core.model.Alias;
import io.nuls.api.core.model.Block;
import io.nuls.api.core.model.BlockHeader;
import io.nuls.api.core.model.Deposit;
import io.nuls.api.core.model.*;
import io.nuls.api.core.model.Transaction;
import io.nuls.sdk.core.contast.TransactionConstant;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.model.*;
import io.nuls.sdk.core.model.transaction.*;
import io.nuls.sdk.core.utils.AddressTool;
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

    public static BlockHeader toBlockHeader(Map<String, Object> map) {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setHash((String) map.get("hash"));
        blockHeader.setTxCount((Integer) map.get("txCount"));
        blockHeader.setSize((Integer) map.get("size"));
        blockHeader.setMerkleHash((String) map.get("merkleHash"));
        blockHeader.setPreHash((String) map.get("preHash"));
        blockHeader.setHeight(Long.parseLong(map.get("height").toString()));
        blockHeader.setPackingIndexOfRound((Integer) map.get("packingIndexOfRound"));
        blockHeader.setReward(Long.parseLong(map.get("reward").toString()));
        blockHeader.setRoundIndex(Long.parseLong(map.get("roundIndex").toString()));
        blockHeader.setTotalFee(Long.parseLong(map.get("fee").toString()));
        blockHeader.setCreateTime(Long.parseLong(map.get("time").toString()));
        blockHeader.setPackingAddress((String) map.get("packingAddress"));
        blockHeader.setScriptSign((String) map.get("scriptSig"));
        return blockHeader;
    }

    public static Block toBlock(io.nuls.sdk.core.model.Block nulsBlock) throws Exception {
        Block block = new Block();

        block.setTxs(toTxs(nulsBlock.getTxs()));
        BlockHeader blockHeader = toBlockHeader(nulsBlock.getHeader());
        //计算coinbase奖励
        blockHeader.setReward(calcCoinBaseReward(block.getTxs().get(0)));
        //计算总手续费
        blockHeader.setTotalFee(calcFee(block.getTxs()));
        return block;
    }

    private static BlockHeader toBlockHeader(io.nuls.sdk.core.model.BlockHeader nulsBlockHeader) throws Exception {
        BlockHeader blockHeader = new BlockHeader();
        blockHeader.setHash(nulsBlockHeader.getHash().getDigestHex());
        blockHeader.setHeight(nulsBlockHeader.getHeight());
        blockHeader.setPreHash(nulsBlockHeader.getPreHash().getDigestHex());
        blockHeader.setMerkleHash(nulsBlockHeader.getMerkleHash().getDigestHex());
        blockHeader.setSize(nulsBlockHeader.getSize());
        blockHeader.setScriptSign(Hex.encode(nulsBlockHeader.getBlockSignature().serialize()));
        blockHeader.setTxCount(new Long(nulsBlockHeader.getTxCount()).intValue());
        BlockExtendsData extendsData = new BlockExtendsData(nulsBlockHeader.getExtend());
        blockHeader.setRoundIndex(extendsData.getRoundIndex());
        blockHeader.setPackingIndexOfRound(extendsData.getPackingIndexOfRound());
        blockHeader.setCreateTime(nulsBlockHeader.getTime());
        return blockHeader;
    }

    private static List<Transaction> toTxs(List<io.nuls.sdk.core.model.transaction.Transaction> txList) throws Exception {
        List<Transaction> txs = new ArrayList<>();
        for (int i = 0; i < txList.size(); i++) {
            txs.add(toTransaction(txList.get(i)));
        }
        return txs;
    }

    private static Transaction toTransaction(io.nuls.sdk.core.model.transaction.Transaction tx) throws Exception {
        Transaction transaction = new Transaction();
        transaction.setHash(tx.getHash().getDigestHex());
        transaction.setHeight(tx.getBlockHeight());
        transaction.setFee(tx.getFee().getValue());
        transaction.setType(tx.getType());
        transaction.setSize(tx.getSize());
        transaction.setTxDataHex(Hex.encode(tx.getTxData().serialize()));
        transaction.setFroms(toInputs(tx.getCoinData()));
        transaction.setTos(toOutputs(tx.getCoinData(), transaction.getHash()));
        transaction.setRemark(Hex.encode(tx.getRemark()));
        return transaction;
    }

    private static List<Input> toInputs(io.nuls.sdk.core.model.CoinData coinData) {
        if (coinData.getFrom() == null || coinData.getFrom().isEmpty()) {
            return null;
        }
        List<Input> inputs = new ArrayList<>();
        Input input;
        for (io.nuls.sdk.core.model.Coin coin : coinData.getFrom()) {
            input = new Input();
            input.setKey(Hex.encode(coin.getOwner()));
            inputs.add(input);
        }
        return inputs;
    }

    private static List<OutPut> toOutputs(io.nuls.sdk.core.model.CoinData coinData, String txHash) {
        if (coinData.getTo() == null || coinData.getTo().isEmpty()) {
            return null;
        }
        List<OutPut> outPuts = new ArrayList<>();
        OutPut outPut;
        byte[] txHashBytes = Hex.decode(txHash);
        io.nuls.sdk.core.model.Coin coin;
        for (int i = 0; i < coinData.getTo().size(); i++) {
            coin = coinData.getTo().get(i);
            outPut = new OutPut();
            outPut.setKey(Hex.encode(Arrays.concatenate(txHashBytes, new VarInt(i).encode())));
            outPut.setAddress(AddressTool.getStringAddressByBytes(coin.getAddress()));
            outPut.setLockTime(coin.getLockTime());
            outPut.setValue(coin.getNa().getValue());

            outPuts.add(outPut);
        }

        return outPuts;
    }

    private static TxData toTxData(io.nuls.sdk.core.model.transaction.Transaction tx) {
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
            RedPunishTransaction redPunishTransaction = (RedPunishTransaction) tx;
            return toRedPulishLog(redPunishTransaction);
        }
        return null;
    }

    private static List<TxData> toTxDataList(io.nuls.sdk.core.model.transaction.Transaction tx) {
        if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
            YellowPunishTransaction yellowPunishTx = (YellowPunishTransaction) tx;
            return toYellowPunishLog(yellowPunishTx);
        }
        return null;
    }

    private static TxData toAlias(io.nuls.sdk.core.model.transaction.Transaction tx) {
        AliasTransaction aliasTx = (AliasTransaction) tx;
        io.nuls.sdk.core.model.Alias model = aliasTx.getTxData();
        Alias alias = new Alias();
        alias.setAddress(AddressTool.getStringAddressByBytes(model.getAddress()));
        alias.setAlias(model.getAlias());
        return alias;
    }

    private static TxData toAgent(io.nuls.sdk.core.model.transaction.Transaction tx) {
        CreateAgentTransaction agentTransaction = (CreateAgentTransaction) tx;
        io.nuls.sdk.core.model.Agent model = agentTransaction.getTxData();

        Agent agent = new Agent();
        agent.setTxHash(tx.getHash().getDigestHex());
        agent.setAgentAddress(AddressTool.getStringAddressByBytes(model.getAgentAddress()));
        agent.setPackingAddress(AddressTool.getStringAddressByBytes(model.getPackingAddress()));
        agent.setRewardAddress(AddressTool.getStringAddressByBytes(model.getRewardAddress()));
        agent.setDeposit(model.getDeposit().getValue());
        agent.setCommissionRate(new BigDecimal(model.getCommissionRate()));
        agent.setBlockHeight(tx.getBlockHeight());
        agent.setStatus(model.getStatus());
        agent.setDepositCount(model.getMemberCount());
        agent.setCreditValue(new BigDecimal(model.getCreditVal()));
        agent.setCreateTime(tx.getTime());
        agent.setTxHash(tx.getHash().getDigestHex());
        return agent;
    }

    private static Deposit toDeposit(io.nuls.sdk.core.model.transaction.Transaction tx) {
        DepositTransaction depositTx = (DepositTransaction) tx;
        io.nuls.sdk.core.model.Deposit model = depositTx.getTxData();

        Deposit deposit = new Deposit();
        deposit.setTxHash(tx.getHash().getDigestHex());
        deposit.setAmount(model.getDeposit().getValue());
        deposit.setAgentHash(model.getAgentHash().getDigestHex());
        deposit.setAddress(AddressTool.getStringAddressByBytes(model.getAddress()));
        deposit.setTxHash(tx.getHash().getDigestHex());
        deposit.setBlockHeight(tx.getBlockHeight());
        deposit.setCreateTime(tx.getTime());
        return deposit;
    }

    private static Deposit toCancelDeposit(io.nuls.sdk.core.model.transaction.Transaction tx) {
        CancelDepositTransaction cancelDepositTx = (CancelDepositTransaction) tx;
        CancelDeposit cancelDeposit = cancelDepositTx.getTxData();
        Deposit deposit = new Deposit();
        deposit.setTxHash(cancelDeposit.getJoinTxHash().getDigestHex());
        return deposit;
    }

    private static Agent toStopAgent(io.nuls.sdk.core.model.transaction.Transaction tx) {
        StopAgentTransaction stopAgentTx = (StopAgentTransaction) tx;
        StopAgent stopAgent = stopAgentTx.getTxData();
        Agent agentNode = new Agent();
        agentNode.setTxHash(stopAgent.getCreateTxHash().getDigestHex());
        return agentNode;
    }

    private static List<TxData> toYellowPunishLog(YellowPunishTransaction tx) {

        YellowPunishData model = tx.getTxData();
        List<TxData> logList = new ArrayList<>();
        for (byte[] address : model.getAddressList()) {
            PunishLog log = new PunishLog();
            log.setAddress(AddressTool.getStringAddressByBytes(address));
            log.setBlockHeight(tx.getBlockHeight());
            log.setTime(tx.getTime());
            log.setType(NulsConstant.PUBLISH_YELLOW);
            log.setReason("No packaged blocks");
            logList.add(log);
        }
        return logList;
    }

    private static PunishLog toRedPulishLog(RedPunishTransaction tx) {
        RedPunishData model = tx.getTxData();

        PunishLog punishLog = new PunishLog();
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
//        punishLog.setRoundIndex(header.getRoundIndex());
        //        punishLog.setReason(new String (model.get);
        return punishLog;

    }

    /**
     * 计算每个区块的coinbase奖励
     *
     * @param coinBaseTx coinbase交易
     * @return
     */
    private static Long calcCoinBaseReward(Transaction coinBaseTx) {
        long reward = 0;
        if (coinBaseTx.getTos() == null) {
            return 0L;
        }

        for (OutPut outPut : coinBaseTx.getTos()) {
            reward += outPut.getValue();
        }

        return reward;
    }


    private static Long calcFee(List<Transaction> txs) {
        long fee = 0;
        for (int i = 1; i < txs.size(); i++) {
            fee += txs.get(i).getFee();
        }
        return fee;
    }

}

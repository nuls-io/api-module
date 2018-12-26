package io.nuls.api.bridge;

import io.nuls.api.core.model.*;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.model.BlockExtendsData;
import io.nuls.sdk.core.utils.AddressTool;
import io.nuls.sdk.core.utils.VarInt;
import org.spongycastle.util.Arrays;

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
//        for(OutPut outPut : coinBaseTx.getTos()) {
//            reward += outPut.getValue();
//        }

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

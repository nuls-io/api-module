package io.nuls.api.core.model;

import java.util.List;

public class BlockInfo {

    private BlockHeaderInfo blockHeader;

    private List<TransactionInfo> txs;

    public BlockHeaderInfo getBlockHeader() {
        return blockHeader;
    }

    public void setBlockHeader(BlockHeaderInfo blockHeader) {
        this.blockHeader = blockHeader;
    }

    public List<TransactionInfo> getTxs() {
        return txs;
    }

    public void setTxs(List<TransactionInfo> txs) {
        this.txs = txs;
    }
}

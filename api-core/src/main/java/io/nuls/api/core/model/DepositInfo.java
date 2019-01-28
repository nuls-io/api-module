package io.nuls.api.core.model;

public class DepositInfo extends TxData {

    private String txHash;

    private Long amount;

    private String agentHash;

    private String address;

    private Long createTime;

    private String deleteHash;

    private long blockHeight;

    private long deleteHeight;

    private Long fee;

    private boolean isNew;
    // 0 加入共识，1 退出共识
    private int type;

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash == null ? null : txHash.trim();
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }

    public String getAgentHash() {
        return agentHash;
    }

    public void setAgentHash(String agentHash) {
        this.agentHash = agentHash == null ? null : agentHash.trim();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address == null ? null : address.trim();
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public String getDeleteHash() {
        return deleteHash;
    }

    public void setDeleteHash(String deleteHash) {
        this.deleteHash = deleteHash == null ? null : deleteHash.trim();
    }

    public long getDeleteHeight() {
        return deleteHeight;
    }

    public void setDeleteHeight(long deleteHeight) {
        this.deleteHeight = deleteHeight;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public void copyInfoWithDeposit(DepositInfo depositInfo) {
        this.amount = depositInfo.amount;
        this.address = depositInfo.address;
        this.agentHash = depositInfo.getAgentHash();
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }
}
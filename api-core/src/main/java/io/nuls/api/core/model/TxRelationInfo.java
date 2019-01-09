package io.nuls.api.core.model;

public class TxRelationInfo {

    private String txHash;

    private String address;

    private Integer type;

    private Long createTime;

    private Long height;

    private Long values;

    private Long fee;

    private long balance;

    public TxRelationInfo() {

    }

    public TxRelationInfo(String address, TransactionInfo info, long values, long balance) {
        this.address = address;
        this.txHash = info.getHash();
        this.type = info.getType();
        this.createTime = info.getCreateTime();
        this.fee = info.getFee();
        this.height = info.getHeight();
        this.values = values;
        this.balance = balance;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address == null ? null : address.trim();
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash == null ? null : txHash.trim();
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TxRelationInfo) {
            TxRelationInfo info = (TxRelationInfo) obj;
            if (this.address.equals(info.address) && this.txHash.equals(info.txHash)) {
                return true;
            }
        }
        return false;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Long getValues() {
        return values;
    }

    public void setValues(Long values) {
        this.values = values;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }
}
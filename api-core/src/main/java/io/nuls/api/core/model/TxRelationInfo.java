package io.nuls.api.core.model;

public class TxRelationInfo {

    public TxRelationInfo() {

    }

    public TxRelationInfo(String address, String txHash) {
        this.address = address;
        this.txHash = txHash;
    }

    public TxRelationInfo(String address, TransactionInfo info) {
        this.address = address;
        this.txHash = info.getHash();
        this.type = info.getType();
        this.createTime = info.getCreateTime();
    }

    private Long id;

    private String address;

    private String txHash;

    private Integer type;

    private Long createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
}
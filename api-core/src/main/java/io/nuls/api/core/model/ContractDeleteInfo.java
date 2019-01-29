package io.nuls.api.core.model;


public class ContractDeleteInfo extends TxData {
    private String txHash;

    private String contractAddress;

    private String creater;

    public ContractDeleteInfo() {

    }

    public String getCreater() {
        return creater;
    }

    public void setCreater(String creater) {
        this.creater = creater;
    }

    public String getContractAddress() {
        return contractAddress;
    }

    public void setContractAddress(String contractAddress) {
        this.contractAddress = contractAddress;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }
}
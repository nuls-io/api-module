package io.nuls.api.core.model;

import io.nuls.sdk.core.model.Address;

public class AccountInfo {

    private String address;

    private String alias;

    private int type;

    private int txCount;

    private long totalOut;

    private long totalIn;

    private long consensusLock;

    private long timeLock;

    private long balance;

    private long totalBalance;

    //记录最新更改账户统计信息的高度
    private long height;
    //是否是根据最新区块的交易新创建的账户，只为业务使用，不存储该字段
    private boolean isNew;

    public AccountInfo() {
    }

    public AccountInfo(String address) {
        this.address = address;
        Address address1 = new Address(address);
        this.type = address1.getAddressType();
        this.isNew = true;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getTxCount() {
        return txCount;
    }

    public void setTxCount(int txCount) {
        this.txCount = txCount;
    }

    public long getTotalOut() {
        return totalOut;
    }

    public void setTotalOut(long totalOut) {
        this.totalOut = totalOut;
    }

    public long getTotalIn() {
        return totalIn;
    }

    public void setTotalIn(long totalIn) {
        this.totalIn = totalIn;
    }

    public long getConsensusLock() {
        return consensusLock;
    }

    public void setConsensusLock(long consensusLock) {
        this.consensusLock = consensusLock;
    }

    public long getTimeLock() {
        return timeLock;
    }

    public void setTimeLock(long timeLock) {
        this.timeLock = timeLock;
    }

    public long getBalance() {
        return balance;
    }

    public void setBalance(long balance) {
        this.balance = balance;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public long getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(long totalBalance) {
        this.totalBalance = totalBalance;
    }
}

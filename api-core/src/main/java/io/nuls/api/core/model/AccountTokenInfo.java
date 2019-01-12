package io.nuls.api.core.model;

import java.math.BigInteger;

public class AccountTokenInfo {

    private String address;

    private String tokenSymbol;

    private BigInteger balance;

    private int decimals;

    private boolean isNew;

    public AccountTokenInfo() {

    }

    public AccountTokenInfo(String address, String tokenSymbol) {
        this.address = address;
        this.tokenSymbol = tokenSymbol;
        this.balance = BigInteger.ZERO;
        this.isNew = true;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTokenSymbol() {
        return tokenSymbol;
    }

    public void setTokenSymbol(String tokenSymbol) {
        this.tokenSymbol = tokenSymbol;
    }

    public BigInteger getBalance() {
        return balance;
    }

    public void setBalance(BigInteger balance) {
        this.balance = balance;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public int getDecimals() {
        return decimals;
    }

    public void setDecimals(int decimals) {
        this.decimals = decimals;
    }
}

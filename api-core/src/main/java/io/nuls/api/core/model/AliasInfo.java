package io.nuls.api.core.model;

public class AliasInfo extends TxData {

    private String address;

    private String alias;

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
}

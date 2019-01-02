package io.nuls.api.core.model;

import java.util.List;

public class BlockRelationInfo {

    private Long height;

    private String preHash;

    private String hash;

    private List<String> txHashList;

    //是否同步完成：0 未完成， 1 已完成
    private int syncFinish;

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public int getSyncFinish() {
        return syncFinish;
    }

    public void setSyncFinish(int syncFinish) {
        this.syncFinish = syncFinish;
    }

    public List<String> getTxHashList() {
        return txHashList;
    }

    public void setTxHashList(List<String> txHashList) {
        this.txHashList = txHashList;
    }

    public String getPreHash() {
        return preHash;
    }

    public void setPreHash(String preHash) {
        this.preHash = preHash;
    }
}

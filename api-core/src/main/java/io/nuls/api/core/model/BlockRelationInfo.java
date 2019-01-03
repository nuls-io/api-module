package io.nuls.api.core.model;

import io.nuls.sdk.core.utils.StringUtils;
import org.bson.Document;

import java.util.List;

public class BlockRelationInfo {

    private Long height;

    private String preHash;

    private String hash;

    private List<String> txHashList;

    private String agentInfo;

    public BlockRelationInfo() {

    }

    public BlockRelationInfo(Document document) {
        this.hash = document.getString("hash");
        this.height = document.getLong("height");
        this.preHash = document.getString("preHash");
        this.txHashList = (List) document.get("txHashList");
    }


    public BlockRelationInfo(BlockHeaderInfo blockHeaderInfo) {
        this.hash = blockHeaderInfo.getHash();
        this.preHash = blockHeaderInfo.getPreHash();
        this.txHashList = blockHeaderInfo.getTxHashList();
        this.height = blockHeaderInfo.getHeight();
    }


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

    public String getAgentInfo() {
        return agentInfo;
    }

    public void setAgentInfo(String agentInfo) {
        this.agentInfo = agentInfo;
    }

    public void setAgentInfo(AgentInfo agentInfo) {
        if (StringUtils.isNotBlank(agentInfo.getAgentAlias())) {
            this.agentInfo = agentInfo.getAgentAlias();
        }
        this.agentInfo = agentInfo.getAgentId();
    }
}

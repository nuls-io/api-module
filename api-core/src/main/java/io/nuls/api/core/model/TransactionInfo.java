package io.nuls.api.core.model;

import io.nuls.sdk.core.contast.TransactionConstant;
import org.bson.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TransactionInfo {

    private String hash;

    private Integer type;

    private Long height;

    private Integer size;

    private Long fee;

    private Long createTime;

    private String remark;

    private String txDataHex;

    private List<Input> froms;

    private List<Output> tos;

    private TxData txData;

    private List<TxData> txDataList;

    private long value;

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Long getHeight() {
        return height;
    }

    public void setHeight(Long height) {
        this.height = height;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public Long getFee() {
        return fee;
    }

    public void setFee(Long fee) {
        this.fee = fee;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getTxDataHex() {
        return txDataHex;
    }

    public void setTxDataHex(String txDataHex) {
        this.txDataHex = txDataHex;
    }

    public TxData getTxData() {
        return txData;
    }

    public void setTxData(TxData txData) {
        this.txData = txData;
    }

    public List<Input> getFroms() {
        return froms;
    }

    public void setFroms(List<Input> froms) {
        this.froms = froms;
    }

    public List<Output> getTos() {
        return tos;
    }

    public void setTos(List<Output> tos) {
        this.tos = tos;
    }

    public List<TxData> getTxDataList() {
        return txDataList;
    }

    public void setTxDataList(List<TxData> txDataList) {
        this.txDataList = txDataList;
    }

    public Long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Long createTime) {
        this.createTime = createTime;
    }

    public void setValue(long value) {
        this.value = value;
    }

    public long getValue() {
        return value;
    }

    public void calcValue() {
        long value = 0;
        if (type == TransactionConstant.TX_TYPE_COINBASE ||
                type == TransactionConstant.TX_TYPE_STOP_AGENT ||
                type == TransactionConstant.TX_TYPE_CANCEL_DEPOSIT) {
            if (tos != null) {
                for (Output output : tos) {
                    value += output.getValue();
                }
            }
        } else if (type == TransactionConstant.TX_TYPE_TRANSFER ||
                type == TransactionConstant.TX_TYPE_ALIAS ||
                type == TransactionConstant.TX_TYPE_CONTRACT_TRANSFER) {
            Set<String> addressSet = new HashSet<>();
            for (Input input : froms) {
                addressSet.add(input.getAddress());
            }
            for (Output output : tos) {
                if (!addressSet.contains(output.getAddress())) {
                    value += output.getValue();
                }
            }
        } else if (type == TransactionConstant.TX_TYPE_REGISTER_AGENT || type == TransactionConstant.TX_TYPE_JOIN_CONSENSUS) {
            for (Output output : tos) {
                if (output.getLockTime() == -1) {
                    value += output.getValue();
                }
            }
        } else {
            value = this.fee;
        }
        this.value = value;
    }

    public Document toDocument() {
        Document document = new Document();
        document.append("_id", hash).append("height", height).append("createTime", createTime).append("type", type).append("value", value).append("fee", fee);
        return document;
    }

    public static TransactionInfo fromDocument(Document document) {
        TransactionInfo info = new TransactionInfo();
        info.setHash(document.getString("_id"));
        info.setHeight(document.getLong("height"));
        info.setCreateTime(document.getLong("createTime"));
        info.setType(document.getInteger("type"));
        info.setFee(document.getLong("fee"));
        info.setValue(document.getLong("value"));
        return info;
    }
}

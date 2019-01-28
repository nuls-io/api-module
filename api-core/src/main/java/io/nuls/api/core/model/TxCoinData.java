package io.nuls.api.core.model;

import io.nuls.sdk.core.utils.JSONUtils;

public class TxCoinData {

    private String txHash;

    private String inputsJson;

    private String outputsJson;

    public TxCoinData() {
    }

    public TxCoinData(TransactionInfo tx) throws Exception {
        this.txHash = tx.getHash();
        if (tx.getFroms() != null && !tx.getFroms().isEmpty()) {
            this.inputsJson = JSONUtils.obj2json(tx.getFroms());
        }
        if (tx.getTos() != null && !tx.getTos().isEmpty()) {
            this.outputsJson = JSONUtils.obj2json(tx.getTos());
        }
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getInputsJson() {
        return inputsJson;
    }

    public void setInputsJson(String inputsJson) {
        this.inputsJson = inputsJson;
    }

    public String getOutputsJson() {
        return outputsJson;
    }

    public void setOutputsJson(String outputsJson) {
        this.outputsJson = outputsJson;
    }
}

package io.nuls.api.service;

import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.Input;
import io.nuls.api.core.model.Output;
import io.nuls.api.core.model.TxCoinData;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.accountledger.utils.LedgerUtil;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.model.CoinData;
import io.nuls.sdk.core.utils.JSONUtils;
import io.nuls.sdk.core.utils.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class UTXOService {

    @Autowired
    private MongoDBService mongoDBService;

    /**
     * 根据input和output更新utxo记录
     * input里的都删除，output里的都新增
     *
     * @param inputs
     * @param outputMap
     */
    public void saveOutputs(List<Input> inputs, Map<String, Output> outputMap) {
        if (inputs.isEmpty() && outputMap.isEmpty()) {
            return;
        }

        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (Input input : inputs) {
            modelList.add(new DeleteOneModel(Filters.eq("_id", input.getKey())));
        }

        for (Output output : outputMap.values()) {
            modelList.add(new InsertOneModel<>(DocumentTransferTool.toDocument(output, "key")));
        }

        mongoDBService.bulkWrite(MongoTableName.UTXO_INFO, modelList);
    }

    public void rollbackOutputs(List<Input> inputs, Map<String, Output> outputMap) throws Exception {
        if (inputs.isEmpty() && outputMap.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (Output output : outputMap.values()) {
            modelList.add(new DeleteOneModel<>(Filters.eq("_id", output.getKey())));
        }
        //回滚input的时候，需要通过input的key找到对应的上一笔交易，然后通过index找到output，重新写入数据库中
        byte[] fromBytes;
        TxCoinData fromCoinData;
        List<Output> outputs = new ArrayList<>();
        Output from;
        for (Input input : inputs) {
            outputs.clear();
            fromBytes = Hex.decode(input.getKey());
            String txHash = LedgerUtil.getTxHash(fromBytes);
            int index = LedgerUtil.getIndex(fromBytes);
            fromCoinData = getTxCoinData(txHash);
            if (StringUtils.isNotBlank(fromCoinData.getOutputsJson())) {
                outputs = JSONUtils.json2list(fromCoinData.getOutputsJson(), Output.class);
            }
            from = outputs.get(index);
            modelList.add(new InsertOneModel<>(DocumentTransferTool.toDocument(from, "key")));
        }
        mongoDBService.bulkWrite(MongoTableName.UTXO_INFO, modelList);
    }


    public List<Output> getAccountUtxos(String address) {
        List<Output> outputs = new ArrayList<>();
        Bson filter = Filters.eq("address", address);
        List<Document> docsList = this.mongoDBService.query(MongoTableName.UTXO_INFO, filter);
        if (docsList != null) {
            for (Document document : docsList) {
                outputs.add(DocumentTransferTool.toInfo(document, "key", Output.class));
            }
        }
        return outputs;
    }

    public void saveCoinDatas(List<TxCoinData> coinDataList) {
        if (coinDataList.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (TxCoinData coinData : coinDataList) {
            Document document = DocumentTransferTool.toDocument(coinData, "txHash");
            documentList.add(document);
        }
        mongoDBService.insertMany(MongoTableName.COINDATA_INFO, documentList);
    }

    public TxCoinData getTxCoinData(String txHash) {
        Document document = mongoDBService.findOne(MongoTableName.COINDATA_INFO, Filters.eq("_id", txHash));
        if (document == null) {
            return null;
        }
        TxCoinData coinData = DocumentTransferTool.toInfo(document, "txHash", TxCoinData.class);
        return coinData;
    }

}

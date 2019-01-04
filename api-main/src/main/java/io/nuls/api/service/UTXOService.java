package io.nuls.api.service;

import com.mongodb.client.model.DeleteOneModel;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.WriteModel;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.model.Input;
import io.nuls.api.core.model.Output;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;

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
    public void saveWithInputOutput(List<Input> inputs, Map<String, Output> outputMap) {
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

    }
}

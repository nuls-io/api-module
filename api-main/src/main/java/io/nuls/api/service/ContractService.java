package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.ContractInfo;
import io.nuls.api.core.model.ContractMethod;
import io.nuls.api.core.model.ContractTxInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.core.utils.JSONUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Component
public class ContractService {

    @Autowired
    private MongoDBService mongoDBService;

    public ContractInfo getContractInfo(String contractAddress) throws Exception {
        Document document = mongoDBService.findOne(MongoTableName.CONTRACT_INFO, Filters.eq("contractAddress", contractAddress));
        if (document == null) {
            return null;
        }
        ContractInfo tokenInfo = DocumentTransferTool.toInfo(document, "contractAddress", ContractInfo.class);
        tokenInfo.setMethods(JSONUtils.json2list(tokenInfo.getMethodStr(), ContractMethod.class));
        return tokenInfo;
    }

    public void saveContractInfos(List<ContractInfo> contractInfoList) throws Exception {
        if (contractInfoList.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (ContractInfo contractInfo : contractInfoList) {
            Document document = DocumentTransferTool.toDocument(contractInfo, "contractAddress");
            document.remove("methods");
            String methodStr = null;
            if (contractInfo.getMethods() != null) {
                methodStr = JSONUtils.obj2json(contractInfo.getMethods());
            }
            document.put("methodStr", methodStr);
            if (contractInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", contractInfo.getContractAddress()), document));
            }
        }
        mongoDBService.bulkWrite(MongoTableName.CONTRACT_INFO, modelList);
    }

    public void saveContractTxInfos(List<ContractTxInfo> contractTxInfos) {
        if (contractTxInfos.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (ContractTxInfo txInfo : contractTxInfos) {
            Document document = DocumentTransferTool.toDocument(txInfo);
            documentList.add(document);
        }
        mongoDBService.insertMany(MongoTableName.CONTRACT_TX_INFO, documentList);
    }

}

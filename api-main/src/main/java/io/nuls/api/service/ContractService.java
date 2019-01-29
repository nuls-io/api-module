package io.nuls.api.service;

import com.mongodb.client.model.*;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.*;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.core.utils.JSONUtils;
import io.nuls.sdk.core.utils.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class ContractService {

    @Autowired
    private MongoDBService mongoDBService;
    @Autowired
    private AccountService accountService;

    public ContractInfo getContractInfo(String contractAddress) throws Exception {
        Document document = mongoDBService.findOne(MongoTableName.CONTRACT_INFO, Filters.eq("_id", contractAddress));
        if (document == null) {
            return null;
        }
        ContractInfo tokenInfo = DocumentTransferTool.toInfo(document, "contractAddress", ContractInfo.class);
        tokenInfo.setMethods(JSONUtils.json2list(tokenInfo.getMethodStr(), ContractMethod.class));
        tokenInfo.setMethodStr(null);
        return tokenInfo;
    }

    public ContractInfo getContractInfoByHash(String txHash) throws Exception {
        Document document = mongoDBService.findOne(MongoTableName.CONTRACT_INFO, Filters.eq("createTxHash", txHash));
        if (document == null) {
            return null;
        }
        ContractInfo tokenInfo = DocumentTransferTool.toInfo(document, "contractAddress", ContractInfo.class);
        tokenInfo.setMethods(JSONUtils.json2list(tokenInfo.getMethodStr(), ContractMethod.class));
        tokenInfo.setMethodStr(null);
        return tokenInfo;
    }

    public void saveContractInfos(Map<String, ContractInfo> contractInfoMap) throws Exception {
        if (contractInfoMap.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (ContractInfo contractInfo : contractInfoMap.values()) {
            AccountInfo accountInfo = accountService.getAccountInfo(contractInfo.getContractAddress());
            if (accountInfo == null) {
                contractInfo.setBalance(0);
            } else {
                contractInfo.setBalance(accountInfo.getTotalBalance());
            }
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


    public void saveContractResults(List<ContractResultInfo> contractResultInfos) throws Exception {
        if (contractResultInfos.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (ContractResultInfo resultInfo : contractResultInfos) {
            Document document = DocumentTransferTool.toDocument(resultInfo, "txHash");
            document.remove("nulsTransfers");
            document.remove("tokenTransfers");

            String str = null;
            if (resultInfo.getNulsTransfers() != null) {
                str = JSONUtils.obj2json(resultInfo.getNulsTransfers());
            }
            document.append("nulsTransfers", str);

            str = null;
            if (resultInfo.getTokenTransfers() != null) {
                str = JSONUtils.obj2json(resultInfo.getTokenTransfers());
            }
            document.append("tokenTransfers", str);

            documentList.add(document);
        }
        mongoDBService.insertMany(MongoTableName.CONTRACT_RESULT_INFO, documentList);
    }


    public PageInfo<ContractTxInfo> getContractTxList(String contractAddress, int type, int pageNumber, int pageSize) {
        Bson filter;
        if (type == 0) {
            filter = Filters.eq("contractAddress", contractAddress);
        } else {
            filter = Filters.and(Filters.eq("contractAddress", contractAddress), Filters.eq("type", type));
        }
        Bson sort = Sorts.descending("time");
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.CONTRACT_TX_INFO, filter, sort, pageNumber, pageSize);
        List<ContractTxInfo> contractTxInfos = new ArrayList<>();
        long totalCount = mongoDBService.getCount(MongoTableName.CONTRACT_TX_INFO, filter);
        for (Document document : docsList) {
            contractTxInfos.add(DocumentTransferTool.toInfo(document, ContractTxInfo.class));
        }
        PageInfo<ContractTxInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, contractTxInfos);
        return pageInfo;
    }

    public PageInfo<ContractInfo> getContractList(int pageNumber, int pageSize, boolean onlyNrc20, boolean isHidden) {
        Bson filter = null;
        if (onlyNrc20) {
            filter = Filters.eq("isNrc20", 1);
        } else if (isHidden) {
            filter = Filters.ne("isNrc20", 1);
        }
        Bson sort = Sorts.descending("createTime");
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.CONTRACT_INFO, filter, sort, pageNumber, pageSize);
        List<ContractInfo> contractInfos = new ArrayList<>();
        long totalCount = mongoDBService.getCount(MongoTableName.CONTRACT_INFO, filter);

        for (Document document : docsList) {
            ContractInfo contractInfo = DocumentTransferTool.toInfo(document, "contractAddress", ContractInfo.class);
            contractInfo.setMethodStr("");
            contractInfos.add(contractInfo);
        }
        PageInfo<ContractInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, contractInfos);
        return pageInfo;
    }

    public ContractResultInfo getContractResultInfo(String txHash) throws Exception {
        Document document = mongoDBService.findOne(MongoTableName.CONTRACT_RESULT_INFO, Filters.eq("_id", txHash));
        if (document == null) {
            return null;
        }
        ContractResultInfo contractResultInfo = DocumentTransferTool.toInfo(document, "txHash", ContractResultInfo.class);
        if (StringUtils.isNotBlank(contractResultInfo.getTokenTransferStr())) {
            contractResultInfo.setTokenTransfers(JSONUtils.json2list(contractResultInfo.getTokenTransferStr(), TokenTransfer.class));
        }
        if (StringUtils.isNotBlank(contractResultInfo.getNulsTransferStr())) {
            contractResultInfo.setNulsTransfers(JSONUtils.json2list(contractResultInfo.getNulsTransferStr(), NulsTransfer.class));
        }
        return contractResultInfo;
    }
}

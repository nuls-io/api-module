package io.nuls.api.service;

import com.mongodb.client.model.*;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AccountTokenInfo;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.model.TokenTransfer;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.core.utils.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class TokenService {

    @Autowired
    private MongoDBService mongoDBService;


    public AccountTokenInfo getAccountTokenInfo(String key) {
        Bson query = Filters.eq("_id", key);

        Document document = mongoDBService.findOne(MongoTableName.ACCOUNT_TOKEN_INFO, query);
        if (document == null) {
            return null;
        }
        AccountTokenInfo tokenInfo = DocumentTransferTool.toInfo(document, "key", AccountTokenInfo.class);
        return tokenInfo;
    }

    public void saveAccountTokens(Map<String, AccountTokenInfo> accountTokenInfos) {
        if (accountTokenInfos.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AccountTokenInfo tokenInfo : accountTokenInfos.values()) {
            Document document = DocumentTransferTool.toDocument(tokenInfo, "key");
            if (tokenInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", tokenInfo.getKey()), document));
            }
        }
        mongoDBService.bulkWrite(MongoTableName.ACCOUNT_TOKEN_INFO, modelList);
    }


    public PageInfo<AccountTokenInfo> getAccountTokens(String address, int pageNumber, int pageSize) {
        Bson query = Filters.eq("address", address);
        Bson sort = Sorts.descending("balance");
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.ACCOUNT_TOKEN_INFO, query, sort, pageNumber, pageSize);
        List<AccountTokenInfo> accountTokenList = new ArrayList<>();
        long totalCount = mongoDBService.getCount(MongoTableName.ACCOUNT_TOKEN_INFO, query);
        for (Document document : docsList) {
            accountTokenList.add(DocumentTransferTool.toInfo(document, "key", AccountTokenInfo.class));
        }
        PageInfo<AccountTokenInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, accountTokenList);
        return pageInfo;
    }

    public PageInfo<AccountTokenInfo> getContractTokens(String contractAddress, int pageNumber, int pageSize) {
        Bson query = Filters.eq("contractAddress", contractAddress);
        Bson sort = Sorts.descending("balance");
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.ACCOUNT_TOKEN_INFO, query, sort, pageNumber, pageSize);
        List<AccountTokenInfo> accountTokenList = new ArrayList<>();
        long totalCount = mongoDBService.getCount(MongoTableName.ACCOUNT_TOKEN_INFO, query);
        for (Document document : docsList) {
            accountTokenList.add(DocumentTransferTool.toInfo(document, "key", AccountTokenInfo.class));
        }
        PageInfo<AccountTokenInfo> pageInfo = new PageInfo<>(pageNumber, pageSize, totalCount, accountTokenList);
        return pageInfo;
    }

    public void saveTokenTransfers(List<TokenTransfer> tokenTransfers) {
        if (tokenTransfers.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (TokenTransfer tokenTransfer : tokenTransfers) {
            Document document = DocumentTransferTool.toDocument(tokenTransfer);
            documentList.add(document);
        }
        mongoDBService.insertMany(MongoTableName.TOKEN_TRANSFER_INFO, documentList);
    }

    public void rollbackTokenTransfers(List<String> tokenTxHashs, long height) {
        if (tokenTxHashs.isEmpty()) {
            return;
        }
        mongoDBService.delete(MongoTableName.TOKEN_TRANSFER_INFO, Filters.eq("height", height));
    }

    public PageInfo<TokenTransfer> getTokenTransfers(String address, String contractAddress, int pageIndex, int pageSize) {
        return this.getTokenTransfersWithTimestamp(address, contractAddress, pageIndex, pageSize, null, null);
    }

    public PageInfo<TokenTransfer> getTokenTransfersWithTimestamp(String address, String contractAddress, int pageIndex, int pageSize, Long start, Long end) {
        Bson filter;
        if (StringUtils.isNotBlank(address) && StringUtils.isNotBlank(contractAddress)) {
            Bson addressFilter = Filters.or(Filters.eq("fromAddress", address), Filters.eq("toAddress", address));
            filter = Filters.and(Filters.eq("contractAddress", contractAddress), addressFilter);
        } else if (StringUtils.isNotBlank(contractAddress)) {
            filter = Filters.eq("contractAddress", contractAddress);
        } else {
            filter = Filters.or(Filters.eq("fromAddress", address), Filters.eq("toAddress", address));
        }

        if(start != null && end != null) {
            Bson timeCondition = Filters.and(Filters.gte("time", start), Filters.lte("time", end));
            filter = Filters.and(filter, timeCondition);
        }

        Bson sort = Sorts.descending("time");
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.TOKEN_TRANSFER_INFO, filter, sort, pageIndex, pageSize);
        List<TokenTransfer> tokenTransfers = new ArrayList<>();
        long totalCount = mongoDBService.getCount(MongoTableName.TOKEN_TRANSFER_INFO, filter);
        for (Document document : docsList) {
            tokenTransfers.add(DocumentTransferTool.toInfo(document, TokenTransfer.class));
        }

        PageInfo<TokenTransfer> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, tokenTransfers);
        return pageInfo;
    }

}

package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.InsertOneModel;
import com.mongodb.client.model.ReplaceOneModel;
import com.mongodb.client.model.WriteModel;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AccountTokenInfo;
import io.nuls.api.core.model.TokenTransfer;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

@Component
public class TokenService {

    @Autowired
    private MongoDBService mongoDBService;


    public AccountTokenInfo getAccountTokenInfo(String address, String contractAddress) {
        Bson query = Filters.eq("key", address + contractAddress);

        Document document = mongoDBService.findOne(MongoTableName.ACCOUNT_TOKEN_INFO, query);
        if (document == null) {
            return null;
        }
        AccountTokenInfo tokenInfo = DocumentTransferTool.toInfo(document, AccountTokenInfo.class);
        return tokenInfo;
    }

    public void saveAccountTokens(List<AccountTokenInfo> accountTokenInfos) {
        if (accountTokenInfos.isEmpty()) {
            return;
        }
        List<WriteModel<Document>> modelList = new ArrayList<>();
        for (AccountTokenInfo tokenInfo : accountTokenInfos) {
            Document document = DocumentTransferTool.toDocument(tokenInfo);
            if (tokenInfo.isNew()) {
                modelList.add(new InsertOneModel(document));
            } else {
                modelList.add(new ReplaceOneModel<>(Filters.eq("_id", tokenInfo.getKey()), document));
            }
        }
        mongoDBService.bulkWrite(MongoTableName.ACCOUNT_TOKEN_INFO, modelList);
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
}

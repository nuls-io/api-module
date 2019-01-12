package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AccountTokenInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

@Component
public class NRC20Sever {

    @Autowired
    private MongoDBService mongoDBService;

    public AccountTokenInfo getAccountTokenInfo(String address, String tokenSymbol) {
        Bson query = Filters.and(Filters.eq("address", address), Filters.eq("tokenSymbol", tokenSymbol));

        Document document = mongoDBService.findOne(MongoTableName.ACCOUNT_TOKEN_INFO, query);
        if (document == null) {
            return null;
        }
        AccountTokenInfo tokenInfo = DocumentTransferTool.toInfo(document, AccountTokenInfo.class);
        return tokenInfo;
    }
}

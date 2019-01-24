package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AliasInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Component
public class AliasService {

    @Autowired
    private MongoDBService mongoDBService;

    public AliasInfo getAliasByAddress(String address) {
        Document document = mongoDBService.findOne(MongoTableName.ALIAS_INFO, Filters.eq("_id", address));
        if (document == null) {
            return null;
        }
        AliasInfo aliasInfo = DocumentTransferTool.toInfo(document, "address", AliasInfo.class);
        return aliasInfo;
    }

    public AliasInfo getAlias(String alias) {
        Document document = mongoDBService.findOne(MongoTableName.ALIAS_INFO, Filters.eq("alias", alias));
        if (document == null) {
            return null;
        }
        AliasInfo aliasInfo = DocumentTransferTool.toInfo(document, AliasInfo.class);
        return aliasInfo;
    }

    public void saveAliasList(List<AliasInfo> aliasInfoList) {
        if (aliasInfoList.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (AliasInfo aliasInfo : aliasInfoList) {
            Document document = DocumentTransferTool.toDocument(aliasInfo, "address");
            documentList.add(document);
        }
        mongoDBService.insertMany(MongoTableName.ALIAS_INFO, documentList);
    }
}

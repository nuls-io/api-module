package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.AliasInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class AliasService {

    private static final Map<String, AliasInfo> aliasMap = new HashMap<>();

    @Autowired
    private MongoDBService mongoDBService;

    private void initCache() {
        if (!aliasMap.isEmpty()) {
            return;
        }
        List<Document> documentList = mongoDBService.query(MongoTableName.ALIAS_INFO);
        for (Document document : documentList) {
            AliasInfo info = DocumentTransferTool.toInfo(document, "address", AliasInfo.class);
            aliasMap.put(info.getAlias(), info);
            aliasMap.put(info.getAddress(), info);
        }
    }

    public AliasInfo getAliasByAddress(String address) {
        this.initCache();
        return aliasMap.get(address);
//        Document document = mongoDBService.findOne(MongoTableName.ALIAS_INFO, Filters.eq("_id", address));
//        if (document == null) {
//            return null;
//        }
//        AliasInfo aliasInfo = DocumentTransferTool.toInfo(document, "address", AliasInfo.class);
//        return aliasInfo;
    }

    public AliasInfo getAlias(String alias) {
        this.initCache();
        return aliasMap.get(alias);
//        Document document = mongoDBService.findOne(MongoTableName.ALIAS_INFO, Filters.eq("alias", alias));
//        if (document == null) {
//            return null;
//        }
//        AliasInfo aliasInfo = DocumentTransferTool.toInfo(document, AliasInfo.class);
//        return aliasInfo;
    }

    public void saveAliasList(List<AliasInfo> aliasInfoList) {
        if (aliasInfoList.isEmpty()) {
            return;
        }
        List<Document> documentList = new ArrayList<>();
        for (AliasInfo info : aliasInfoList) {
            Document document = DocumentTransferTool.toDocument(info, "address");
            documentList.add(document);
            aliasMap.put(info.getAlias(), info);
            aliasMap.put(info.getAddress(), info);
        }
        mongoDBService.insertMany(MongoTableName.ALIAS_INFO, documentList);
    }

    public void rollbackAliasList(List<AliasInfo> aliasInfoList) {
        if (aliasInfoList.isEmpty()) {
            return;
        }
        List<String> list = new ArrayList<>();
        for (AliasInfo aliasInfo : aliasInfoList) {
            list.add(aliasInfo.getAddress());
            aliasMap.remove(aliasInfo.getAddress());
            aliasMap.remove(aliasInfo.getAlias());
        }
        mongoDBService.delete(MongoTableName.ALIAS_INFO, Filters.in("_id", list));
    }
}

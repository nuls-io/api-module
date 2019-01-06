package io.nuls.api.service;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.PunishLog;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Component
public class PunishService {

    @Autowired
    private MongoDBService mongoDBService;


    public void savePunishList(List<PunishLog> punishLogList) {
        if (punishLogList.isEmpty()) {
            return;
        }

        List<Document> documentList = new ArrayList<>();
        for (PunishLog punishLog : punishLogList) {
            documentList.add(DocumentTransferTool.toDocument(punishLog));
        }

        mongoDBService.insertMany(MongoTableName.PUNISH_INFO, documentList);
    }

}

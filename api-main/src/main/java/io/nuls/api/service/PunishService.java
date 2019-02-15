package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.model.PunishLog;
import io.nuls.api.core.model.TxData;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Filter;

import static com.mongodb.client.model.Filters.*;

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

    /**
     * 根据类型和地址获取红黄牌列表
     *
     * @param type    1：yellow,2:red
     * @param address
     * @return
     */
    public PageInfo<PunishLog> getPunishLogList(int type, String address, int pageIndex, int pageSize) {
        Bson filter;
        if (type == 0) {
            filter = Filters.eq("address", address);
        } else {
            filter = Filters.and(eq("type", type), eq("address", address));
        }

        long totalCount = mongoDBService.getCount(MongoTableName.PUNISH_INFO, filter);
        List<Document> documentList = mongoDBService.pageQuery(MongoTableName.PUNISH_INFO, filter, Sorts.descending("time"), pageIndex, pageSize);
        List<PunishLog> punishLogList = new ArrayList<>();
        for (Document document : documentList) {
            punishLogList.add(DocumentTransferTool.toInfo(document, PunishLog.class));
        }
        PageInfo<PunishLog> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, punishLogList);
        return pageInfo;
    }

    public long getYellowCount(String agentAddress) {
        Bson filter = and(eq("type", 1), eq("address", agentAddress));
        long count = mongoDBService.getCount(MongoTableName.PUNISH_INFO, filter);
        return count;
    }

    public PunishLog getRedPunishLog(String txHash) {
        Document document = mongoDBService.findOne(MongoTableName.PUNISH_INFO, Filters.eq("txHash", txHash));
        if (document == null) {
            return null;
        }
        PunishLog punishLog = DocumentTransferTool.toInfo(document, PunishLog.class);
        return punishLog;
    }

    public List<TxData> getYellowPunishLog(String txHash) {
        List<Document> documentList = mongoDBService.query(MongoTableName.PUNISH_INFO, Filters.eq("txHash", txHash));
        List<TxData> punishLogs = new ArrayList<>();
        for (Document document : documentList) {
            PunishLog punishLog = DocumentTransferTool.toInfo(document, PunishLog.class);
            punishLogs.add(punishLog);
        }
        return punishLogs;
    }

    public void rollbackPunishLog(List<String> txHashs, long height) {
        if (txHashs.isEmpty()) {
            return;
        }

        mongoDBService.delete(MongoTableName.PUNISH_INFO, Filters.eq("blockHeight", height));
    }
}

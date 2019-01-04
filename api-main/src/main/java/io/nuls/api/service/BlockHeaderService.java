package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;

@Component
public class BlockHeaderService {

    @Autowired
    private MongoDBService mongoDBService;

    /**
     * 获取本地存储的最新区块头
     *
     * @return BlockHeaderInfo 最新的区块头
     */
    public BlockHeaderInfo getBestBlockHeader() {
        Document document = mongoDBService.findOne(MongoTableName.NEW_INFO, Filters.eq("_id", MongoTableName.BEST_BLOCK_HEIGHT));
        if (document == null) {
            return null;
        }
        return getBlockHeaderInfoByHeight(document.getLong(MongoTableName.BEST_BLOCK_HEIGHT));
    }

    /**
     * 按照高度获取区块头
     *
     * @param height 高度
     * @return BlockHeaderInfo
     */
    public BlockHeaderInfo getBlockHeaderInfoByHeight(long height) {
        Document document = mongoDBService.findOne(MongoTableName.BLOCK_HEADER, Filters.eq("_id", height));
        if (document == null) {
            return null;
        }
        return DocumentTransferTool.toInfo(document, "height", BlockHeaderInfo.class);
    }

    /**
     * 存区块头
     *
     * @param blockHeaderInfo 区块头
     */
    public void saveBLockHeaderInfo(BlockHeaderInfo blockHeaderInfo) {
        Document document = DocumentTransferTool.toDocument(blockHeaderInfo, "height");
        mongoDBService.insertOne(MongoTableName.BLOCK_HEADER, document);
    }
}

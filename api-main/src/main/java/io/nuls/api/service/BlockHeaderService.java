package io.nuls.api.service;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import io.nuls.sdk.core.utils.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

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
        return getBlockHeaderInfoByHeight(document.getLong("height"));
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

    /**
     * 分页查询区块列表，支持按照出快地址过滤
     */
    public PageInfo<BlockHeaderInfo> pageQuery(int pageIndex, int pageSize, String packingAddress, boolean filterEmptyBlocks) {
        Bson filter = null;
        if (StringUtils.isNotBlank(packingAddress)) {
            filter = Filters.eq("packingAddress", packingAddress);
        }
        if (filterEmptyBlocks) {
            if (filter == null) {
                filter = Filters.gt("txCount", 1);
            } else {
                filter = Filters.and(filter, Filters.gt("txCount", 1));
            }
        }
        long totalCount = mongoDBService.getCount(MongoTableName.BLOCK_HEADER, filter);
        List<Document> docsList = this.mongoDBService.pageQuery(MongoTableName.BLOCK_HEADER, filter, Sorts.descending("height"), pageIndex, pageSize);
        List<BlockHeaderInfo> list = new ArrayList<>();
        for (Document document : docsList) {
            list.add(DocumentTransferTool.toInfo(document, "height", BlockHeaderInfo.class));
        }
        PageInfo<BlockHeaderInfo> pageInfo = new PageInfo<>(pageIndex, pageSize, totalCount, list);
        return pageInfo;
    }
}

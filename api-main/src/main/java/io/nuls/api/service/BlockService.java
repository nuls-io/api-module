package io.nuls.api.service;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.model.BlockRelationInfo;
import io.nuls.api.core.mongodb.MongoDBService;

@Component
public class BlockService {

    @Autowired
    private MongoDBService mongoDBService;

    /**
     * 获取本地存储的最新区块关系表
     *
     * @return BlockRelationInfo 最新的区块关系记录
     */
    public BlockRelationInfo getBestBlockRelation() {
        // mongoDBService.findMax("height",)
        return null;
    }


}

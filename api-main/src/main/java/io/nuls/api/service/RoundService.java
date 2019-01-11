/*
 * MIT License
 * Copyright (c) 2017-2019 nuls.io
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.nuls.api.service;

import com.mongodb.client.model.Sorts;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.constant.MongoTableName;
import io.nuls.api.core.model.PocRound;
import io.nuls.api.core.model.PocRoundItem;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.core.util.DocumentTransferTool;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * @author Niels
 */
@Component
public class RoundService {


    @Autowired
    private MongoDBService mongoDBService;

    public void saveRound(PocRound round) {
        Document document = DocumentTransferTool.toDocument(round, "index");
        this.mongoDBService.insertOne(MongoTableName.ROUND_INFO, document);
    }

    public void saveRoundItem(PocRoundItem item) {
        Document document = DocumentTransferTool.toDocument(item, "id");
        this.mongoDBService.insertOne(MongoTableName.ROUND_ITEM_INFO, document);
    }

    public void saveRoundItemList(List<PocRoundItem> itemList) {
        List<Document> docsList = new ArrayList<>();
        for (PocRoundItem item : itemList) {
            Document document = DocumentTransferTool.toDocument(item, "id");
            docsList.add(document);
        }
        this.mongoDBService.insertMany(MongoTableName.ROUND_ITEM_INFO, docsList);
    }

    public long updateRound(PocRound round) {
        Document document = DocumentTransferTool.toDocument(round, "index");
        return this.mongoDBService.updateOne(MongoTableName.ROUND_INFO, eq("_id", round.getIndex()), document);
    }

    public long updateRoundItem(PocRoundItem item) {
        Document document = DocumentTransferTool.toDocument(item, "id");
        return this.mongoDBService.updateOne(MongoTableName.ROUND_ITEM_INFO, eq("_id", item.getId()), document);
    }

    public void removeRound(long roundIndex) {
        this.mongoDBService.delete(MongoTableName.ROUND_INFO, eq("_id", roundIndex));
        this.mongoDBService.delete(MongoTableName.ROUND_ITEM_INFO, eq("roundIndex", roundIndex));
    }

    public long getTotalCount() {
        return this.mongoDBService.getCount(MongoTableName.ROUND_INFO);
    }

    public List<PocRound> getRoundList(int pageIndex, int pageSize) {
        List<Document> list = this.mongoDBService.pageQuery(MongoTableName.ROUND_INFO, Sorts.descending("index"), pageIndex, pageSize);
        List<PocRound> roundList = new ArrayList<>();
        for (Document document : list) {
            roundList.add(DocumentTransferTool.toInfo(document, "index", PocRound.class));
        }
        return roundList;
    }

    public PocRound getRound(long roundIndex) {
        Document document = this.mongoDBService.findOne(MongoTableName.ROUND_INFO, eq("_id", roundIndex));
        if (null == document) {
            return null;
        }
        return DocumentTransferTool.toInfo(document, "index", PocRound.class);
    }

    public List<PocRoundItem> getRoundItemList(long roundIndex) {

        List<Document> list = this.mongoDBService.query(MongoTableName.ROUND_INFO, eq("roundIndex", roundIndex), Sorts.ascending("order"));
        List<PocRoundItem> itemList = new ArrayList<>();
        for (Document document : list) {
            itemList.add(DocumentTransferTool.toInfo(document, PocRoundItem.class));
        }
        return itemList;
    }
}

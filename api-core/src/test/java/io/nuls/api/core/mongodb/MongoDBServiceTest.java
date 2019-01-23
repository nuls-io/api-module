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

package io.nuls.api.core.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import io.nuls.api.core.constant.MongoTableName;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.junit.Test;

import java.util.*;

import static com.mongodb.client.model.Filters.eq;
import static org.bson.codecs.configuration.CodecRegistries.*;

public class MongoDBServiceTest {

    @Test
    public void testInsert() {
        CodecRegistry codecRegistry = fromRegistries(MongoClient.getDefaultCodecRegistry(), fromProviders(
                PojoCodecProvider.builder().register("com.mongodb.models").build()));
        MongoClientOptions.Builder options = new MongoClientOptions.Builder().codecRegistry(codecRegistry);
        MongoClientURI uri = new MongoClientURI("mongodb://localhost/test?retryWrites=true", options);
        // 连接到 mongodb 服务
        MongoClient mongoClient = new MongoClient(uri);

        mongoClient.startSession();
        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);
//        service.createCollection("relations");

        long start = System.currentTimeMillis();
        //插入1亿条数据
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < 8200000; i++) {
            map.put("address", UUID.randomUUID().toString());
            map.put("type", i % 20);
            map.put("height", i % 20000000);
            map.put("hash", UUID.randomUUID().toString() + UUID.randomUUID().toString());
            map.put("time", System.currentTimeMillis());
            service.insertOne("relations-j", map);
            if (i % 10000 == 0) {
                System.out.println((1 + i) + "::::" + (System.currentTimeMillis() - start));
            }
        }

    }

    @Test
    public void testCreateIndex() {

        // 连接到 mongodb 服务
        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);


        String result = service.createIndex("relations", new Document("height", 1));
        System.out.println(result);

    }

    @Test
    public void testCount() {

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);
        MongoCollection<Document> collection = service.getCollection("relations");
        System.out.println(collection.estimatedDocumentCount());
    }


    @Test
    public void testQuery() {

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 100000000; i++) {
            List<Document> list = service.query("relations", eq("height", i % 20000000));
            if (i % 100000 == 0) {
                System.out.println((1 + i) + "::::" + (System.currentTimeMillis() - start));
            }
        }

    }

    @Test
    public void testFindOne() {

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);

        service.findOne("relations", eq("height", 1000));
    }

    @Test
    public void testSum() {

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("nuls");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);
        MongoCollection<Document> collection = service.getCollection(MongoTableName.ACCOUNT_INFO);


        AggregateIterable<Document> ai = collection.aggregate(Arrays.asList(
                Aggregates.group(null, Accumulators.sum("total", "$totalBalance"))
        ));
        MongoCursor<Document> cursor = ai.iterator();
        while (cursor.hasNext()) {
            System.out.println(cursor.next());
        }
    }


    @Test
    public void testFind() {

        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);

//        Document document = new Document();
////        document.append("_id", "bestHeight").append("height", 6);
////        service.insertOne("newInfo", document);
        Bson bson = Filters.eq("_id", "bestHeight");
        Document document1 = service.findOne("newInfo", bson);
        document1.put("height", 19);
        service.update("newInfo", bson, document1);
        System.out.println(document1);
    }


    @Test
    public void testUpdate() {

        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);

        Document document = service.findOne("newInfo", Filters.eq("_id", "bestHeight"));
        System.out.println(document);
    }

    @Test
    public void testBlockRelationInfo() {
//        BlockRelationInfo relationInfo = new BlockRelationInfo();
//        relationInfo.setHash("cccccc");
//        relationInfo.setPreHash("bbbbb");
//        relationInfo.setHeight(1L);
//        List<String> txHashList = new ArrayList<>();
//        txHashList.add("a123");
//        txHashList.add("b123");
//        txHashList.add("c123");
//        relationInfo.setTxHashList(txHashList);
//
//        Document document = DocumentTransferTool.toDocument(relationInfo, "hash");
        // 连接到数据库
        MongoClient mongoClient = new MongoClient("127.0.0.1", 27017);
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoClient, mongoDatabase);

//        service.insertOne(MongoTableName.BLOCK_RELATION, document);
        Document document1 = service.findOne(MongoTableName.BLOCK_HEADER, Filters.eq("preHash", "bbbbb"));
//        BlockRelationInfo relationInfo1 = DocumentTransferTool.toInfo(document1, BlockRelationInfo.class);
//        System.out.println(relationInfo1);
    }

    @Test
    public void testPageQuery() {

    }

}

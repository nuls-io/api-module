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
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;

public class MongoDBServiceTest {

    @Test
    public void testInsert() {

        // 连接到 mongodb 服务
        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoDatabase);
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
            service.insertOne("relations", map);
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
        MongoDBService service = new MongoDBService(mongoDatabase);


        String result = service.createIndex("relations", new Document("height", 1));
        System.out.println(result);

    }

    @Test
    public void testCount() {

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoDatabase);
        MongoCollection<Document> collection = service.getCollection("relations");
        System.out.println(collection.estimatedDocumentCount());
    }

    @Test
    public void testQuery() {

        MongoClient mongoClient = new MongoClient("localhost", 27017);

        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("test");
        MongoDBService service = new MongoDBService(mongoDatabase);

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
        MongoDBService service = new MongoDBService(mongoDatabase);

        service.findOne("relations", eq("height", 1000));
    }

    @Test
    public void testPageQuery() {

    }

}

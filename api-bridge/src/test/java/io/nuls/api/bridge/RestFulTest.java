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

package io.nuls.api.bridge;

import io.nuls.sdk.core.model.Result;
import io.nuls.sdk.core.utils.RestFulUtils;

/**
 * @author Niels
 */
public class RestFulTest {


    public static void main(String[] args) {
        RestFulUtils utils = RestFulUtils.getInstance();
        utils.setServerUri("http://192.168.1.37:8001/api");
        Result result = utils.get("/block/header/height/1",null);
        System.out.println(result);


//        Client client = ClientBuilder.newClient();
//        WebTarget target = client.target("http://192.168.1.127:8001").path("/api/block/header/height/1");
//        Response response = target.request(new String[]{"application/json"}).get();
//        System.out.println(response);
    }


}

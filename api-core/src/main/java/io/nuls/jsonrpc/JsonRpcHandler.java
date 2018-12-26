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

package io.nuls.jsonrpc;

import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.core.util.Log;
import io.nuls.sdk.core.utils.JSONUtils;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
public class JsonRpcHandler extends HttpHandler {
    @Override
    public void service(Request request, Response response) throws Exception {
        if (!request.getMethod().equals(Method.POST)) {
            Log.warn("the request is not POST!");
            return;
        }

        Map<String, Object> jsonRpcParam = JSONUtils.json2map(getParam(request));
        if (!"2.0".equals(jsonRpcParam.get("jsonrpc"))) {
            Log.warn("the request is not a json-rpc 2.0 request!");
            return;
        }
        String method = (String) jsonRpcParam.get("method");
        int id = (int) jsonRpcParam.get("id");
        RpcMethodInvoker invoker = JsonRpcContext.RPC_METHOD_INVOKER_MAP.get(method);

        if (null == invoker) {
            Log.warn("Can't find the method:{}", method);
            return;
        }

        RpcResult result = invoker.invoke((List<Object>) jsonRpcParam.get("params"));
        result.setId(id);
        response.getWriter().write(JSONUtils.obj2json(result));
    }

    private String getParam(Request request) throws IOException {
        int contentLength = request.getContentLength();
        byte buffer[] = new byte[contentLength];
        for (int i = 0; i < contentLength; ) {

            int readlen = request.getInputStream().read(buffer, i,
                    contentLength - i);
            if (readlen == -1) {
                break;
            }
            i += readlen;
        }
        return new String(buffer, "UTF-8");
    }

}

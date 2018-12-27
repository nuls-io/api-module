/*
 *
 *  * MIT License
 *  *
 *  * Copyright (c) 2017-2019 nuls.io
 *  *
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy
 *  * of this software and associated documentation files (the "Software"), to deal
 *  * in the Software without restriction, including without limitation the rights
 *  * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  * copies of the Software, and to permit persons to whom the Software is
 *  * furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all
 *  * copies or substantial portions of the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  * SOFTWARE.
 *  *
 *
 */
package io.nuls.api.bridge;

import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.util.Log;
import io.nuls.api.bean.annotation.Component;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.model.Result;
import io.nuls.sdk.core.model.transaction.Transaction;
import io.nuls.sdk.core.utils.RestFulUtils;
import io.nuls.sdk.tool.NulsSDKTool;

import java.util.HashMap;
import java.util.Map;

/**
 * 钱包RPC调用处理器
 */
@Component
public class WalletRPCHandler {

    private RestFulUtils restFulUtils = RestFulUtils.getInstance();

    /**
     * 根据高度同步区块头
     *
     * @param height 区块高度
     * @return 区块头信息
     */
    public RpcClientResult<BlockHeaderInfo> getBlockHeader(long height) {
        Result result = restFulUtils.get("/block/header/height/" + height, null);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = RpcClientResult.getSuccess();
        try {
            BlockHeaderInfo blockHeader = AnalysisHandler.toBlockHeader((Map<String, Object>) result.getData());
            clientResult.setSuccess(true);
            clientResult.setData(blockHeader);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    /**
     * 根据高度同步区块头
     *
     * @param hash 区块hash
     * @return 区块头信息
     */
    public RpcClientResult<BlockHeaderInfo> getBlockHeader(String hash) {
        Result result = restFulUtils.get("/block/header/hash/" + hash, null);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = RpcClientResult.getSuccess();
        try {
            BlockHeaderInfo blockHeader = AnalysisHandler.toBlockHeader((Map<String, Object>) result.getData());
            clientResult.setData(blockHeader);
            clientResult.setSuccess(true);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    public RpcClientResult<Transaction> getTx(String hash) {
        Map<String, Object> params = new HashMap<>();
        params.put("hash", hash);
        Result result = restFulUtils.get("/tx/bytes", params);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = RpcClientResult.getSuccess();
        try {

        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    /**
     * 根据区块hash获取完整区块
     *
     * @param hash 区块hash
     * @return 区块信息
     */
    public RpcClientResult<BlockInfo> getBlock(String hash) {
        Result result = NulsSDKTool.getBlockWithBytes(hash);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = new RpcClientResult();
        try {
            io.nuls.sdk.core.model.Block nulsBlock = (io.nuls.sdk.core.model.Block) result.getData();
            BlockInfo block = AnalysisHandler.toBlock(nulsBlock);
            clientResult.setSuccess(true);
            clientResult.setData(block);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }
}

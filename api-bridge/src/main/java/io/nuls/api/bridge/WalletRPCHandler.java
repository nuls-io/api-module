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

import io.nuls.api.core.model.*;
import io.nuls.api.core.util.Log;
import io.nuls.api.bean.annotation.Component;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.model.Address;
import io.nuls.sdk.core.model.Block;
import io.nuls.sdk.core.model.Result;
import io.nuls.sdk.core.model.transaction.Transaction;
import io.nuls.sdk.core.utils.AddressTool;
import io.nuls.sdk.core.utils.RestFulUtils;
import io.nuls.sdk.core.utils.VarInt;
import io.nuls.sdk.tool.NulsSDKTool;
import org.spongycastle.util.Arrays;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        RpcClientResult<BlockHeaderInfo> clientResult = RpcClientResult.getSuccess();
        try {
            BlockHeaderInfo blockHeader = AnalysisHandler.toBlockHeader((Map<String, Object>) result.getData());
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
        RpcClientResult<BlockHeaderInfo> clientResult = RpcClientResult.getSuccess();
        try {
            BlockHeaderInfo blockHeader = AnalysisHandler.toBlockHeader((Map<String, Object>) result.getData());
            clientResult.setData(blockHeader);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    public RpcClientResult<TransactionInfo> getTx(String hash) {

        Result result = NulsSDKTool.getTxWithBytes(hash);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult<TransactionInfo> clientResult = RpcClientResult.getSuccess();
        try {
            TransactionInfo transactionInfo = AnalysisHandler.toTransaction((Transaction) result.getData());
            if (transactionInfo.getFroms() != null && !transactionInfo.getFroms().isEmpty()) {
                if (transactionInfo.getFroms().get(0).getAddress() == null) {
                    transactionInfo.setFroms(queryTxInput(hash));
                }
            }
            clientResult.setData(transactionInfo);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    public List<Input> queryTxInput(String hash) {
        Result result = restFulUtils.get("/tx/hash/" + hash, null);
        List<Input> inputList = new ArrayList<>();

        Map<String, Object> map = (HashMap<String, Object>) result.getData();
        List<Map<String, Object>> inputs = (ArrayList<Map<String, Object>>) map.get("inputs");
        for (Map<String, Object> valueMap : inputs) {
            Input input = new Input();
            input.setAddress((String) valueMap.get("address"));
            input.setValue((Long) valueMap.get("value"));

            byte[] fromHashBytes = Hex.decode((String) valueMap.get("fromHash"));
            int fromIndex = (Integer) valueMap.get("fromIndex");
            byte[] key = Arrays.concatenate(fromHashBytes, new VarInt(fromIndex).encode());
            input.setKey(Hex.encode(key));
            inputList.add(input);
        }
        return inputList;
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
        RpcClientResult clientResult = RpcClientResult.getSuccess();
        try {
            Block block = (Block) result.getData();
            BlockInfo blockInfo = AnalysisHandler.toBlock(block);
            clientResult.setData(blockInfo);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    /**
     * 根据区块hash获取完整区块
     *
     * @param height 区块height
     * @return 区块信息
     */
    public RpcClientResult<BlockInfo> getBlock(long height) {
        Result result = NulsSDKTool.getBlockWithBytes(height);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = RpcClientResult.getSuccess();
        try {
            Block block = (Block) result.getData();
            BlockInfo blockInfo = AnalysisHandler.toBlock(block);
            clientResult.setData(blockInfo);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

}

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

package io.nuls.api.controller.block;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.controller.model.RpcErrorCode;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.model.RpcResultError;
import io.nuls.api.controller.utils.VerifyUtils;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.service.BlockHeaderService;
import io.nuls.api.utils.JsonRpcException;
import io.nuls.sdk.core.utils.StringUtils;

import java.util.List;

import static io.nuls.api.controller.model.RpcErrorCode.DATA_NOT_EXISTS;

/**
 * @author Niels
 */
@Controller
public class BlockController {

    @Autowired
    private MongoDBService dbService;

    @Autowired
    private WalletRPCHandler rpcHandler;

    @Autowired
    private BlockHeaderService blockHeaderService;

    @RpcMethod("getBestBlockHeader")
    public RpcResult getBestInfo(List<Object> params) {
        BlockHeaderInfo localBestBlockHeader = blockHeaderService.getBestBlockHeader();
        return new RpcResult().setResult(localBestBlockHeader);
    }

    @RpcMethod("getHeaderByHeight")
    public RpcResult getHeaderByHeight(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        long height = Long.parseLong("" + params.get(0));
        if (height < 0) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[height] should not be less than 0"));
        }
        BlockHeaderInfo header = blockHeaderService.getBlockHeaderInfoByHeight(height);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(header);

        return rpcResult;
    }

    @RpcMethod("getHeaderByHash")
    public RpcResult getHeaderByHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String hash = (String) params.get(0);
        if (StringUtils.isBlank(hash)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[hash] is required"));
        }
        BlockHeaderInfo header = blockHeaderService.getBlockHeaderInfoByHash(hash);

        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(header);
        return rpcResult;
    }

    @RpcMethod("getBlockByHash")
    public RpcResult getBlockByHash(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String hash = (String) params.get(0);
        if (StringUtils.isBlank(hash)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[hash] is required"));
        }
        RpcClientResult<BlockInfo> result = rpcHandler.getBlock(hash);
        BlockInfo block = result.getData();
        if (result.isFailed()) {
            throw new JsonRpcException(new RpcResultError(result.getCode(), result.getMsg(), null));
        }
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(block);
        return rpcResult;
    }

    @RpcMethod("getBlockByHeight")
    public RpcResult getBlockByHeight(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        long height = Long.parseLong("" + params.get(0));
        if (height < 0) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[height] should not be less than 0"));
        }
        BlockHeaderInfo blockHeaderInfo = blockHeaderService.getBlockHeaderInfoByHeight(height);
        if (blockHeaderInfo == null) {
            throw new JsonRpcException(new RpcResultError(DATA_NOT_EXISTS.getCode(), DATA_NOT_EXISTS.getMessage(), null));
        }
        RpcClientResult<BlockInfo> result = rpcHandler.getBlock(blockHeaderInfo.getHash());
        if (result.isFailed()) {
            throw new JsonRpcException(new RpcResultError(result.getCode(), result.getMsg(), null));
        }
        BlockInfo block = result.getData();
        block.setBlockHeader(blockHeaderInfo);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(block);
        return rpcResult;
    }

    @RpcMethod("getBlockList")
    public RpcResult getBlockList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        String packingAddress = null;
        if (params.size() > 2) {
            packingAddress = (String) params.get(2);
        }
//        Whether to filter empty blocks
        boolean filterEmptyBlocks = false;
        if (params.size() > 3) {
            filterEmptyBlocks = (boolean) params.get(3);
        }

        PageInfo<BlockHeaderInfo> pageInfo = blockHeaderService.pageQuery(pageIndex, pageSize, packingAddress, filterEmptyBlocks);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

}

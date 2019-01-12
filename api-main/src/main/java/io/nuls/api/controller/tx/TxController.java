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

package io.nuls.api.controller.tx;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.controller.model.RpcErrorCode;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.model.RpcResultError;
import io.nuls.api.controller.utils.VerifyUtils;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.api.service.TransactionService;
import io.nuls.api.service.TxCountService;
import io.nuls.api.utils.JsonRpcException;
import io.nuls.sdk.core.utils.StringUtils;

import java.util.List;

/**
 * @author Niels
 */
@Controller
public class TxController {

    @Autowired
    private WalletRPCHandler rpcHandler;

    @Autowired
    private TransactionService txService;

    @Autowired
    private TxCountService countService;

    @RpcMethod("getTx")
    public RpcResult getTx(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String hash = "" + params.get(0);
        if (StringUtils.isBlank(hash)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[hash] is required"));
        }
        RpcClientResult<TransactionInfo> rpcClientResult = rpcHandler.getTx(hash);
        if (rpcClientResult.isFailed()) {
            RpcResult result = new RpcResult();
            if (null == rpcClientResult.getData()) {
                result.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
            } else {
                result.setError(new RpcResultError(rpcClientResult.getCode(), rpcClientResult.getMsg(), null));
            }
            return result;
        }
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(rpcClientResult.getData());
        return rpcResult;
    }

    @RpcMethod("getTxList")
    public RpcResult getTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        int type = 0;
        if (params.size() > 2) {
            type = (int) params.get(2);
        }
        boolean includeCoinBase = false;
        if (params.size() > 3) {
            includeCoinBase = (boolean) params.get(3);
        }
        PageInfo<TransactionInfo> pageInfo = txService.getTxList(pageIndex, pageSize, type, includeCoinBase);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(pageInfo);
        return rpcResult;
    }


    @RpcMethod("getBlockTxList")
    public RpcResult getBlockTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);

        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        long height = Long.valueOf(params.get(2).toString());
        int type = Integer.parseInt("" + params.get(3));

        PageInfo<TransactionInfo> pageInfo = txService.getBlockTxList(pageIndex, pageSize, height, type);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(pageInfo);
        return rpcResult;
    }


    @RpcMethod("getTxStatistical")
    public RpcResult getTxStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int type = (int) params.get(0);
        List list = this.countService.getList(type);
        return new RpcResult().setResult(list);
    }


}

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

package io.nuls.api.controller.contract;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.controller.model.RpcErrorCode;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.model.RpcResultError;
import io.nuls.api.controller.utils.VerifyUtils;
import io.nuls.api.core.model.*;
import io.nuls.api.service.ContractService;
import io.nuls.api.service.TokenService;
import io.nuls.api.utils.JsonRpcException;
import io.nuls.sdk.core.utils.AddressTool;

import java.util.List;

/**
 * @author Niels
 */
@Controller
public class ContractController {

    @Autowired
    private TokenService tokenService;
    @Autowired
    private ContractService contractService;

    @RpcMethod("getAddressTokens")
    public RpcResult getAccountTokens(List<Object> params) {
        VerifyUtils.verifyParams(params, 3);
        String address = (String) params.get(0);
        if (!AddressTool.validAddress(address)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[address] is inValid"));
        }
        int pageIndex = (int) params.get(1);
        int pageSize = (int) params.get(2);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<AccountTokenInfo> pageInfo = tokenService.getAccountTokens(address, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

    @RpcMethod("getTokenTransfers")
    public RpcResult getTokenTransfers(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        String address = (String) params.get(0);
        if (!AddressTool.validAddress(address)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[address] is inValid"));
        }
        String contractAddress = (String) params.get(1);

        int pageIndex = (int) params.get(2);
        int pageSize = (int) params.get(3);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<TokenTransfer> pageInfo = tokenService.getTokenTransfers(address, contractAddress, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;

    }

    @RpcMethod("getContract")
    public RpcResult getContract(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String contractAddress = (String) params.get(0);
        if (!AddressTool.validAddress(contractAddress)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
        }
        RpcResult rpcResult = new RpcResult();
        try {
            ContractInfo contractInfo = contractService.getContractInfo(contractAddress);
            if (contractInfo == null) {
                rpcResult.setError(new RpcResultError(RpcErrorCode.DATA_NOT_EXISTS));
            } else {
                rpcResult.setResult(contractInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rpcResult;
    }

    @RpcMethod("getContractTxList")
    public RpcResult getContractTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        String contractAddress = (String) params.get(0);
        if (!AddressTool.validAddress(contractAddress)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[contractAddress] is inValid"));
        }
        int type = (int) params.get(1);
        int pageIndex = (int) params.get(2);
        int pageSize = (int) params.get(3);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<ContractTxInfo> pageInfo = contractService.getContractTxList(contractAddress, type, pageIndex, pageSize);
        RpcResult result = new RpcResult();
        result.setResult(pageInfo);
        return result;
    }

}

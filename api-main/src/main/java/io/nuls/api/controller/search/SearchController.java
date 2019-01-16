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

package io.nuls.api.controller.search;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.controller.ex.NotFoundException;
import io.nuls.api.controller.model.RpcErrorCode;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.model.RpcResultError;
import io.nuls.api.controller.search.dto.SearchResultDTO;
import io.nuls.api.controller.utils.VerifyUtils;
import io.nuls.api.core.model.*;
import io.nuls.api.service.AccountService;
import io.nuls.api.service.BlockHeaderService;
import io.nuls.api.service.TransactionService;
import io.nuls.api.utils.JsonRpcException;
import io.nuls.sdk.core.utils.AddressTool;

import java.util.List;

import static io.nuls.api.controller.model.RpcErrorCode.DATA_NOT_EXISTS;

/**
 * @author Niels
 */
@Controller
public class SearchController {

    @Autowired
    private BlockHeaderService blockHeaderService;

    @Autowired
    private WalletRPCHandler rpcHandler;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountService accountService;

    /**
     * 根据查询字符串自动匹配结果
     *
     * @param params
     * @return
     */
    @RpcMethod("search")
    public RpcResult search(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String text = (String) params.get(0);
        text = text.trim();
        int length = text.length();
        SearchResultDTO result = null;
        if (length < 20) {
            result = getBlockByHeight(text);
        } else if (length < 40) {
            result = getAccountByAddress(text);
        } else {
            result = getResultByHash(text);
        }
        if (null == result) {
            throw new NotFoundException();
        }
        return new RpcResult().setResult(result);
    }

    private SearchResultDTO getResultByHash(String text) {

        BlockHeaderInfo blockHeaderInfo = blockHeaderService.getBlockHeaderInfoByHash(text);
        if (blockHeaderInfo != null) {
            return getBlockInfo(blockHeaderInfo);
        }
        TransactionInfo tx = transactionService.getTx(text);
        if (null == tx) {
            throw new NotFoundException();
        }
        RpcClientResult<TransactionInfo> rpcClientResult = rpcHandler.getTx(text);
        if (rpcClientResult.isSuccess()) {
            tx = rpcClientResult.getData();
        }
        SearchResultDTO dto = new SearchResultDTO();
        dto.setData(tx);
        dto.setType("tx");
        return dto;
    }

    private SearchResultDTO getAccountByAddress(String address) {

        if (!AddressTool.validAddress(address)) {
            throw new JsonRpcException(new RpcResultError(RpcErrorCode.PARAMS_ERROR, "[address] is inValid"));
        }

        AccountInfo accountInfo = accountService.getAccount(address);
        if (accountInfo == null) {
            throw new NotFoundException();
        }
        SearchResultDTO dto = new SearchResultDTO();
        dto.setData(accountInfo);
        dto.setType("account");
        return dto;
    }

    private SearchResultDTO getBlockByHeight(String text) {
        Long height;
        try {
            height = Long.parseLong(text);
        } catch (Exception e) {
            return null;
        }
        BlockHeaderInfo blockHeaderInfo = blockHeaderService.getBlockHeaderInfoByHeight(height);
        if (blockHeaderInfo == null) {
            throw new JsonRpcException(new RpcResultError(DATA_NOT_EXISTS.getCode(), DATA_NOT_EXISTS.getMessage(), null));
        }
        return getBlockInfo(blockHeaderInfo);
    }

    private SearchResultDTO getBlockInfo(BlockHeaderInfo blockHeaderInfo) {
        RpcClientResult<BlockInfo> result = rpcHandler.getBlock(blockHeaderInfo.getHash());
        if (result.isFailed()) {
            throw new JsonRpcException(new RpcResultError(result.getCode(), result.getMsg(), null));
        }
        BlockInfo block = result.getData();
        if (null == block) {
            return null;
        } else {
            SearchResultDTO dto = new SearchResultDTO();
            dto.setData(block);
            dto.setType("block");
            return dto;
        }
    }
}

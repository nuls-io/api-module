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

package io.nuls.api.controller.ledger;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.core.model.AccountInfo;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.service.AccountService;
import io.nuls.api.service.AgentService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Controller
public class LedgerController {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AgentService agentService;

    @RpcMethod("getCoinInfo")
    public RpcResult getCoinInfo(List<Object> params) {
        //todo 尚未实现
        Map<String, Long> map = new HashMap<>();
        map.put("total", accountService.getAllAccountBalance());
        map.put("circulation", agentService.getConsensusCoinTotal());
        map.put("consensusTotal", 3000000000000000L);

        return new RpcResult().setResult(map);
    }

    @RpcMethod("getCoinRanking")
    public RpcResult getCoinRanking(List<Object> params) {
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        int sortType = (int) params.get(2);

        PageInfo<AccountInfo> pageInfo = accountService.getCoinRanking(pageIndex, pageSize, sortType);
        return new RpcResult().setResult(pageInfo);
    }

    @RpcMethod("getAccountTxs")
    public RpcResult getAccountTxs(List<Object> params) {
        //todo
        return null;
    }

    @RpcMethod("getAccountAssets")
    public RpcResult getAccountAssets(List<Object> params) {
        //todo
        return null;
    }

    @RpcMethod("getAccountsTokenTxs")
    public RpcResult getAccountsTokenTxs(List<Object> params) {
        //todo
        return null;
    }
}

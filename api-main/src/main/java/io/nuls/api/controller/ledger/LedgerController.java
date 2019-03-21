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
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.AccountInfo;
import io.nuls.api.core.model.Output;
import io.nuls.api.core.model.PageInfo;
import io.nuls.api.service.AccountService;
import io.nuls.api.service.AgentService;
import io.nuls.api.service.UTXOService;
import io.nuls.sdk.accountledger.utils.LedgerUtil;
import io.nuls.sdk.core.crypto.Hex;
import io.nuls.sdk.core.utils.TimeService;

import java.util.ArrayList;
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
    @Autowired
    private UTXOService utxoService;

    @RpcMethod("getCoinInfo")
    public RpcResult getCoinInfo(List<Object> params) {
        return new RpcResult().setResult(ApiContext.NULS_MAP);
    }

    @RpcMethod("getCoinRanking")
    public RpcResult getCoinRanking(List<Object> params) {
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        int sortType = (int) params.get(2);

        PageInfo<AccountInfo> pageInfo = accountService.getCoinRanking(pageIndex, pageSize, sortType);
        return new RpcResult().setResult(pageInfo);
    }

    @RpcMethod("getUTXO")
    public RpcResult getUTXO(List<Object> params) {
        String address = (String) params.get(0);
        long amount = Long.parseLong(params.get(1).toString());

        //金额再加3个NULS的最高手续费
        amount += 300000000L;

        long value = 0L;
        List<Output> outputs = utxoService.getAccountUtxos(address);
        List<Map<String, Object>> list = new ArrayList<>();

        long currentTime = TimeService.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < outputs.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            Output output = outputs.get(i);

            if (output.getLockTime() < 0) {
                continue;
            }
            if (output.getLockTime() > 0) {
                if (output.getLockTime() >= NulsConstant.BlOCKHEIGHT_TIME_DIVIDE && output.getLockTime() > currentTime) {
                    continue;
                }
                if (output.getLockTime() < NulsConstant.BlOCKHEIGHT_TIME_DIVIDE && output.getLockTime() > ApiContext.bestHeight) {
                    continue;
                }
            }
            count++;
            value += output.getValue();
            map.put("fromHash", output.getTxHash());
            map.put("fromIndex", LedgerUtil.getIndex(Hex.decode(output.getKey())));
            map.put("lockTime", output.getLockTime());
            map.put("value", output.getValue());
            list.add(map);
            if (value >= amount) {
                break;
            } else if (count >= 6000) {
                break;
            }
        }
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getUTXOS")
    public RpcResult getUTXOS(List<Object> params) {
        String address = (String) params.get(0);
        long amount = Long.parseLong(params.get(1).toString());

        //金额再加3个NULS的最高手续费
        amount += 300000000L;
        long value = 0L;
        List<Output> outputs = utxoService.getAccountUtxos(address);
        List<Map<String, Object>> list = new ArrayList<>();
        long currentTime = TimeService.currentTimeMillis();
        int count = 0;
        for (int i = 0; i < outputs.size(); i++) {
            Map<String, Object> map = new HashMap<>();
            Output output = outputs.get(i);

            if (output.getLockTime() < 0) {
                continue;
            }
            if (output.getLockTime() > 0) {
                if (output.getLockTime() >= NulsConstant.BlOCKHEIGHT_TIME_DIVIDE && output.getLockTime() > currentTime) {
                    continue;
                }
                if (output.getLockTime() < NulsConstant.BlOCKHEIGHT_TIME_DIVIDE && output.getLockTime() > ApiContext.bestHeight) {
                    continue;
                }
            }
            count++;
            value += output.getValue();
            map.put("owner", output.getKey());
            map.put("lockTime", output.getLockTime());
            map.put("value", output.getValue());
            list.add(map);
            if (value >= amount) {
                break;
            } else if (count >= 6000) {
                break;
            }
        }
        return new RpcResult().setResult(list);
    }
}

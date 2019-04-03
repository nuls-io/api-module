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
import io.nuls.api.core.model.*;
import io.nuls.api.core.util.Log;
import io.nuls.api.service.*;
import io.nuls.api.utils.JsonRpcException;
import io.nuls.sdk.accountledger.model.Transaction;
import io.nuls.sdk.core.contast.TransactionConstant;
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
    private DepositService depositService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private PunishService punishService;
    @Autowired
    private StatisticalService statisticalService;
    @Autowired
    private ContractService contractService;

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
        TransactionInfo tx = rpcClientResult.getData();
        if (tx.getType() == TransactionConstant.TX_TYPE_JOIN_CONSENSUS) {
            DepositInfo depositInfo = (DepositInfo) tx.getTxData();
            AgentInfo agentInfo = agentService.getAgentByAgentHash(depositInfo.getAgentHash());
            tx.setTxData(agentInfo);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CANCEL_DEPOSIT) {
            DepositInfo depositInfo = (DepositInfo) tx.getTxData();
            depositInfo = depositService.getDepositInfoByHash(depositInfo.getTxHash());
            AgentInfo agentInfo = agentService.getAgentByAgentHash(depositInfo.getAgentHash());
            tx.setTxData(agentInfo);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_STOP_AGENT) {
            AgentInfo agentInfo = (AgentInfo) tx.getTxData();
            agentInfo = agentService.getAgentByAgentHash(agentInfo.getTxHash());
            tx.setTxData(agentInfo);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
            List<TxData> punishLogs = punishService.getYellowPunishLog(tx.getHash());
            tx.setTxDataList(punishLogs);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
            PunishLog punishLog = punishService.getRedPunishLog(tx.getHash());
            tx.setTxData(punishLog);
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CREATE_CONTRACT) {
            try {
                ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());
                ContractInfo contractInfo = (ContractInfo) tx.getTxData();
                contractInfo.setResultInfo(resultInfo);
            } catch (Exception e) {
                Log.error(e);
            }
        } else if (tx.getType() == TransactionConstant.TX_TYPE_CALL_CONTRACT) {
            try {
                ContractResultInfo resultInfo = contractService.getContractResultInfo(tx.getHash());
                ContractCallInfo contractCallInfo = (ContractCallInfo) tx.getTxData();
                contractCallInfo.setResultInfo(resultInfo);
            } catch (Exception e) {
                Log.error(e);
            }
        }
        rpcResult.setResult(rpcClientResult.getData());
        return rpcResult;
    }

    @RpcMethod("getTxList")
    public RpcResult getTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        int type = (int) params.get(2);
        boolean isHidden = (boolean) params.get(3);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        PageInfo<TransactionInfo> pageInfo = txService.getTxList(pageIndex, pageSize, type, isHidden);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(pageInfo);
        return rpcResult;
    }

    @RpcMethod("getBlockTxList")
    public RpcResult getBlockTxList(List<Object> params) {
        VerifyUtils.verifyParams(params, 4);

        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        long height = Long.valueOf(params.get(2).toString());
        int type = Integer.parseInt("" + params.get(3));

        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }

        PageInfo<TransactionInfo> pageInfo = txService.getBlockTxList(pageIndex, pageSize, height, type);
        RpcResult rpcResult = new RpcResult();
        rpcResult.setResult(pageInfo);
        return rpcResult;
    }


    @RpcMethod("getTxStatistical")
    public RpcResult getTxStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int type = (int) params.get(0);
        List list = this.statisticalService.getStatisticalList(type, "txCount");
        return new RpcResult().setResult(list);
    }
}

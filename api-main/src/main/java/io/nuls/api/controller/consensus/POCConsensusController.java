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

package io.nuls.api.controller.consensus;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Controller;
import io.nuls.api.bean.annotation.RpcMethod;
import io.nuls.api.controller.model.RpcResult;
import io.nuls.api.controller.utils.VerifyUtils;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.model.*;
import io.nuls.api.service.*;
import io.nuls.api.utils.RoundManager;
import io.nuls.sdk.core.utils.DoubleUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Niels
 */
@Controller
public class POCConsensusController {


    @Autowired
    private RoundManager roundManager;

    @Autowired
    private AgentService agentService;

    @Autowired
    private PunishService punishService;

    @Autowired
    private DepositService depositService;

    @Autowired
    private RoundService roundService;

    @Autowired
    private StatisticalService statisticalService;

    @RpcMethod("getBestRoundItemList")
    public RpcResult getBestRoundItemList(List<Object> params) {
        List<PocRoundItem> itemList = roundManager.getCurrentRound().getItemList();
        RpcResult rpcResult = new RpcResult();
        itemList.addAll(itemList);
        rpcResult.setResult(itemList);
        return rpcResult;
    }

    @RpcMethod("getConsensusNodeCount")
    public RpcResult getConsensusNodeCount(List<Object> params) {
        String[] seeds = ApiContext.config.getProperty("wallet.consensus.seeds").split(",");
        Map<String, Long> resultMap = new HashMap<>();
        resultMap.put("seedsCount", (long) seeds.length);
        resultMap.put("consensusCount", (long) (roundManager.getCurrentRound().getMemberCount() - seeds.length));
        long count = agentService.agentsCount(ApiContext.bestHeight);
        resultMap.put("totalCount", count + seeds.length);
        RpcResult result = new RpcResult();
        result.setResult(resultMap);
        return result;
    }

    @RpcMethod("getConsensusStatistical")
    public RpcResult getConsensusStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int type = (int) params.get(0);
        List list = this.statisticalService.getStatisticalList(type, "consensusLocked");
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getConsensusNodes")
    public RpcResult getConsensusNodes(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        //todo 条件过滤，状态修改，字段填充
        List<AgentInfo> list = agentService.getAgentList(ApiContext.bestHeight);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getConsensusNode")
    public RpcResult getConsensusNode(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String agentHash = (String) params.get(0);

        AgentInfo agentInfo = agentService.getAgentByAgentHash(agentHash);
        long count = punishService.getYellowCount(agentInfo.getAgentAddress());
        agentInfo.setLostRate(DoubleUtils.div(count, count + agentInfo.getTotalPackingCount()));

        List<PocRoundItem> itemList = roundManager.getCurrentRound().getItemList();

        PocRoundItem roundItem = null;
        if (null != itemList) {
            for (PocRoundItem item : itemList) {
                if (item.getPackingAddress().equals(agentInfo.getPackingAddress())) {
                    roundItem = item;
                    break;
                }
            }
        }
        if (null == roundItem) {
            agentInfo.setStatus(0);
        } else {
            agentInfo.setRoundPackingTime(roundManager.getCurrentRound().getStartTime() + roundItem.getOrder() * 10000);
            agentInfo.setStatus(1);
        }

        List<DepositInfo> depositInfoList = depositService.getDepositListByAgentHash(agentHash);
        long totalDeposit = 0;
        for (DepositInfo dep : depositInfoList) {
            totalDeposit += dep.getAmount();
        }
        agentInfo.setDepositCount(depositInfoList.size());
        agentInfo.setTotalDeposit(totalDeposit);


        return new RpcResult().setResult(agentInfo);
    }

    @RpcMethod("getConsensusNodeStatistical")
    public RpcResult getConsensusNodeStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int type = (int) params.get(0);
        List list = this.statisticalService.getStatisticalList(type, "nodeCount");
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getAnnulizedRewardStatistical")
    public RpcResult getAnnulizedRewardStatistical(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        int type = (int) params.get(0);
        List list = this.statisticalService.getStatisticalList(type, "annualizedReward");
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getPunishList")
    public RpcResult getPunishList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int type = (int) params.get(0);
        String agentAddress = (String) params.get(1);
        List<PunishLog> list = punishService.getPunishLogList(type, agentAddress, roundManager.getCurrentRound().getIndex());
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getConsensusDeposit")
    public RpcResult getConsensusDeposit(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String agentHash = (String) params.get(0);
        List<DepositInfo> list = this.depositService.getDepositListByAgentHash(agentHash);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getBestRoundInfo")
    public RpcResult getBestRoundInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 0);
        return new RpcResult().setResult(roundManager.getCurrentRound());
    }

    @RpcMethod("getConsensusCancelDeposit")
    public RpcResult getConsensusCancelDeposit(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        String agentHash = (String) params.get(0);
        List<DepositInfo> list = this.depositService.getCancelDepositListByAgentHash(agentHash);
        return new RpcResult().setResult(list);
    }

    @RpcMethod("getRoundList")
    public RpcResult getRoundList(List<Object> params) {
        VerifyUtils.verifyParams(params, 2);
        int pageIndex = (int) params.get(0);
        int pageSize = (int) params.get(1);
        if (pageIndex <= 0) {
            pageIndex = 1;
        }
        if (pageSize <= 0 || pageSize > 100) {
            pageSize = 10;
        }
        long count = roundService.getTotalCount();
        List<PocRound> roundList = roundService.getRoundList(pageIndex, pageSize);
        PageInfo<PocRound> pageInfo = new PageInfo<>();
        pageInfo.setPageNumber(pageIndex);
        pageInfo.setPageSize(pageSize);
        pageInfo.setTotalCount(count);
        pageInfo.setList(roundList);
        return new RpcResult().setResult(pageInfo);
    }

    @RpcMethod("getRoundInfo")
    public RpcResult getRoundInfo(List<Object> params) {
        VerifyUtils.verifyParams(params, 1);
        long roundIndex = Long.parseLong(params.get(0) + "");
        CurrentRound round = new CurrentRound();
        PocRound pocRound = roundService.getRound(roundIndex);
        List<PocRoundItem> itemList = roundService.getRoundItemList(roundIndex);
        round.setItemList(itemList);
        round.initByPocRound(pocRound);
        return new RpcResult().setResult(round);
    }

}

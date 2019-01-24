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

package io.nuls.api.task;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.model.AgentInfo;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.DepositInfo;
import io.nuls.api.core.model.StatisticalInfo;
import io.nuls.api.core.util.Log;
import io.nuls.api.service.AgentService;
import io.nuls.api.service.BlockHeaderService;
import io.nuls.api.service.DepositService;
import io.nuls.api.service.StatisticalService;
import io.nuls.sdk.core.utils.DoubleUtils;

import java.util.Calendar;
import java.util.List;

/**
 * @author Niels
 */
@Component
public class StatisticalTask implements Runnable {

    @Autowired
    private StatisticalService statisticalService;

    @Autowired
    private BlockHeaderService blockHeaderService;

    @Autowired
    private DepositService depositService;

    @Autowired
    private AgentService agentService;


    @Override
    public void run() {
        try {
            this.doCalc();
        } catch (Exception e) {
            Log.error(e);
        }
    }


    private void doCalc() {
        long bestId = statisticalService.getBestId();
        BlockHeaderInfo header = blockHeaderService.getBestBlockHeader();
        if (null == header) {
            return;
        }
        long day = 24 * 3600000;
        long start = bestId + 1;
        long end = 0;
        if (bestId == 0) {
            BlockHeaderInfo header0 = blockHeaderService.getBlockHeaderInfoByHeight(0);
            start = header0.getCreateTime();
            end = start + day;
            this.statisticalService.saveBestId(start);
        } else {
            end = start + day - 1;
        }
        while (true) {
            if (end > header.getCreateTime()) {
                break;
            }
            statistical(start, end);
            start = end + 1;
            end = end + day;
            BlockHeaderInfo newBlockHeader = blockHeaderService.getBestBlockHeader();
            if (null != newBlockHeader) {
                header = newBlockHeader;
            }
        }
    }

    private void statistical(long start, long end) {
        long txCount = statisticalService.calcTxCount(start, end);
        long consensusLocked = 0;
        long height = blockHeaderService.getMaxHeight(end);
        List<AgentInfo> agentList = agentService.getAgentList(height);
        List<DepositInfo> depositList = depositService.getDepositList(height);
        int nodeCount = agentList.size();
        for (AgentInfo agent : agentList) {
            consensusLocked += agent.getDeposit();
        }
        for (DepositInfo deposit : depositList) {
            consensusLocked += deposit.getAmount();
        }
        double annualizedReward = 0L;
        if (consensusLocked != 0) {
            annualizedReward = DoubleUtils.mul(100, DoubleUtils.div(500000000000000L, consensusLocked, 4), 2);
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(end);
        StatisticalInfo info = new StatisticalInfo();
        info.setTime(end);
        info.setTxCount(txCount);
        info.setAnnualizedReward(annualizedReward);
        info.setNodeCount(nodeCount);
        info.setConsensusLocked(consensusLocked);
        info.setDate(calendar.get(Calendar.DATE));
        info.setMonth(calendar.get(Calendar.MONTH) + 1);
        info.setYear(calendar.get(Calendar.YEAR));
        try {
            this.statisticalService.insert(info);
        } catch (Exception e) {
            Log.error(e);
        }
        this.statisticalService.updateBestId(info.getTime());
    }

}

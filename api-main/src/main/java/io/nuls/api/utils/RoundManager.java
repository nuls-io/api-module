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

package io.nuls.api.utils;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.model.*;
import io.nuls.api.core.util.Log;
import io.nuls.api.service.AgentService;
import io.nuls.api.service.DepositService;
import io.nuls.api.service.RoundService;
import io.nuls.sdk.core.contast.TransactionConstant;
import io.nuls.sdk.core.crypto.Sha256Hash;
import io.nuls.sdk.core.utils.AddressTool;
import io.nuls.sdk.core.utils.ArraysTool;
import io.nuls.sdk.core.utils.DoubleUtils;
import io.nuls.sdk.core.utils.SerializeUtils;

import java.util.*;

/**
 * @author Niels
 */
@Component
public class RoundManager {

    private static final long MIN_DEPOSIT = 20000000000000L;

    private CurrentRound currentRound = new CurrentRound();

    @Autowired
    private AgentService agentService;

    @Autowired
    private DepositService depositService;

    @Autowired
    private RoundService roundService;

    public void process(BlockInfo blockInfo) {
        if (blockInfo.getBlockHeader().getRoundIndex() == currentRound.getIndex()) {
            processCurrentRound(blockInfo);
        } else {
            processNextRound(blockInfo);
        }
    }

    private void processNextRound(BlockInfo blockInfo) {
        long startHeight = currentRound.getStartHeight();
        if (null != currentRound.getStartBlockHeader() && currentRound.getStartBlockHeader().getPackingIndexOfRound() > 1) {
            startHeight = startHeight - 1;
        }
        List<AgentInfo> agentList = this.agentService.getAgentList(startHeight);
        List<DepositInfo> depositList = this.depositService.getDepositList(startHeight);
        Map<String, AgentInfo> map = new HashMap<>();
        for (AgentInfo agent : agentList) {
            agent.setTotalDeposit(agent.getDeposit());
            map.put(agent.getTxHash(), agent);
        }
        for (DepositInfo deposit : depositList) {
            AgentInfo agent = map.get(deposit.getAgentHash());
            if (null == agent) {
                Log.warn("Wrong deposit！");
                continue;
            }
            agent.setTotalDeposit(agent.getTotalDeposit() + deposit.getAmount());
        }
        List<AgentSorter> sorterList = new ArrayList<>();
        for (AgentInfo agent : map.values()) {
            if (agent.getTotalDeposit() >= MIN_DEPOSIT) {
                AgentSorter sorter = new AgentSorter();
                sorter.setAgentId(agent.getTxHash());
                byte[] hash = ArraysTool.concatenate(AddressTool.getAddress(agent.getPackingAddress()), SerializeUtils.uint64ToByteArray(blockInfo.getBlockHeader().getRoundStartTime()));

                sorter.setSorter(Sha256Hash.twiceOf(hash).toString());

                sorterList.add(sorter);
            }
        }

        String seeds = ApiContext.config.getProperty("wallet.consensus.seeds");
        String[] seedAddresses = seeds.split(",");
        for (String address : seedAddresses) {
            AgentSorter sorter = new AgentSorter();
            sorter.setSeedAddress(address);
            byte[] hash = ArraysTool.concatenate(AddressTool.getAddress(address), SerializeUtils.uint64ToByteArray(blockInfo.getBlockHeader().getRoundStartTime()));
            sorter.setSorter(Sha256Hash.twiceOf(hash).toString());
            sorterList.add(sorter);
        }
        Collections.sort(sorterList);


        BlockHeaderInfo header = blockInfo.getBlockHeader();
        //生成新的round
        CurrentRound round = new CurrentRound();
        round.setIndex(header.getRoundIndex());
        round.setStartHeight(header.getHeight());
        round.setStartBlockHeader(header);
        round.setStartTime(header.getRoundStartTime());
        round.setMemberCount(sorterList.size());
        round.setEndTime(round.getStartTime() + 10000 * sorterList.size());
        round.setProducedBlockCount(1);


        List<PocRoundItem> itemList = new ArrayList<>();
        int index = 1;
        for (AgentSorter sorter : sorterList) {
            PocRoundItem item = new PocRoundItem();
            item.setRoundIndex(header.getRoundIndex());
            item.setOrder(index++);
            item.setId(item.getRoundIndex() + "_" + item.getOrder());
            if (null == sorter.getSeedAddress()) {
                AgentInfo agentInfo = map.get(sorter.getAgentId());
                item.setAgentName(agentInfo.getAlias() == null ?
                        agentInfo.getTxHash().substring(agentInfo.getTxHash().length() - 8) : agentInfo.getAlias());
                item.setPackingAddress(agentInfo.getPackingAddress());
            } else {
                item.setSeedAddress(sorter.getSeedAddress());

            }
            itemList.add(item);
        }
        round.setItemList(itemList);
        round.setMemberCount(itemList.size());
        round.setPackerOrder(header.getPackingIndexOfRound() + 1);

        round.setRedCardCount(0);
        round.setYellowCardCount(0);
        round.setLostRate(DoubleUtils.div(header.getPackingIndexOfRound() - round.getProducedBlockCount(), round.getMemberCount()));

        fillPunishCount(blockInfo.getTxs(), round);
        this.currentRound = round;
//        Log.warn("++++++++{}({})+++++++" + round.toString(), blockInfo.getBlockHeader().getHeight(), startHeight);
        roundService.saveRound(round.toPocRound());
        roundService.saveRoundItemList(round.getItemList());


    }

    private void fillPunishCount(List<TransactionInfo> txs, CurrentRound round) {
        int redCount = 0;
        int yellowCount = 0;
        for (TransactionInfo tx : txs) {
            if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
                yellowCount += tx.getTxDataList() != null ? tx.getTxDataList().size() : 0;
            } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                redCount++;
            }
        }
        round.setYellowCardCount(round.getYellowCardCount() + yellowCount);
        round.setRedCardCount(round.getRedCardCount() + redCount);
    }

    private void processCurrentRound(BlockInfo blockInfo) {
        int indexOfRound = blockInfo.getBlockHeader().getPackingIndexOfRound();
        //下一个出块者
        currentRound.setPackerOrder(indexOfRound < currentRound.getMemberCount() ? indexOfRound + 1 : indexOfRound);
//        System.out.println("+++++++++" + blockInfo.getBlockHeader().getHeight());
        PocRoundItem item = currentRound.getItemList().get(indexOfRound - 1);
        item.setBlockHeight(blockInfo.getBlockHeader().getHeight());
        item.setReward(blockInfo.getBlockHeader().getReward());
        item.setTxCount(blockInfo.getBlockHeader().getTxCount());

        roundService.updateRoundItem(item);
        this.currentRound.setProducedBlockCount(this.currentRound.getProducedBlockCount() + 1);
        this.currentRound.setEndHeight(blockInfo.getBlockHeader().getHeight());
        currentRound.setLostRate(DoubleUtils.div(blockInfo.getBlockHeader().getPackingIndexOfRound() - currentRound.getProducedBlockCount(), currentRound.getMemberCount()));
        this.fillPunishCount(blockInfo.getTxs(), currentRound);

        this.roundService.updateRound(this.currentRound.toPocRound());

    }

    private void rollbackCurrentRound(BlockInfo blockInfo) {
        //todo


    }

    private void rollbackPreRound(BlockInfo blockInfo) {
        //todo
    }

    public void rollback(BlockInfo blockInfo) {
        if (blockInfo.getBlockHeader().getHeight() == currentRound.getStartHeight()) {
            rollbackPreRound(blockInfo);
        } else {
            rollbackCurrentRound(blockInfo);
        }
    }


    public CurrentRound getCurrentRound() {
        return currentRound;
    }
}

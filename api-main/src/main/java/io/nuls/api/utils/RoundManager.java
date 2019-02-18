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
import io.nuls.api.service.BlockHeaderService;
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

    @Autowired
    private BlockHeaderService blockHeaderService;

    public void process(BlockInfo blockInfo) {
        try {
            if (null == this.currentRound.getItemList()) {
                PocRound round = null;
                long roundIndex = blockInfo.getBlockHeader().getRoundIndex();
                while (round == null && blockInfo.getBlockHeader().getHeight() > 0) {
                    round = roundService.getRound(roundIndex--);
                }
                if (round != null) {
                    CurrentRound preRound = new CurrentRound();
                    preRound.initByPocRound(round);
                    List<PocRoundItem> list = roundService.getRoundItemList(round.getIndex());
                    preRound.setItemList(list);
                    preRound.setStartBlockHeader(blockHeaderService.getBlockHeaderInfoByHeight(round.getStartHeight()));
                    preRound.setPackerOrder(round.getMemberCount());
                    this.currentRound = preRound;
                }

            }
            if (blockInfo.getBlockHeader().getRoundIndex() == currentRound.getIndex()) {
                processCurrentRound(blockInfo);
            } else {
                processNextRound(blockInfo);
            }
        } catch (Exception e) {
            Log.error(e);
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

        for (String address : ApiContext.SEED_NODE_ADDRESS) {
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
            if (item.getOrder() == header.getPackingIndexOfRound()) {
                item.setTime(header.getCreateTime());
                item.setBlockHeight(header.getHeight());
                item.setBlockHash(header.getHash());
                item.setTxCount(header.getTxCount());
                item.setReward(header.getReward());
            }
            item.setId(item.getRoundIndex() + "_" + item.getOrder());
            if (null == sorter.getSeedAddress()) {
                AgentInfo agentInfo = map.get(sorter.getAgentId());
                item.setAgentName(agentInfo.getAgentAlias() == null ?
                        agentInfo.getTxHash().substring(agentInfo.getTxHash().length() - 8) : agentInfo.getAgentAlias());
                item.setAgentHash(agentInfo.getTxHash());
                item.setPackingAddress(agentInfo.getPackingAddress());
            } else {
                item.setSeedAddress(sorter.getSeedAddress());
                item.setPackingAddress(sorter.getSeedAddress());

            }
            item.setTime(round.getStartTime() + item.getOrder() * 10000L);
            itemList.add(item);
        }
        round.setItemList(itemList);
        round.setMemberCount(itemList.size());
        round.setPackerOrder(header.getPackingIndexOfRound() + 1);

        round.setRedCardCount(0);
        round.setYellowCardCount(0);
        round.setLostRate(DoubleUtils.div(header.getPackingIndexOfRound() - round.getProducedBlockCount(), round.getMemberCount()));

        fillPunishCount(blockInfo.getTxs(), round, true);
        this.currentRound = round;
//        Log.warn("++++++++{}({})+++++++" + round.toString(), blockInfo.getBlockHeader().getHeight(), startHeight);
        roundService.saveRoundItemList(round.getItemList());
        roundService.saveRound(round.toPocRound());


    }

    private void fillPunishCount(List<TransactionInfo> txs, CurrentRound round, boolean add) {
        int redCount = 0;
        int yellowCount = 0;
        for (TransactionInfo tx : txs) {
            if (tx.getType() == TransactionConstant.TX_TYPE_YELLOW_PUNISH) {
                yellowCount += tx.getTxDataList() != null ? tx.getTxDataList().size() : 0;
            } else if (tx.getType() == TransactionConstant.TX_TYPE_RED_PUNISH) {
                redCount++;
            }
        }
        if (add) {
            round.setYellowCardCount(round.getYellowCardCount() + yellowCount);
            round.setRedCardCount(round.getRedCardCount() + redCount);
        } else {
            round.setYellowCardCount(round.getYellowCardCount() - yellowCount);
            round.setRedCardCount(round.getRedCardCount() - redCount);
        }
    }


    private void processCurrentRound(BlockInfo blockInfo) {
        int indexOfRound = blockInfo.getBlockHeader().getPackingIndexOfRound();
        //下一个出块者
        currentRound.setPackerOrder(indexOfRound < currentRound.getMemberCount() ? indexOfRound + 1 : indexOfRound);
//        System.out.println("+++++++++" + blockInfo.getBlockHeader().getHeight());
        PocRoundItem item = currentRound.getItemList().get(indexOfRound - 1);
        BlockHeaderInfo header = blockInfo.getBlockHeader();
        item.setTime(header.getCreateTime());
        item.setBlockHeight(header.getHeight());
        item.setBlockHash(header.getHash());
        item.setTxCount(header.getTxCount());
        item.setReward(header.getReward());

        roundService.updateRoundItem(item);
        this.currentRound.setProducedBlockCount(this.currentRound.getProducedBlockCount() + 1);
        this.currentRound.setEndHeight(blockInfo.getBlockHeader().getHeight());
        currentRound.setLostRate(DoubleUtils.div(blockInfo.getBlockHeader().getPackingIndexOfRound() - currentRound.getProducedBlockCount(), currentRound.getMemberCount()));
        this.fillPunishCount(blockInfo.getTxs(), currentRound, true);

        this.roundService.updateRound(this.currentRound.toPocRound());

    }

    private void rollbackCurrentRound(BlockInfo blockInfo) {
        int indexOfRound = blockInfo.getBlockHeader().getPackingIndexOfRound() - 1;
        if (currentRound.getItemList() == null) {
            PocRound round = roundService.getRound(blockInfo.getBlockHeader().getRoundIndex());
            CurrentRound preRound = new CurrentRound();
            preRound.initByPocRound(round);
            List<PocRoundItem> list = roundService.getRoundItemList(round.getIndex());
            preRound.setItemList(list);
            preRound.setStartBlockHeader(blockHeaderService.getBlockHeaderInfoByHeight(round.getStartHeight()));
            preRound.setPackerOrder(round.getMemberCount());
            this.currentRound = preRound;
        }
        PocRoundItem item = currentRound.getItemList().get(indexOfRound);
        item.setBlockHeight(0);
        item.setReward(0);
        item.setTxCount(0);

        roundService.updateRoundItem(item);
        this.currentRound.setProducedBlockCount(this.currentRound.getProducedBlockCount() - 1);
        this.currentRound.setEndHeight(blockInfo.getBlockHeader().getHeight() - 1);
        currentRound.setLostRate(DoubleUtils.div(blockInfo.getBlockHeader().getPackingIndexOfRound() - currentRound.getProducedBlockCount(), currentRound.getMemberCount()));
        this.fillPunishCount(blockInfo.getTxs(), currentRound, false);

        this.roundService.updateRound(this.currentRound.toPocRound());


    }

    private void rollbackPreRound(BlockInfo blockInfo) {
        this.roundService.removeRound(currentRound.getIndex());
        PocRound round = null;
        long roundIndex = currentRound.getIndex() - 1;
        while (round == null) {
            round = roundService.getRound(roundIndex--);
        }
        CurrentRound preRound = new CurrentRound();
        preRound.initByPocRound(round);
        List<PocRoundItem> list = roundService.getRoundItemList(round.getIndex());
        preRound.setItemList(list);
        preRound.setStartBlockHeader(blockHeaderService.getBlockHeaderInfoByHeight(round.getStartHeight()));
        preRound.setPackerOrder(round.getMemberCount());
        this.currentRound = preRound;
    }

    public void rollback(BlockInfo blockInfo) {
        if (null == this.currentRound.getItemList()) {
            PocRound round = null;
            long roundIndex = blockInfo.getBlockHeader().getRoundIndex();
            while (round == null && blockInfo.getBlockHeader().getHeight() > 0) {
                round = roundService.getRound(roundIndex--);
            }
            if (round != null) {
                CurrentRound preRound = new CurrentRound();
                preRound.initByPocRound(round);
                List<PocRoundItem> list = roundService.getRoundItemList(round.getIndex());
                preRound.setItemList(list);
                preRound.setStartBlockHeader(blockHeaderService.getBlockHeaderInfoByHeight(round.getStartHeight()));
                preRound.setPackerOrder(round.getMemberCount());
                this.currentRound = preRound;
            }
        }
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

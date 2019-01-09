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

package io.nuls.api.core.model;

import java.util.List;

/**
 * @author Niels
 */
public class CurrentRound extends PocRound {

    public CurrentRound() {
        this.setIndex(-1);
    }

    private int packerOrder;

    private List<PocRoundItem> itemList;

    public int getPackerOrder() {
        return packerOrder;
    }

    public void setPackerOrder(int packerOrder) {
        this.packerOrder = packerOrder;
    }

    public List<PocRoundItem> getItemList() {
        return itemList;
    }

    public void setItemList(List<PocRoundItem> itemList) {
        this.itemList = itemList;
    }

    public PocRound toPocRound() {
        PocRound round = new PocRound();
        round.setEndHeight(this.getEndHeight());
        round.setEndTime(this.getEndTime());
        round.setIndex(this.getIndex());
        round.setMemberCount(this.getMemberCount());
        round.setProducedBlockCount(this.getProducedBlockCount());
        round.setRedCardCount(this.getRedCardCount());
        round.setStartHeight(this.getStartHeight());
        round.setStartTime(this.getStartTime());
        round.setYellowCardCount(this.getYellowCardCount());
        return round;
    }
}

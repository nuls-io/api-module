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
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.TxCountInfo;
import io.nuls.api.core.util.Log;
import io.nuls.api.service.BlockHeaderService;
import io.nuls.api.service.TxCountService;

import java.util.Calendar;

/**
 * @author Niels
 */
@Component
public class TxCountTask implements Runnable {

    @Autowired
    private TxCountService txCountService;

    @Autowired
    private BlockHeaderService blockHeaderService;

    @Override
    public void run() {
        try {
            this.calcTxCount();
        } catch (Exception e) {
            Log.error(e);
        }
    }


    private void calcTxCount() {
        long bestId = txCountService.getBestId();
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
        } else {
            end = start + day - 1;
        }
        while (true) {
            if (end > header.getCreateTime()) {
                break;
            }
            doCalc(start, end);
            start = end + 1;
            end = end + day;
            header = blockHeaderService.getBestBlockHeader();
        }
    }

    private void doCalc(long start, long end) {
        long count = txCountService.calcTxCount(start, end);
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(end);
        TxCountInfo info = new TxCountInfo();
        info.setTime(end);
        info.setCount(count);
        info.setDate(calendar.get(Calendar.DATE));
        info.setMonth(calendar.get(Calendar.MONTH));
        info.setYear(calendar.get(Calendar.YEAR));
        this.txCountService.insert(info);
    }

}

package io.nuls.api.utils;

import io.nuls.api.core.constant.NulsConstant;
import io.nuls.api.core.model.AccountInfo;
import io.nuls.api.core.model.Output;
import io.nuls.sdk.core.utils.TimeService;

import java.util.List;

public class CalcUtil {

    public static void calcBalance(AccountInfo accountInfo, List<Output> outputs, long bestHeight) {
        if (outputs == null || outputs.isEmpty()) {
            return;
        }

        long timeLockUtxo = 0;
        long consensusUtxo = 0;
        long balance = 0;
        long currentTime = TimeService.currentTimeMillis();
        for (Output output : outputs) {
            if (output.getLockTime() == null || output.getLockTime() == 0) {
                balance += output.getValue();
            } else if (output.getLockTime() == -1L) {
                consensusUtxo += output.getValue();
            } else if (output.getLockTime() > 0) {
                if (output.getLockTime() >= NulsConstant.BlOCKHEIGHT_TIME_DIVIDE) {
                    if (output.getLockTime() <= currentTime) {
                        balance += output.getValue();
                    } else {
                        timeLockUtxo += output.getValue();
                    }
                } else {
                    if (output.getLockTime() <= bestHeight) {
                        balance += output.getValue();
                    } else {
                        timeLockUtxo += output.getValue();
                    }
                }
            }
        }
        accountInfo.setBalance(balance);
        accountInfo.setTimeLock(timeLockUtxo);
        accountInfo.setConsensusLock(consensusUtxo);
        accountInfo.setTotalBalance(balance + timeLockUtxo + consensusUtxo);
    }
}

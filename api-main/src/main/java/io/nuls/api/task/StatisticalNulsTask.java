package io.nuls.api.task;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.service.AccountService;
import io.nuls.api.service.AgentService;

@Component
public class StatisticalNulsTask implements Runnable {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AgentService agentService;

    @Override
    public void run() {
        //统计所有nuls总数量
        long totalNuls = accountService.getAllAccountBalance();
        //统计所有参与共识的nuls数量
        long consensusTotal = agentService.getConsensusCoinTotal();
    }
}

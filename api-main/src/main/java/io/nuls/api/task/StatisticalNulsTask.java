package io.nuls.api.task;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.core.ApiContext;
import io.nuls.api.service.AccountService;
import io.nuls.api.service.AgentService;
import io.nuls.api.service.UTXOService;

@Component
public class StatisticalNulsTask implements Runnable {

    @Autowired
    private AccountService accountService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private UTXOService utxoService;

    @Override
    public void run() {
        //统计所有nuls总数量
        long totalNuls = accountService.getAllAccountBalance();
        //统计所有参与共识的nuls数量
        long consensusTotal = agentService.getConsensusCoinTotal();
        //团队持有数量
        long teamNuls = accountService.getAccountTotalBalance(ApiContext.TEAM_ADDRESS);
        //销毁数量
        long destroyNuls = accountService.getAccountTotalBalance(ApiContext.DESTROY_ADDRESS);
        //商务持有数量
        long businessNuls = accountService.getAccountTotalBalance(ApiContext.BUSINESS_ADDRESS);
        //社区持有数量
        long communityNuls = accountService.getAccountTotalBalance(ApiContext.COMMUNITY_ADDRESS);
        //映射地址数量
        long mappingNuls = 0;
        for (String address : ApiContext.MAPPING_ADDRESS) {
            mappingNuls += accountService.getAccountTotalBalance(address);
        }
        //大使地址持有数量
        long ambassadorNuls = 0;
        for (String address : ApiContext.AMBASSADOR_NODE_ADDRESS) {
            ambassadorNuls += accountService.getAccountTotalBalance(address);
        }
        //流通量
        long circulationNuls = totalNuls - teamNuls - destroyNuls - communityNuls - businessNuls - mappingNuls - ambassadorNuls;
        ApiContext.NULS_MAP.put("total", totalNuls);
        ApiContext.NULS_MAP.put("consensusTotal", consensusTotal);
        ApiContext.NULS_MAP.put("circulation", circulationNuls);
    }
}

package io.nuls;

import io.nuls.api.bean.SpringLiteContext;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.sdk.core.SDKBootstrap;
import org.junit.Test;

public class TEST {

    @Test
    public void test() {
        SpringLiteContext.init("io.nuls.api.bridge");
        SDKBootstrap.init("192.168.1.127", "8001", 261);

        WalletRPCHandler rpcHandler = SpringLiteContext.getBean(WalletRPCHandler.class);
        RpcClientResult<BlockInfo> clientResult = rpcHandler.getBlock(936293L);
        BlockInfo blockInfo = clientResult.getData();

        System.out.println(1);
    }
}

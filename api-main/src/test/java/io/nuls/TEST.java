package io.nuls;

import io.nuls.api.bean.SpringLiteContext;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.sdk.core.SDKBootstrap;
import org.junit.Test;

public class TEST {

    @Test
    public void test() {
        SpringLiteContext.init("io.nuls.api.bridge");
        SDKBootstrap.init("127.0.0.1", "6001", 8964);

        WalletRPCHandler rpcHandler = SpringLiteContext.getBean(WalletRPCHandler.class);
        String hash = "0020e7e7e48d4091e04bd9cb6bbbf903fb71bf3189d550ac2d0f9cde8360aa5069f2";

        RpcClientResult<TransactionInfo> clientResult = rpcHandler.getTx(hash);
        TransactionInfo txInfo = clientResult.getData();
        txInfo.calcValue();
        System.out.println(txInfo);
    }
}

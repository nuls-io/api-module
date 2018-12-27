package io.nuls;

import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.sdk.core.SDKBootstrap;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    WalletRPCHandler walletRPCHandler;

    @Before
    public void init() {
        SDKBootstrap.init("127.0.0.1", "6001");
        walletRPCHandler = new WalletRPCHandler();
    }

    @Test
    public void testGetBlockHeader() {
        RpcClientResult<BlockHeaderInfo> clientResult = walletRPCHandler.getBlockHeader(0);
        System.out.println(clientResult.isSuccess());
    }

    @Test
    public void testGetBlock() {
        RpcClientResult<BlockInfo> clientResult = walletRPCHandler.getBlock("0020302f3246db55094dd01cd7b12f0ec6120fcfd51d7a8b8f518b99348455333f9a");
        System.out.println(clientResult.isSuccess());
    }
}

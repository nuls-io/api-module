package io.nuls;

import io.nuls.api.bean.SpringLiteContext;
import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.model.TransactionInfo;
import io.nuls.sdk.core.SDKBootstrap;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {

    @Autowired
    WalletRPCHandler walletRPCHandler;

    @Before
    public void init() {

        SDKBootstrap.init("127.0.0.1", "8001");
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

    @Test
    public void testGetBlock2() {
        SpringLiteContext.init("io.nuls");
        RpcClientResult<BlockInfo> clientResult = walletRPCHandler.getBlock(68108,"1");
        System.out.println(clientResult.isSuccess());
    }

    @Test
    public void testGetTx() {
        String hash = "0020e0e63e090e1b5c068f8230708f3928fe4ca026b7cbd72595651ef25800fe1ee1e";
        RpcClientResult<TransactionInfo> clientResult = walletRPCHandler.getTx(hash);
        System.out.println(clientResult.isSuccess());
    }

    @Test
    public void gestGettx() {
        String hash = "0020e0e63e090e1b5c068f8230708f3928fe4ca026b7cbd72595651ef25800fe1eee";
        walletRPCHandler.queryTxInput(hash);
    }

    @Test
    public void test() {
        String s = "aaaaaaaa,bbbbbbb,ccccccc,ddddd,eeeeee";
        System.out.println(s.substring(s.length()-8));
    }
}

package io.nuls;

import static org.junit.Assert.assertTrue;

import io.nuls.api.core.util.RestFulUtils;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for simple App.
 */
public class AppTest {


    private static String serverUri = "http://127.0.0.1:6001";

    @Before
    public void init() {
        RestFulUtils.getInstance().init(serverUri);
    }

    /**
     * Rigorous Test :-)
     */
    @Test
    public void shouldAnswerWithTrue() {
        assertTrue(true);
    }

    @Test
    public void testGetBlockHeader() {
//        RpcClientResult<BlockHeader> clientResult =
    }
}

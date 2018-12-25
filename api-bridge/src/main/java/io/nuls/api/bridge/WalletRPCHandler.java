package io.nuls.api.bridge;

import io.nuls.api.core.model.BlockHeader;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.util.Log;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.model.Result;
import io.nuls.sdk.core.utils.RestFulUtils;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 钱包RPC调用处理器
 */
@Component
public class WalletRPCHandler {

    private RestFulUtils restFulUtils = RestFulUtils.getInstance();

    /**
     * 按照高度同步区块
     *
     * @param height
     * @return
     */
    public RpcClientResult<BlockHeader> getBlockHeader(long height) {
        Result result = restFulUtils.get("/block/header/height/" + height, null);
        RpcClientResult clientResult = new RpcClientResult();
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        try {
            BlockHeader blockHeader = AnalysisHandler.toBlockHeader((Map<String, Object>) result.getData());
            result.setData(blockHeader);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }


}

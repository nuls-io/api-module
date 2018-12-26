package io.nuls.api.bridge;

import io.nuls.api.core.model.BlockHeader;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.util.Log;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.model.Block;
import io.nuls.sdk.core.model.Result;
import io.nuls.sdk.core.utils.RestFulUtils;
import io.nuls.sdk.tool.NulsSDKTool;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 钱包RPC调用处理器
 */
@Component
public class WalletRPCHandler {

    private RestFulUtils restFulUtils = RestFulUtils.getInstance();

    /**
     * 根据高度同步区块头
     * @param height 区块高度
     * @return 区块头信息
     */
    public RpcClientResult<BlockHeader> getBlockHeader(long height) {
        Result result = restFulUtils.get("/block/header/height/" + height, null);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = new RpcClientResult();
        try {
            BlockHeader blockHeader = AnalysisHandler.toBlockHeader((Map<String, Object>) result.getData());
            result.setData(blockHeader);
        } catch (Exception e) {
            Log.error(e);
            clientResult = RpcClientResult.getFailed(KernelErrorCode.DATA_PARSE_ERROR);
        }
        return clientResult;
    }

    /**
     * 根据区块hash获取完整区块
     * @param hash 区块hash
     * @return 区块信息
     */
    public RpcClientResult<Block> getBlock(String hash) {
        Result result = NulsSDKTool.getBlockWithBytes(hash);
        if (result.isFailed()) {
            return RpcClientResult.errorResult(result);
        }
        RpcClientResult clientResult = new RpcClientResult();

        return null;
    }
}

package io.nuls.api.task;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.service.BlockService;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.utils.Log;

/**
 * 区块同步定时任务
 * 同步钱包的区块，同步到最新块后，再定时执行检查是否有最新区块
 */
@Component
public class SyncBlockTask implements Runnable {

    @Autowired
    private WalletRPCHandler walletRPCHandler;
    @Autowired
    private BlockService blockService;

    @Override
    public void run() {
        boolean running = true;
        while (running) {
            try {
                running = syncBlock();
            } catch (Exception e) {
                Log.error(e);
            }
        }
    }

    /**
     * 同步逻辑
     * 1. 从本地取出已保存的最新块的记录
     * 2. 根据本地最新块的高度，去同步钱包的下一个块（本地没有则从第0块开始）
     * 3. 同步到最新块后，任务结束，等待下个10秒，重新同步
     * 4. 每次同步都需要和上一块做连续性验证，如果验证失败，说明本地分叉，需要做回滚处理
     *
     * @return boolean 是否还继续同步
     */
    private boolean syncBlock() {
        long localBestHeight;
        //取出本地已经同步到最新块，然后根据高度同步下一块
        BlockHeaderInfo localBestBlockHeader = blockService.getBestBlockHeader();
        if (localBestBlockHeader == null) {
            localBestHeight = -1;
        } else {
            localBestHeight = localBestBlockHeader.getHeight();
        }
        //调用RPC接口获取节点钱包下一区块
        RpcClientResult<BlockHeaderInfo> result = null;
        try {
            result = walletRPCHandler.getBlockHeader(localBestHeight + 1);
        } catch (Exception e) {
            Log.error("--------获取下一区块头信息异常:" + e);
            return false;
        }

        //根据返回结果，做相应的处理
        if (result.isSuccess()) {
            return processWithSuccessResult(result, localBestBlockHeader);
        } else {
            return processWithFailResult(result, localBestBlockHeader);
        }
    }

    private boolean processWithSuccessResult(RpcClientResult<BlockHeaderInfo> result, BlockHeaderInfo localBestBlockHeader) {
        BlockHeaderInfo newBlockHeader = result.getData();
        //验证区块连续性
        if (checkBlockContinuity(localBestBlockHeader, newBlockHeader)) {
            RpcClientResult<BlockInfo> blockResult = walletRPCHandler.getBlock(newBlockHeader.getHash());
            if (blockResult.isSuccess()) {
                return blockService.saveNewBlock(blockResult.getData());
            } else {
                Log.error("--------sync new block error:" + blockResult.getMsg());
            }
        } else {
            return blockService.rollbackBlock();
        }
        return false;
    }

    private boolean processWithFailResult(RpcClientResult<BlockHeaderInfo> result, BlockHeaderInfo localBestBlockHeader) {
        if (!(result.getCode() + "").equals(KernelErrorCode.BLOCK_IS_NULL.getCode())) {
            //如果错误信息不是未找到最新块的话 就说明查询区块报错
            Log.error("-------获取下一区块头信息失败:" + result.getCode() + "-" + result.getMsg());
        }
        //todo 如果返回的错误码是最新块未找到时
        // 是否需要判断本地已保存的最新区块头和钱包里相同高度区块的一致性
        return false;
    }

    /**
     * 区块连续性检查
     *
     * @param localBest
     * @param newest
     * @return
     */
    private boolean checkBlockContinuity(BlockHeaderInfo localBest, BlockHeaderInfo newest) {
        if (localBest == null) {
            if (newest.getHeight() == 0) {
                return true;
            } else {
                return false;
            }
        } else {
            if (newest.getHeight() == localBest.getHeight() + 1) {
                if (newest.getPreHash().equals(localBest.getHash())) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }
    }
}

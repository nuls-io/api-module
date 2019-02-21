package io.nuls.api.task;

import io.nuls.api.bean.annotation.Autowired;
import io.nuls.api.bean.annotation.Component;
import io.nuls.api.bridge.WalletRPCHandler;
import io.nuls.api.core.ApiContext;
import io.nuls.api.core.model.BlockHeaderInfo;
import io.nuls.api.core.model.BlockInfo;
import io.nuls.api.core.model.RpcClientResult;
import io.nuls.api.core.mongodb.MongoDBService;
import io.nuls.api.service.BlockHeaderService;
import io.nuls.api.service.RollbackService;
import io.nuls.api.service.SyncService;
import io.nuls.sdk.core.contast.KernelErrorCode;
import io.nuls.sdk.core.utils.Log;
import org.bson.Document;

/**
 * 区块同步定时任务
 * 同步钱包的区块，同步到最新块后，再定时执行检查是否有最新区块
 */
@Component
public class SyncBlockTask implements Runnable {

    @Autowired
    private WalletRPCHandler walletRPCHandler;
    @Autowired
    private BlockHeaderService blockHeaderService;
    @Autowired
    private SyncService syncService;
    @Autowired
    private RollbackService rollbackBlock;
    @Autowired
    private MongoDBService mongoDBService;

    @Override
    public void run() {
        if (!ApiContext.doSync) {
            return;
        }
        /*
         * 首先检查上一同步区块时，是否有完整解析和存储区块信息，
         * 如果不完整，先需要回滚掉上个区块的数据，再重新同步
         */
        Document document = blockHeaderService.getBestBlockHeightInfo();
        if (document != null && !document.getBoolean("finish")) {
            try {
                rollbackBlock.rollbackBlock(document.getLong("height"));
            } catch (Exception e) {
                Log.error(e);
                return;
            }
        }

        boolean running = true;
        while (running) {
            try {
//                long start = System.currentTimeMillis();
                running = syncBlock();
//                if (running) {
//                    Log.info("whole block use:" + (System.currentTimeMillis() - start));
//                }
            } catch (Exception e) {
                Log.error(e);
                running = false;
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
        BlockHeaderInfo localBestBlockHeader = blockHeaderService.getBestBlockHeader();
        if (localBestBlockHeader == null) {
            localBestHeight = -1;
        } else {
            localBestHeight = localBestBlockHeader.getHeight();
        }

//         测试的时候，只同步到20万块，测试回滚
//        if (localBestHeight >= 200000) {
//            return false;
//        }
        ApiContext.bestHeight = localBestHeight;
        try {
            return processWithSuccessResult(localBestBlockHeader);
        } catch (Exception e) {
            Log.error(e);
//            session.abortTransaction();
            return false;
        } finally {
//            session.close();
        }
    }

    private boolean processWithSuccessResult(BlockHeaderInfo localBestBlockHeader) throws Exception {
        long start = System.nanoTime();
        RpcClientResult<BlockInfo> blockResult = walletRPCHandler.getBlock(localBestBlockHeader.getHeight() + 1);
        Log.info("request use:" + (System.nanoTime() - start) + "ns");

        if (blockResult.isSuccess()) {
            BlockHeaderInfo newBlockHeader = blockResult.getData().getBlockHeader();
            start = System.nanoTime();
            //验证区块连续性
            if (checkBlockContinuity(localBestBlockHeader, newBlockHeader)) {
                Log.info("check use:" + (System.nanoTime() - start) + "ns");
                start = System.nanoTime();
                boolean result = syncService.saveNewBlock(blockResult.getData());
                Log.info("save use:" + (System.nanoTime() - start) + "ns");
                return result;
            } else {
                if (localBestBlockHeader != null) {
                    return rollbackBlock.rollbackBlock(localBestBlockHeader.getHeight());
                }
            }
        }
        return false;
    }

    private boolean processWithFailResult(RpcClientResult<BlockHeaderInfo> result, BlockHeaderInfo localBestBlockHeader) {
        if (!(result.getCode() + "").equals(KernelErrorCode.BLOCK_IS_NULL.getCode())) {
            //如果错误信息不是未找到最新块的话 就说明查询区块报错
            Log.error("-------获取下一区块头信息失败:" + result.getCode() + "-" + result.getMsg());
        }
        // 如果返回的错误码是最新块未找到时
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

    /**
     * 测试回滚的时候 ，把上面方法注释掉，启用下面这个方法就可以
     */
//    private boolean checkBlockContinuity(BlockHeaderInfo localBest, BlockHeaderInfo newest) {
//        return false;
//    }
}

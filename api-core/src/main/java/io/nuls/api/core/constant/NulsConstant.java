package io.nuls.api.core.constant;

import java.util.HashSet;
import java.util.Set;

public class NulsConstant {

    //黄牌惩罚
    public static final int PUBLISH_YELLOW = 1;
    //红牌惩罚
    public static final int PUTLISH_RED = 2;
    //尝试分叉
    public static final int TRY_FORK = 1;
    //打包双花交易
    public static final int DOUBLE_SPEND = 2;
    //太多黄牌惩罚
    public static final int TOO_MUCH_YELLOW_PUNISH = 3;
    //委托共识
    public static final int JOIN_CONSENSUS = 0;
    //取消委托共识
    public static final int CANCEL_CONSENSUS = 1;

    public static final Set<String> SEED_NODE_ADDRESS = new HashSet<>();

    static {
        SEED_NODE_ADDRESS.add("Nse82gBCKKk7VqZZBQriobM7qJLTNULS");
        SEED_NODE_ADDRESS.add("Nse4QvHepkFw8igZC8qzH9VUj2KPNULS");
        SEED_NODE_ADDRESS.add("Nse6tpcdrkBeZyzeRpea4wHxuRL9NULS");
        SEED_NODE_ADDRESS.add("NsdtQumE67eeSTEJtNmq27Fv9uCWNULS");
        SEED_NODE_ADDRESS.add("NsduWJCm1JhSzqQgHVEUEAfUpmT7NULS");
        SEED_NODE_ADDRESS.add("Nse1ozJ8y7FE5Awv7zvbAoenHvmhEhJL");
        SEED_NODE_ADDRESS.add("TTakkAtUaBCY6XLLbEt7vWLqP4SuNULS");
        SEED_NODE_ADDRESS.add("TTak8TVrVWwG42RXAD1g4AAnYqNrNULS");
        SEED_NODE_ADDRESS.add("TTavq5rDMcnU4FRRRBJdPFMXfYfuNULS");
        SEED_NODE_ADDRESS.add("TTaqyWKAbTSJp1PvBeiEnM1MWTytTfnm");
    }
}

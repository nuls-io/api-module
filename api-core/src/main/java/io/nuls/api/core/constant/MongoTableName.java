package io.nuls.api.core.constant;

public class MongoTableName {

    //存储最新区块的各种相关信息
    public static final String NEW_INFO = "new_info";
    //区块头表
    public static final String BLOCK_HEADER = "block_header";
    //共识节点信息表
    public static final String AGENT_INFO = "agent_info";
    //别名信息表
    public static final String ALIAS_INFO = "alias_info";
    //账户信息表
    public static final String ACCOUNT_INFO = "account_info";
    //委托记录表
    public static final String DEPOSIT_INFO = "deposit_info";
    //交易关系记录表
    public static final String TX_RELATION_INFO = "tx_relation_info";
    //交易表
    public static final String TX_INFO = "tx_info";
    //红黄牌记录表
    public static final String PUNISH_INFO = "punish_info";
    //UTXO记录
    public static final String UTXO_INFO = "utxo_info";

    public static final String ROUND_INFO = "round_table";

    public static final String ROUND_ITEM_INFO = "round_item_table";
    //账户token信息表
    public static final String ACCOUNT_TOKEN_INFO = "account_token_info";


    public static final String TX_COUNT_INFO = "tx_count_info";

    //------------------------------------字段------------------------------------------------------------


    //new_info表，最新高度的_id字段名
    public static final String BEST_BLOCK_HEIGHT = "best_block_height";

    //new_info表，最新交易数量统计的id字段并
    public static final String BEST_TX_COUNT_ID = "best_tx_count_id";


}

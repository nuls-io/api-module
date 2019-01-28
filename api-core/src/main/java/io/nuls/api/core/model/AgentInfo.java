package io.nuls.api.core.model;

public class AgentInfo extends TxData {

    private String txHash;

    private String agentId;

    private String agentAddress;

    private String packingAddress;

    private String rewardAddress;

    private String agentAlias;

    private Long deposit;

    private int commissionRate;

    private long createTime;

    private int status;

    private long totalDeposit;

    private int depositCount;

    private double creditValue;

    private long totalPackingCount;

    private double lostRate;

    private long lastRewardHeight;

    private String deleteHash;

    private long blockHeight;

    private long deleteHeight;

    private long totalReward;

    private long commissionReward;

    private long agentReward;

    private boolean isNew;

    private long roundPackingTime;

    private int version;

    public AgentInfo() {
        totalReward = 0L;
        totalPackingCount = 0L;
    }

    public String getTxHash() {
        return txHash;
    }

    public void setTxHash(String txHash) {
        this.txHash = txHash;
    }

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getAgentAddress() {
        return agentAddress;
    }

    public void setAgentAddress(String agentAddress) {
        this.agentAddress = agentAddress;
    }

    public String getPackingAddress() {
        return packingAddress;
    }

    public void setPackingAddress(String packingAddress) {
        this.packingAddress = packingAddress;
    }

    public String getRewardAddress() {
        return rewardAddress;
    }

    public void setRewardAddress(String rewardAddress) {
        this.rewardAddress = rewardAddress;
    }

    public String getAgentAlias() {
        return agentAlias;
    }

    public void setAgentAlias(String agentAlias) {
        this.agentAlias = agentAlias;
    }

    public Long getDeposit() {
        return deposit;
    }

    public void setDeposit(Long deposit) {
        this.deposit = deposit;
    }

    public int getCommissionRate() {
        return commissionRate;
    }

    public void setCommissionRate(int commissionRate) {
        this.commissionRate = commissionRate;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getTotalDeposit() {
        return totalDeposit;
    }

    public void setTotalDeposit(long totalDeposit) {
        this.totalDeposit = totalDeposit;
    }

    public int getDepositCount() {
        return depositCount;
    }

    public void setDepositCount(int depositCount) {
        this.depositCount = depositCount;
    }

    public double getCreditValue() {
        return creditValue;
    }

    public void setCreditValue(double creditValue) {
        this.creditValue = creditValue;
    }

    public long getTotalPackingCount() {
        return totalPackingCount;
    }

    public void setTotalPackingCount(long totalPackingCount) {
        this.totalPackingCount = totalPackingCount;
    }

    public long getLastRewardHeight() {
        return lastRewardHeight;
    }

    public void setLastRewardHeight(long lastRewardHeight) {
        this.lastRewardHeight = lastRewardHeight;
    }

    public String getDeleteHash() {
        return deleteHash;
    }

    public void setDeleteHash(String deleteHash) {
        this.deleteHash = deleteHash;
    }

    public long getDeleteHeight() {
        return deleteHeight;
    }

    public void setDeleteHeight(long deleteHeight) {
        this.deleteHeight = deleteHeight;
    }

    public long getTotalReward() {
        return totalReward;
    }

    public void setTotalReward(long totalReward) {
        this.totalReward = totalReward;
    }

    public boolean isNew() {
        return isNew;
    }

    public void setNew(boolean aNew) {
        isNew = aNew;
    }

    public double getLostRate() {
        return lostRate;
    }

    public void setLostRate(double lostRate) {
        this.lostRate = lostRate;
    }

    public long getRoundPackingTime() {
        return roundPackingTime;
    }

    public void setRoundPackingTime(long roundPackingTime) {
        this.roundPackingTime = roundPackingTime;
    }

    public long getCommissionReward() {
        return commissionReward;
    }

    public void setCommissionReward(long commissionReward) {
        this.commissionReward = commissionReward;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public long getBlockHeight() {
        return blockHeight;
    }

    public void setBlockHeight(long blockHeight) {
        this.blockHeight = blockHeight;
    }

    public long getAgentReward() {
        return agentReward;
    }

    public void setAgentReward(long agentReward) {
        this.agentReward = agentReward;
    }
}

package io.nuls.api.core.model;

public enum ProgramStatus {

    /**
     * 0-未找到、1-正常、2-停止
     */
    NOT_FOUND("not_found", 0), NORMAL("normal", 1), STOP("stop", 2);

    //状态描述
    private String name;
    //状态编码
    private int code;

    ProgramStatus(String name, int code) {
        this.name = name;
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public static ProgramStatus codeOf(String name){
        for(ProgramStatus status : values()){
            if(status.getName().equals(name)){
                return status;
            }
        }
        throw new RuntimeException("没有找到对应的枚举");
    }
}

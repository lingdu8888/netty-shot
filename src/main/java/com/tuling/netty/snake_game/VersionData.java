package com.tuling.netty.snake_game;

/**
 * 版本数据
 * Created by Tommy on 2018/1/16.
 */
public class VersionData {
    private long version;// 版本号
    private long vTime; // 版本构建时间
    private Boolean full;
    private String cmds[]; // 命令
    private String cmdDatas[];// 命令数据

    public VersionData(long version, long vTime) {
        this.version = version;
        this.vTime = vTime;
        this.full=false;
    }


    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getvTime() {
        return vTime;
    }

    public void setvTime(long vTime) {
        this.vTime = vTime;
    }

    public Boolean getFull() {
        return full;
    }

    public void setFull(Boolean full) {
        this.full = full;
    }

    public String[] getCmds() {
        return cmds;
    }

    public void setCmds(String[] cmds) {
        this.cmds = cmds;
    }

    public String[] getCmdDatas() {
        return cmdDatas;
    }

    public void setCmdDatas(String[] cmdDatas) {
        this.cmdDatas = cmdDatas;
    }
}

package com.rm.freedrawsample;

/**
 * Created by Administrator on 2017/4/24.
 */

public class User {
    private long time;
    private String info;

    public User(long time, String info) {
        this.time = time;
        this.info = info;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}

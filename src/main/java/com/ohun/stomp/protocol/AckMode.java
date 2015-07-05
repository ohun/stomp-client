package com.ohun.stomp.protocol;

/**
 * Created by xiaoxu.yxx on 2014/7/17.
 */
public enum AckMode {

    AUTO("auto", "自动确认"),

    CLIENT("client", "客户端确认(累计)"),

    CLIENT_INDIVIDUAL("client-individual", "客户端确认(无累计)");

    AckMode(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public final String name;
    public final String desc;
}

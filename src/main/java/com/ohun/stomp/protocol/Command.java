package com.ohun.stomp.protocol;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public enum Command {

    /*=========Client Command===========*/
    STOMP("STOMP", "请求连接"),
    CONNECT("CONNECT", "请求连接"),
    DISCONNECT("DISCONNECT", "断开连接"),
    SEND("SEND", "发送消息到stomp server"),
    SUBSCRIBE("SUBSCRIBE", "订阅"),
    UNSUBSCRIBE("UNSUBSCRIBE", "取消订阅"),
    BEGIN("BEGIN", "开始事务"),
    COMMIT("COMMIT", "提交事务"),
    ABORT("ABORT", "取消事务"),
    ACK("ACK", "确认接受"),
    NACK("NACK", "确认不能接受"),

    /*=========Server Command===========*/
    MESSAGE("MESSAGE", "下发消息到订阅者(client)"),
    RECEIPT("RECEIPT", "回执消息"),
    CONNECTED("CONNECTED", "已连接"),
    ERROR("ERROR", "错误");

    Command(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public final String name;
    public final String desc;
}

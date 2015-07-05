package com.ohun.stomp.common;

/**
 * Created by xiaoxu.yxx on 2014/9/30.
 * <p/>
 * stomp client config
 *
 * @author ohun@live.cn
 * @version 1.0
 */
public final class StompConfig implements Cloneable {
    /**
     * stomp server host
     */
    private String host;
    /**
     * stomp server port
     */
    private int port;
    private String login;
    private String pass;

    private long connectTimeout = 3000;

    /**
     * default client send ping pre 10min when Idle
     * unit millisecond
     */
    private long heartbeatX = 600000;

    /**
     * default client received pong pre 10s when Idle
     * unit millisecond
     */
    private long heartbeatY = 7000;

    /**
     * monitor server host period
     * unit is minute
     */
    private int monitorPeriod = 5;

    private int connectCountPreHost = 1;

    public StompConfig() {
    }

    public StompConfig(String host, int port, String login, String pass) {
        this.host = host;
        this.port = port;
        this.login = login;
        this.pass = pass;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getConnectCountPreHost() {
        return connectCountPreHost;
    }

    public void setConnectCountPreHost(int connectCountPreHost) {
        this.connectCountPreHost = connectCountPreHost;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public long getHeartbeatX() {
        return heartbeatX;
    }

    public void setHeartbeatX(long heartbeatX) {
        this.heartbeatX = heartbeatX;
    }

    public long getHeartbeatY() {
        return heartbeatY;
    }

    public void setHeartbeatY(long heartbeatY) {
        this.heartbeatY = heartbeatY;
    }

    public int getMonitorPeriod() {
        return monitorPeriod;
    }

    public void setMonitorPeriod(int monitorPeriod) {
        this.monitorPeriod = monitorPeriod;
    }

    @Override
    public StompConfig clone() {
        try {
            return (StompConfig) super.clone();
        } catch (CloneNotSupportedException e) {
            return new StompConfig(host, port, login, pass);
        }
    }

    @Override
    public String toString() {
        return '{' +
                "host='" + host + '\'' +
                ", port=" + port +
                ", login='" + login + '\'' +
                ", pass='" + pass + '\'' +
                ", connectTimeout=" + connectTimeout +
                '}';
    }
}

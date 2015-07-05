package com.ohun.stomp.util;

import com.ohun.stomp.api.StompException;
import com.ohun.stomp.common.StompConfig;
import com.ohun.stomp.protocol.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Created by xiaoxu.yxx on 2014/7/25.
 */
public final class Stomps {
    private static Logger logger = LoggerFactory.getLogger(Stomps.class);

    public static StompConfig parseUri(String uri_str) {
        URI uri = null;
        try {
            uri = new URI(uri_str);
        } catch (URISyntaxException e) {
            logger.error("parse stomp uri error, uri=" + uri, e);
        }
        if (uri == null || !"stomp".equals(uri.getScheme())) {
            throw new StompException("the uri is not stomp uri=" + uri);
        }
        StompConfig config = new StompConfig();
        config.setHost(uri.getHost());
        config.setPort(uri.getPort());
        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            String[] login_pass = userInfo.split(":");
            if (login_pass.length == 2) {
                config.setLogin(login_pass[0]);
                config.setPass(login_pass[1]);
            }
        }

        String query = uri.getQuery();
        if (query == null || query.length() == 0) return config;
        String[] kvs = query.split("&");
        if (kvs.length == 0) return config;
        for (String kv : kvs) {
            String[] k_v = kv.split("=");
            if (k_v.length != 2) continue;
            if (k_v[0].equals("connectTimeout"))
                config.setConnectTimeout(Long.parseLong(k_v[1].trim()));
            else if (k_v[0].equals("heartbeatX"))
                config.setHeartbeatX(Long.parseLong(k_v[1].trim()));
            else if (k_v[0].equals("heartbeatY"))
                config.setHeartbeatY(Long.parseLong(k_v[1].trim()));
            else if (k_v[0].equals("connectCountPreHost"))
                config.setConnectCountPreHost(Integer.parseInt(k_v[1].trim()));

        }
        return config;
    }

    /**
     * 根据主IP地址获取子IP列表
     *
     * @param host
     * @return
     */
    public static List<String> getHostAddress(String host) {
        List<String> list = new ArrayList<String>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress address : addresses) {
                list.add(address.getHostAddress());
            }
        } catch (UnknownHostException e) {
            logger.error("failed to getHostAddress.", e);
        }
        return list;
    }

    /**
     * 拆包
     *
     * @param body
     * @return
     */
    public static Map<String, String> load(byte[] body) {
        Map<String, String> map = new HashMap<String, String>();
        ByteBuffer buffer = ByteBuffer.wrap(body);
        try {
            int fieldNum = buffer.getInt();
            for (int i = 0; i < fieldNum; i++) {
                int keyLen = buffer.get();
                byte[] key = new byte[keyLen];
                buffer.get(key, 0, keyLen);
                int valueLen = buffer.getInt();
                byte[] value = new byte[valueLen];
                buffer.get(value, 0, valueLen);
                map.put(bts(key), bts(value));
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return map;
    }


    public static String bts(byte[] buffer) {
        return new String(buffer, Constants.UTF_8);
    }
}

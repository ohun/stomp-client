package com.ohun.stomp;

import com.ohun.stomp.api.ClientListener;
import com.ohun.stomp.api.StompException;
import com.ohun.stomp.common.StompConfig;
import com.ohun.stomp.util.Function;
import com.ohun.stomp.util.Stomps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by xiaoxu.yxx on 2014/10/17.
 * <p/>
 * manager all client and monitor the server host change.
 *
 * @author ohun@live.cn
 * @version 1.0
 * @see StompClient
 */
public final class StompClientManager implements ClientListener {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ScheduledExecutorService schedule = Executors.newSingleThreadScheduledExecutor();

    final ConsumerManager consumerManager = new ConsumerManager(this);

    final ProducerManager producerManager = new ProducerManager(this);

    private final Map<String, StompClient> clientMap = new ConcurrentHashMap<String, StompClient>();

    private List<StompClient> clients = new ArrayList<StompClient>();

    private boolean isShutdown = false;

    private StompConfig config;

    private String uri;

    /**
     * this method for that you need init this manager as a spring bean.
     * in this way you need set stompConfig or set stomp uri.
     * this method won't throw any exception.
     *
     * @see StompClientManager#setConfig(StompConfig)
     * @see StompClientManager#setUri(String)
     */
    public void start() {
        if (this.config == null && uri != null) {
            this.config = Stomps.parseUri(uri);
        }
        try {
            this.connect();
        } catch (Exception e) {
            logger.error("StompClientManager init failed,please your stomp config", e);
        }
    }

    /**
     * this method for that you need stop this manager as a spring bean.
     * this method won't throw any exception.
     *
     * @see StompClientManager#start()
     */
    public void stop() {
        this.isShutdown = true;
        this.schedule.shutdownNow();
        this.disconnect();
        this.clientMap.clear();
        this.clients.clear();
        this.consumerManager.shutdown();
        this.producerManager.shutdown();
    }

    /**
     * parse uri and connect to stomp server
     * the uri protocol must a stomp protocol:
     * stomp://username:password@127.0.0.1:61613?connectTimeout=1000
     *
     * @param uri
     * @throws Exception
     * @see StompConfig
     */
    public void connect(String uri) throws Exception {
        this.config = Stomps.parseUri(uri);
        this.connect();
    }

    public void connect() throws Exception {
        if (this.isShutdown) {
            throw new StompException("stomp client manager is shutdown!");
        }
        this.initClients();
        this.startHostMonitor();
    }

    public void disconnect() {
        for (StompClient client : clients) {
            client.disconnect();
        }
    }

    /**
     * create message consumer for this destination
     * use it your can subscribe the destination
     *
     * @param destination
     * @return
     */
    public MessageConsumer createConsumer(String destination) {
        return new MessageConsumer(consumerManager, destination);
    }

    /**
     * create a message producer for this destination
     * use it your can send message to the destination
     *
     * @param destination
     * @return
     */
    public MessageProducer createProducer(String destination) {
        return new MessageProducer(producerManager, destination);
    }


    /**
     * the uri protocol must a stomp protocol:
     * stomp://username:password@127.0.0.1:61613?connectTimeout=1000
     * the param part is StompConfig fields
     *
     * @param uri
     * @see StompConfig
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setConfig(StompConfig config) {
        this.config = config;
    }

    public StompConfig getConfig() {
        return config;
    }

    private void initClients() {
        if (this.config == null) {
            throw new StompException("stomp uri is null,please set the stomp uri");
        }
        List<String> list = Stomps.getHostAddress(config.getHost());
        if (list.isEmpty()) {
            throw new StompException("stomp server host address is null,host=" + config.getHost());
        }
        logger.warn("Stomp server host ip list=" + list);
        int L = Math.max(1, config.getConnectCountPreHost());
        for (int i = 0; i < L; i++) {
            for (String ip : list) {
                createClient(ip);
            }
        }
    }

    private StompClient createClient(String ip) {
        StompConfig cfg = config.clone();
        cfg.setHost(ip);
        StompClient client = new StompClient(cfg, this);
        try {
            client.connect();
        } catch (Exception e) {
            logger.error("[Exception] client connect failed,config=" + cfg, e);
        }
        return client;
    }

    private void startHostMonitor() {
        long period = config.getMonitorPeriod();
        HostMonitor monitor = new HostMonitor();
        this.schedule.scheduleAtFixedRate(monitor, period, period, TimeUnit.SECONDS);
    }


    StompClient foreach(Function<StompClient, Boolean> fun) {
        for (StompClient client : clientMap.values()) {
            if (fun.apply(client)) continue;
            return client;
        }
        return null;
    }

    List<StompClient> getClients() {
        return clients;
    }

    @Override
    public void onConnected(StompClient client) {
        clientMap.put(client.getConfig().getHost(), client);
        clients = new ArrayList<StompClient>(clientMap.values());
        logger.warn("One stomp client connect, client={}, count={},", client.getConfig(), clientMap.size());
    }

    @Override
    public void onDisconnected(StompClient client) {
        clientMap.remove(client.getConfig().getHost());
        clients = new ArrayList<StompClient>(clientMap.values());
        logger.warn("One stomp client disconnect, client={}, count={},", client.getConfig(), clientMap.size());
    }

    @Override
    public void onException(StompClient client, Throwable throwable) {
        clientMap.remove(client.getConfig().getHost());
        clients = new ArrayList<StompClient>(clientMap.values());
        logger.warn("One stomp client exception, client={}, count={},", client.getConfig(), clientMap.size());
    }

    /**
     * stomp server host address monitor
     * when stomp server host changed
     * to add or remove stomp client
     */
    private final class HostMonitor implements Runnable {

        @Override
        public void run() {
            List<String> allIps = Stomps.getHostAddress(config.getHost());
            if (allIps == null || allIps.isEmpty()) {
                logger.warn("stomp server host address is null,host=" + config.getHost());
                return;
            }
            List<String> newIps = new ArrayList<String>(allIps);
            List<String> delIps = new ArrayList<String>(2);
            for (Map.Entry<String, StompClient> entry : clientMap.entrySet()) {
                String ip = entry.getKey();
                StompClient client = entry.getValue();
                if (!client.isConnected()) {//如果链接断开直接移除
                    delIps.add(ip);
                    continue;
                }

                if (allIps.contains(ip)) {//都包含
                    newIps.remove(ip);//去除都包含的,剩下的是新增的
                } else {
                    delIps.add(ip);//没包含的是要被删除的
                }
            }

            //host address not changed
            if (delIps.isEmpty() && newIps.isEmpty()) return;

            logger.warn("StompClientManager server host changed, delIps={}, newIps={}", delIps, newIps);

            for (String delIp : delIps) {
                StompClient client = clientMap.remove(delIp);
                if (client == null || !client.isConnected()) continue;
                client.disconnect();
            }

            for (String newIp : newIps) {
                StompClient client = createClient(newIp);
                consumerManager.resubscribe(client);
            }

            Iterator<StompClient> it = clientMap.values().iterator();
            for (; it.hasNext(); ) {
                if (!it.next().isConnected()) it.remove();
            }

            clients = new ArrayList<StompClient>(clientMap.values());
        }
    }
}

## A stomp client base on netty,see wiki.

### Subscribe:
```java
public class StompSubscribeTest {
    @Test
    public void testSub() throws Exception {
        StompClientManager stompClientManager = new StompClientManager();
        stompClientManager.connect("stomp://username:password@10.232.136.85:61613?connectTimeout=3000");
        stompClientManager.createConsumer("/topic/logon")
                .id("wqteam_test")
                .ackMode(AckMode.AUTO)
                .handler(new MessageHandler() {
                    @Override
                    public void onMessage(Message message) {
                        System.out.println(message);
                    }
                })
               .subscribe();
        LockSupport.park();
    }
}
```

### Sender:
```java
public class StompSenderTest {
    @Test
    public void testSend() throws Exception {
        StompClientManager stompClientManager = new StompClientManager();
        stompClientManager.connect("stomp://username:password@10.232.136.85:61613");
        MessageProducer producer = stompClientManager.createProducer("/topic/logon3");

        for (int i = 0; i < 100; i++) {
            if (i > 5) {
                StompTransaction tx = producer.begin();
                Thread.sleep(1000);
                tx.send(i + "风格的歌");
                tx.abort();
            } else {
                producer.send(i + "大苏打");
            }

            try {
              ReceiptFuture future = producer.sendW((i + "大苏打").getBytes("UTF-8"));
              future.await();
            } catch (Exception e) {
              e.printStackTrace();
            }
        }
        LockSupport.park();
    }
}
```

### For Spring:
```xml
<bean id="stompClientManager" class="com.ohun.stomp.StompClientManager"
            init-method="start" destroy-method="stop">
    <!--<property name="uri" value="stomp://username:password@10.232.136.85:61613"/>
           两种配置方式是一样的,看个人喜好-->
    <property name="config">
        <bean class="com.ohun.stomp.common.StompConfig">
            <property name="host" value="${wangxin.mq.stomp.host}"/>
            <property name="port" value="${wangxin.mq.stomp.port}"/>
            <property name="login" value="${wangxin.stomp.username}"/>
            <property name="pass" value="${wangxin.stomp.password}"/>
            <property name="connectTimeout" value="3000"/>
            <property name="heartbeatX" value="600000"/>
            <property name="heartbeatY" value="100000"/>
            <property name="monitorPeriod" value="60"/>
            <property name="connectCountPreHost" value="1"/>
        </bean>
    </property>
</bean>

<bean id="wx2PubMsgListener" class="com.taobao.wangxin.admin.biz.feedback.WX2PublicMsgListener"
                  init-method="init" destroy-method="destroy">
    <property name="topic" value="/topic/pamsgfromwx"/>
    <property name="clientId" value="wxadmin"/>
</bean>
```
```java
public class WX2PublicMsgListener implements MessageHandler {

    @Resource
    private StompClientManager stompClientManager;

    public void init() {
        this.executor = newExecutor();
        MessageConsumer consumer = stompClientManager.createConsumer(topic);
        consumer.id(clientId).executor(executor).handler(this).subscribe();
    }

    @Override
    public void onMessage(final Message message) {
          logger.error(message.getTextBody())
    }

    public void destroy() throws Exception {
        executor.shutdown();
    }

    private ThreadPoolExecutor newExecutor() {
        final ThreadFactory threadFactory = new BasicThreadFactory.Builder()
                    .daemon(true).namingPattern("wx-2-pub-%d").build();
        return new ThreadPoolExecutor(2, poolSize, 5L, TimeUnit.MINUTES,
                    new LinkedBlockingQueue<Runnable>(queueSize),
                    threadFactory,
                    new RejectedExecutionHandler() {
                        @Override
                        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                            logger.warn("one message task was rejected total="
                                    + rejectedCount.incrementAndGet()
                                    + ",poolStatus=" + poolStatus());
                        }
                });
    }

    private final Logger logger = LoggerFactory.getLogger(WX2PublicMsgListener.class);
    private AtomicInteger rejectedCount = new AtomicInteger(0);
    private int poolSize = 10, queueSize = 100;
    private ThreadPoolExecutor executor;

    private String topic;

    private String clientId;
}
```

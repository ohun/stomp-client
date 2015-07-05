import com.ohun.stomp.StompClientManager;
import com.ohun.stomp.api.Message;
import com.ohun.stomp.api.MessageHandler;
import com.ohun.stomp.protocol.AckMode;
import org.junit.Test;

import java.util.concurrent.locks.LockSupport;

/**
 * User: xiaoxu.yxx
 * Date: 14-4-30
 * Time: 下午6:50
 */
public class StompSubscribeTest {
    @Test
    public void testSub() throws Exception {
        StompClientManager stompClientManager = new StompClientManager();
        stompClientManager.connect("stomp://wangxin:wangxin@10.232.136.85:61613");
        stompClientManager.createConsumer("/topic/logon")
                .id("wqteam_test")
                .header("name1", "value")
                .header("name2", "value")
                .ackMode(AckMode.CLIENT)
                .handler(new MH())
                .subscribe();
        LockSupport.park();
    }

    public static class MH implements MessageHandler {
        final long start;

        public MH() {
            this.start = System.nanoTime();
        }

        @Override
        public void onMessage(Message message) {
            System.out.println(message);
            message.ack();
            // System.out.println((System.nanoTime() - start) / 100000);
        }
    }

}

import com.ohun.stomp.MessageProducer;
import com.ohun.stomp.StompClientManager;
import org.junit.Test;

/**
 * User: xiaoxu.yxx
 * Date: 14-4-30
 * Time: 下午6:50
 */
public class StompSenderTest {
    @Test
    public void testSend() throws Exception {
        StompClientManager stompClientManager = new StompClientManager();
        stompClientManager.connect("stomp://wangxin:wangxin@10.232.136.83:61613");
        MessageProducer producer = stompClientManager.createProducer("/topic/pamsgfromwx2");

        for (int i = 1; ; i++) {
            String content = i + "阿";
            producer.send(content);
            if (i % 2 == 0) Thread.sleep(100);
            /*try {
                ReceiptFuture future = producer.sendW((i + "大苏打").getBytes("UTF-8"));
                future.await();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
        }

        //LockSupport.park();
    }

}

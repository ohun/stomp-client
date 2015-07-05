package com.ohun.stomp.protocol;

import com.ohun.stomp.api.Message;
import com.ohun.stomp.common.ReceiptFuture;
import com.ohun.stomp.common.ReceiptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.ohun.stomp.protocol.Command.*;
import static com.ohun.stomp.protocol.Constants.UTF_8;
import static com.ohun.stomp.protocol.Heads.*;


/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public abstract class Stomp {
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected final ReceiptManager receiptManager = ReceiptManager.INSTANCE;//TODO share or ?

    protected abstract void transmit(Command command, Map<String, String> header, byte[] body);

    protected void transmit(Command command, Map<String, String> header) {
        transmit(command, header, null);
    }

    public void connect(Map<String, String> header) {
        transmit(CONNECT, header);
    }

    public ReceiptFuture disconnect(Map<String, String> header) {
        ReceiptFuture future = addReceipt(header);
        transmit(DISCONNECT, header);
        return future;
    }

    public void subscribe(String destination, String id, Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(3);
        header.put(DESTINATION, destination);
        header.put(ID, id);
        transmit(SUBSCRIBE, header);
        logger.info("Stomp an listener subscribe, destination={},id={}", destination, id);
    }

    public void unsubscribe(String id) {
        unsubscribe(id, null);
    }

    protected void unsubscribe(String id, Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(2);
        header.put(ID, id);
        transmit(UNSUBSCRIBE, header);
    }

    public void send(String destination, String content) {
        byte[] body = content != null ? content.getBytes(UTF_8) : null;
        Map<String, String> header = new HashMap<String, String>(3);
        header.put(CONTENT_TYPE, "text/plain");
        send(destination, body, null);
    }

    public void send(String destination, byte[] body, Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(2);
        if (body != null && !header.containsKey(CONTENT_LENGTH)) {
            header.put(CONTENT_LENGTH, Integer.toString(body.length));
        }
        header.put(DESTINATION, destination);
        transmit(SEND, header, body);
    }

    public ReceiptFuture sendW(String destination, byte[] body, Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(3);
        if (body != null && !header.containsKey(CONTENT_LENGTH)) {
            header.put(CONTENT_LENGTH, Integer.toString(body.length));
        }
        header.put(DESTINATION, destination);
        ReceiptFuture future = addReceipt(header);
        transmit(SEND, header, body);
        return future;
    }


    protected void ack(Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(3);
        transmit(Command.ACK, header);
    }


    protected void nack(Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(3);
        transmit(NACK, header);
    }

    public void begin(String txId) {
        Map<String, String> header = new HashMap<String, String>(2);
        header.put(TRANSACTION, txId);
        transmit(BEGIN, header);
    }

    public void commit(String txId) {
        Map<String, String> header = new HashMap<String, String>(2);
        header.put(TRANSACTION, txId);
        transmit(COMMIT, header);
    }

    public void abort(String txId) {
        Map<String, String> header = new HashMap<String, String>(2);
        header.put(TRANSACTION, txId);
        transmit(ABORT, header);
    }

    protected ReceiptFuture addReceipt(Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(1);
        ReceiptFuture future = receiptManager.createFuture();
        header.put(Heads.RECEIPT, future.getReceiptId());
        return future;
    }

    /**
     * ****************************************server********************************************************
     */

    public void connected(Version version, Map<String, String> header) {
        if (header == null) header = new HashMap<String, String>(1);
        header.put(Heads.VERSION, version.name);
        transmit(Command.CONNECTED, header);
    }

    public void message(Message message) {
        transmit(Command.MESSAGE, message.headers, message.body);
    }

    public void receipt(String receiptId) {
        Map<String, String> header = new HashMap<String, String>(1);
        header.put(Heads.RECEIPT_ID, receiptId);
        transmit(Command.RECEIPT, header);
    }

    public void error(String message) {
        Map<String, String> header = null;
        if (message != null) {
            header = new HashMap<String, String>(1);
            header.put(Heads.MESSAGE, message);
        }
        transmit(Command.RECEIPT, header);
    }
}

package com.ohun.stomp.common;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by xiaoxu.yxx on 2014/10/2.
 */
public final class ReceiptManager {
    public static final ReceiptManager INSTANCE = new ReceiptManager();

    private final Map<String, ReceiptFuture> futures = new ConcurrentHashMap<String, ReceiptFuture>();

    private final AtomicInteger idCount = new AtomicInteger();

    private final String idPrefix = "rid-";

    public String nextReceiptId() {
        return idPrefix + idCount.getAndIncrement();
    }

    public ReceiptFuture createFuture() {
        String receiptId = nextReceiptId();
        ReceiptFuture future = new ReceiptFuture(receiptId);
        futures.put(receiptId, future);
        return future;
    }

    public void onReceipt(String receipt_id) {
        ReceiptFuture future = futures.remove(receipt_id);
        if (future != null) {
            future.received();
        }
    }

    /*========================thread synchronized=========================*/
    public boolean waitOnReceipt(String receipt_id, long timeout)
            throws InterruptedException {
        synchronized (receipts) {
            if (!hasReceipt(receipt_id))//while or if ?
                receipts.wait(timeout);
            return hasReceipt(receipt_id);
        }
    }


    public void onReceipt2(String receipt_id) {
        receipts.add(receipt_id);
        synchronized (receipts) {
            receipts.notify();
        }
    }

    public boolean hasReceipt(String receipt_id) {
        return receipts.contains(receipt_id);
    }


    public void clearReceipt(String receipt_id) {
        receipts.remove(receipt_id);
    }


    public void clearReceipts() {
        receipts.clear();
    }


    private final List<String> receipts = new CopyOnWriteArrayList<String>();

}

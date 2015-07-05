package com.ohun.stomp.common;

import java.util.concurrent.*;

/**
 * Created by xiaoxu.yxx on 2014/10/18.
 */
public final class ReceiptFuture {
    private String errorMessage;

    private final String receiptId;

    private final FutureTask future;

    ReceiptFuture(String receiptId) {
        this.receiptId = receiptId;
        this.future = new FutureTask(NO_OP);
    }

    void received() {
        future.run();
    }

    void received(String errorMessage) {
        this.errorMessage = errorMessage;
        future.run();
    }

    public String await() throws InterruptedException, ExecutionException {
        future.get();
        return this.errorMessage;
    }

    public String await(long timeout) throws InterruptedException, ExecutionException, TimeoutException {
        future.get(timeout, TimeUnit.MILLISECONDS);
        return this.errorMessage;
    }

    public boolean isError() throws InterruptedException, ExecutionException {
        return (this.await() != null);
    }

    public String getReceiptId() {
        return receiptId;
    }


    private static final Callable<Void> NO_OP = new Callable<Void>() {
        public Void call() throws Exception {
            return null;
        }
    };
}

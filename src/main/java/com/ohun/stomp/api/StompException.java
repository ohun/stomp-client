package com.ohun.stomp.api;

/**
 * Created by xiaoxu.yxx on 2014/7/25.
 */
public class StompException extends RuntimeException {
    public StompException() {
    }

    public StompException(String message) {
        super(message);
    }

    public StompException(String message, Throwable cause) {
        super(message, cause);
    }

    public StompException(Throwable cause) {
        super(cause);
    }
}

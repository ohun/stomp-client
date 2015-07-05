package com.ohun.stomp.protocol;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public interface Heads {
    String ACCEPT_VERSION = "accept-version";
    String CONTENT_LENGTH = "content-length";
    String CONTENT_TYPE = "content-type";
    String HOST = "host";
    String LOGIN = "login";
    String PASSCODE = "passcode";

    String VERSION = "version";
    String SESSION = "session";
    String SERVER = "server";
    String HEART_BEAT = "heart-beat";
    String DESTINATION = "destination";
    String TRANSACTION = "transaction";

    String ID = "id";
    String ACK = "ack";
    String RECEIPT = "receipt";
    String MESSAGE_ID = "message-id";
    String SUBSCRIPTION = "subscription";
    String RECEIPT_ID = "receipt-id";
    String MESSAGE = "message";

}

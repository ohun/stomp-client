package com.ohun.stomp.common;

import com.ohun.stomp.api.Channel;
import com.ohun.stomp.api.Receiver;
import com.ohun.stomp.protocol.Command;
import com.ohun.stomp.protocol.Constants;
import com.ohun.stomp.protocol.Frame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static com.ohun.stomp.protocol.Constants.UTF_8;

/**
 * Created by xiaoxu.yxx on 2014/7/16.
 */
public final class BlockChannel implements Channel, Runnable {
    private Receiver receiver;
    private BufferedReader input;
    private OutputStream output;
    private Socket socket;
    private StompConfig config;


    public BlockChannel(Receiver receiver) {
        this.receiver = receiver;
    }

    public void open() throws Exception {
        this.initChannel(config.getHost(), config.getPort());
        this.start();
    }

    private void start() {
        Thread thread = new Thread(this);
        thread.setName("BlockChannel-Thread");
        thread.setDaemon(true);
        thread.start();
    }

    private void initChannel(String host, int port) throws IOException {
        socket = new Socket(host, port);
        output = socket.getOutputStream();
        input = new BufferedReader(new InputStreamReader(socket.getInputStream(), UTF_8));
    }

    public void run() {
        // Loop reading from stream, calling receive()
        try {
            Thread thread = Thread.currentThread();
            while (!thread.isInterrupted()) {
                if (!input.ready()) {
                    if (this.isClosed()) {
                        this.close();
                        return;
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        thread.interrupt();
                    }
                    continue;
                }
                // Get command
                String command = input.readLine();
                if (command.length() == 0) continue;
                try {
                    Command c = Command.valueOf(command);
                    // Get headers
                    Map<String, String> headers = new HashMap<String, String>();
                    String header;
                    while ((header = input.readLine()).length() > 0) {
                        int ind = header.indexOf(Constants.COLON);
                        String k = header.substring(0, ind);
                        String v = header.substring(ind + 1, header.length());
                        headers.put(k, v);
                    }
                    // Read body
                    StringBuilder body = new StringBuilder();
                    int b;

                    while ((b = input.read()) != Constants.NULL_0) {
                        body.append((char) b);
                    }

                    try {
                        receiver.receive(new Frame(c, headers, body.toString().getBytes(UTF_8)));
                    } catch (Exception e) {/* We ignore these. */}
                } catch (Error e) {
                    try {
                        while (input.read() != 0) ;
                    } catch (Exception ex) {/* We ignore these. */}
                    try {
                        receiver.exception(e.getMessage(), e);
                    } catch (Exception ex) {
                    }
                }
            }
        } catch (IOException e) {
            receiver.exception(e.getMessage(), e);
        } catch (Exception e) {
            receiver.exception(e.getMessage(), e);
        } finally {
            close();
        }
    }


    @Override
    public void write(byte[] buffer) throws IOException {
        output.write(buffer);
    }

    @Override
    public boolean isClosed() {
        return socket == null || socket.isClosed();
    }

    @Override
    public void close() {
        try {
            receiver.disconnect(config);
        } catch (Exception e) {/* We ignore these. */}
        try {
            input.close();
        } catch (IOException e) {/* We ignore these. */}
        try {
            output.close();
        } catch (IOException e) {/* We ignore these. */}
        try {
            socket.close();
        } catch (IOException e) {/* We ignore these. */}
    }

    @Override
    public void setHeartbeat(long readTimeout, long writeTimeout) {

    }
}

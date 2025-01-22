package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.ConnectionHandler;
import bgu.spl.net.api.MessageEncoderDecoder;
import bgu.spl.net.api.StompMessagingProtocol;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class StompConnectionHandler<T> implements ConnectionHandler<T>, Runnable {

    private final StompMessagingProtocol<T> protocol;
    private final MessageEncoderDecoder<T> encdec;
    private final Socket socket;
    private volatile boolean connected = true;

    public StompConnectionHandler(Socket socket, MessageEncoderDecoder<T> encdec, StompMessagingProtocol<T> protocol) {
        this.socket = socket;
        this.encdec = encdec;
        this.protocol = protocol;
    }

    @Override
    public void send(T msg) {
        try {
            byte[] encodedMessage = encdec.encode(msg);
            synchronized (this) {
                socket.getOutputStream().write(encodedMessage);
                socket.getOutputStream().flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            connected = false;
        }
    }

    @Override
    public void run() {
        try {
            InputStream in = socket.getInputStream();
            byte[] buffer = new byte[1024];
            int read;
            while (connected && (read = in.read(buffer)) >= 0) {
                for (int i = 0; i < read; i++) {
                    T nextMessage = encdec.decodeNextByte(buffer[i]);
                    if (nextMessage != null) {
                        protocol.process(nextMessage);
                        if (protocol.shouldTerminate()) {
                            close();
                            return;
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            close();
        }
    }

    public void close() {
        connected = false;
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

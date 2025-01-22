package bgu.spl.net.impl.stomp;

import java.util.Map;

public class StompFrame {
    public String command;
    public Map<String, String> headers;
    public String body;

    public StompFrame(String command, Map<String, String> headers, String body) {
        this.command = command;
        this.headers = headers;
        this.body = body;
    }
    public String getCommand() {
        return command;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
}

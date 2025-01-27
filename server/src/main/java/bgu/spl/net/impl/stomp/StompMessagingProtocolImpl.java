package bgu.spl.net.impl.stomp;

import bgu.spl.net.api.StompMessagingProtocol;
import bgu.spl.net.srv.Connections;
import java.util.HashMap;
import java.util.Map;

public class StompMessagingProtocolImpl implements StompMessagingProtocol<StompFrame> {

    private int connectionId;
    private Connections<StompFrame> connections;
    private boolean shouldTerminate = false;
    private boolean loggedIn = false;
    private String username;
    private final Map<String, String> subscriptions; // Maps subscription IDs to topics

    public StompMessagingProtocolImpl() {
        this.subscriptions = new HashMap<>();
    }

    @Override
    public void start(int connectionId, Connections<StompFrame> connections) {
        this.connectionId = connectionId;
        this.connections = connections;
    }

    @Override
    public StompFrame process(StompFrame frame) {
        switch (frame.getCommand()) {
            case "CONNECT":
                return handleConnect(frame);
            case "SUBSCRIBE":
                return handleSubscribe(frame);
            case "SEND":
                return handleSend(frame);
            case "UNSUBSCRIBE":
                return handleUnsubscribe(frame);
            case "DISCONNECT":
                return handleDisconnect(frame);
            default:
                return sendError("Unknown command: " + frame.getCommand());
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    // Handle CONNECT Command
    private StompFrame handleConnect(StompFrame frame) {
        if (loggedIn) {
            return sendError("Client is already logged in.");
        }

        String login = frame.getHeaders().get("login");
        String passcode = frame.getHeaders().get("passcode");

        if (isValidUser(login, passcode)) {
            loggedIn = true;
            username = login;
            Map<String, String> map = new HashMap<>();
            map.put("version", "1.2");
            StompFrame connectedFrame = new StompFrame(
                    "CONNECTED",
                    map,
                    ""
            );
            return connectedFrame;
        } else {
            return sendError("Invalid username or password.");
        }
    }

    // Handle SUBSCRIBE Command
    private StompFrame handleSubscribe(StompFrame frame) {
        if (!loggedIn) {
            return sendError("Client is not logged in.");
        }

        String destination = frame.getHeaders().get("destination");
        String id = frame.getHeaders().get("id");

        if (destination == null || id == null) {
            return sendError("Client is not logged in.");
        }

        subscriptions.put(id, destination);
        Map<String, String> map = new HashMap<>();
        map.put("receipt-id", id);
        StompFrame receiptFrame = new StompFrame(
                "RECEIPT",
                map,
                ""
        );
        return receiptFrame;
    }

    // Handle SEND Command
    private StompFrame handleSend(StompFrame frame) {
        if (!loggedIn) {
            return sendError("Client is not logged in.");
        }
    
        String destination = frame.getHeaders().get("destination");
        if (destination == null || destination.isEmpty()) {
            return sendError("SEND frame is missing a destination header.");
        }
    
        // Forward the message to all clients subscribed to the topic
        connections.send(destination, frame);
    
        // Optionally, you can return a RECEIPT frame if a receipt header exists
        String receiptId = frame.getHeaders().get("receipt");
        if (receiptId != null) {
            Map<String, String> map = new HashMap<>();
            map.put("receipt-id", receiptId);
            return new StompFrame("RECEIPT", map, "");
        }
    
        return null; // No response is needed unless a receipt is requested
    }
    

    // Handle UNSUBSCRIBE Command
    private StompFrame handleUnsubscribe(StompFrame frame) {
        if (!loggedIn) {
            return sendError("Client is not logged in.");
        }

        String id = frame.getHeaders().get("id");
        if (id == null || !subscriptions.containsKey(id)) {
            return sendError("Invalid subscription ID: " + id);
        }

        subscriptions.remove(id);
        Map<String, String> map = new HashMap<>();
        map.put("receipt-id", id);
        StompFrame receiptFrame = new StompFrame(
                "RECEIPT",
                map,
                ""
        );
        return receiptFrame;
    }

    // Handle DISCONNECT Command
    private StompFrame handleDisconnect(StompFrame frame) {
        if (!loggedIn) {
            return sendError("Client is not logged in.");
        }

        for (String topic : subscriptions.values()) {
            Map<String, String> tempmap = new HashMap<>();
            tempmap.put("destination", topic);
            connections.send(topic, new StompFrame(
                    "MESSAGE",
                    tempmap,
                    "User " + username + " has disconnected."
            ));
        }

        shouldTerminate = true;
        connections.disconnect(connectionId);

        String receiptId = frame.getHeaders().get("receipt");
        Map<String, String> map = new HashMap<>();
        map.put("receipt-id", receiptId);
        StompFrame receiptFrame = new StompFrame(
                "RECEIPT",
                map,
                ""
        );
        return receiptFrame;
    }

    // Utility Methods
    // private void sendFrame(StompFrame frame) {
    //     connections.send(connectionId, frame);
    // }

    private StompFrame sendError(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        StompFrame errorFrame = new StompFrame(
                "ERROR",
                map,
                ""
        );
        return errorFrame;
    }

    private boolean isValidUser(String login, String passcode) {
        // Placeholder: Replace with actual authentication logic
        return login != null && passcode != null && !login.isEmpty() && !passcode.isEmpty();
    }
}

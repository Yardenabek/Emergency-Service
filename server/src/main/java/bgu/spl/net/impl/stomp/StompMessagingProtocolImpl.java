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
    public void process(StompFrame frame) {
        switch (frame.getCommand()) {
            case "CONNECT":
                handleConnect(frame);
                break;
            case "SUBSCRIBE":
                handleSubscribe(frame);
                break;
            case "SEND":
                handleSend(frame);
                break;
            case "UNSUBSCRIBE":
                handleUnsubscribe(frame);
                break;
            case "DISCONNECT":
                handleDisconnect(frame);
                break;
            default:
                sendError("Unknown command: " + frame.getCommand());
        }
    }

    @Override
    public boolean shouldTerminate() {
        return shouldTerminate;
    }

    // Handle CONNECT Command
    private void handleConnect(StompFrame frame) {
        if (loggedIn) {
            sendError("Client is already logged in.");
            return;
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
            sendFrame(connectedFrame);
        } else {
            sendError("Invalid username or password.");
        }
    }

    // Handle SUBSCRIBE Command
    private void handleSubscribe(StompFrame frame) {
        if (!loggedIn) {
            sendError("Client is not logged in.");
            return;
        }

        String destination = frame.getHeaders().get("destination");
        String id = frame.getHeaders().get("id");

        if (destination == null || id == null) {
            sendError("Missing required headers for SUBSCRIBE.");
            return;
        }

        subscriptions.put(id, destination);
        Map<String, String> map = new HashMap<>();
        map.put("receipt-id", id);
        StompFrame receiptFrame = new StompFrame(
                "RECEIPT",
                map,
                ""
        );
        sendFrame(receiptFrame);
    }

    // Handle SEND Command
    private void handleSend(StompFrame frame) {
        if (!loggedIn) {
            sendError("Client is not logged in.");
            return;
        }

        String destination = frame.getHeaders().get("destination");
        if (destination == null || !subscriptions.containsValue(destination)) {
            sendError("You are not subscribed to the topic: " + destination);
            return;
        }

        connections.send(destination, frame);
    }

    // Handle UNSUBSCRIBE Command
    private void handleUnsubscribe(StompFrame frame) {
        if (!loggedIn) {
            sendError("Client is not logged in.");
            return;
        }

        String id = frame.getHeaders().get("id");
        if (id == null || !subscriptions.containsKey(id)) {
            sendError("Invalid subscription ID: " + id);
            return;
        }

        subscriptions.remove(id);
        Map<String, String> map = new HashMap<>();
        map.put("receipt-id", id);
        StompFrame receiptFrame = new StompFrame(
                "RECEIPT",
                map,
                ""
        );
        sendFrame(receiptFrame);
    }

    // Handle DISCONNECT Command
    private void handleDisconnect(StompFrame frame) {
        if (!loggedIn) {
            sendError("Client is not logged in.");
            return;
        }

        String receiptId = frame.getHeaders().get("receipt");
        Map<String, String> map = new HashMap<>();
        map.put("receipt-id", receiptId);
        StompFrame receiptFrame = new StompFrame(
                "RECEIPT",
                map,
                ""
        );
        sendFrame(receiptFrame);

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
    }

    // Utility Methods
    private void sendFrame(StompFrame frame) {
        connections.send(connectionId, frame);
    }

    private void sendError(String message) {
        Map<String, String> map = new HashMap<>();
        map.put("message", message);
        StompFrame errorFrame = new StompFrame(
                "ERROR",
                map,
                ""
        );
        sendFrame(errorFrame);
    }

    private boolean isValidUser(String login, String passcode) {
        // Placeholder: Replace with actual authentication logic
        return login != null && passcode != null && !login.isEmpty() && !passcode.isEmpty();
    }
}

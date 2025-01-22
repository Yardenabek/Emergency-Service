package bgu.spl.net.impl.stomp;

import bgu.spl.net.srv.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.List;
import java.util.ArrayList;

public class ConnectionsImpl<T> implements Connections<T> {
    private final ConcurrentMap<Integer, ConnectionHandler<T>> connectionHandlers = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<Integer>> channelSubscriptions = new ConcurrentHashMap<>();

    @Override
    public boolean send(int connectionId, T msg) {
        ConnectionHandler<T> handler = connectionHandlers.get(connectionId);
        if (handler != null) {
            handler.send(msg);
            return true;
        }
        return false;
    }

    @Override
    public void send(String channel, T msg) {
        List<Integer> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            for (int connectionId : subscribers) {
                send(connectionId, msg);
            }
        }
    }

    @Override
    public void disconnect(int connectionId) {
        connectionHandlers.remove(connectionId);
        // Remove the client from all channels they are subscribed to
        channelSubscriptions.forEach((channel, subscribers) -> {
            subscribers.remove((Integer) connectionId);
        });
    }

    public void addConnection(int connectionId, ConnectionHandler<T> handler) {
        connectionHandlers.put(connectionId, handler);
    }

    public void subscribe(int connectionId, String channel) {
        channelSubscriptions.putIfAbsent(channel, new ArrayList<>());
        List<Integer> subscribers = channelSubscriptions.get(channel);
        synchronized (subscribers) {
            if (!subscribers.contains(connectionId)) {
                subscribers.add(connectionId);
            }
        }
    }

    public void unsubscribe(int connectionId, String channel) {
        List<Integer> subscribers = channelSubscriptions.get(channel);
        if (subscribers != null) {
            synchronized (subscribers) {
                subscribers.remove((Integer) connectionId);
                if (subscribers.isEmpty()) {
                    channelSubscriptions.remove(channel);
                }
            }
        }
    }
}
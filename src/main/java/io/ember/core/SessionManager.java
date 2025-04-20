package io.ember.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    public Map<String, Object> session(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    public void setSessionAttribute(String sessionId, String key, Object value) {
        session(sessionId).put(key, value);
    }

    public Object sessionAttribute(String sessionId, String key) {
        return session(sessionId).get(key);
    }

    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
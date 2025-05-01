package io.github.renatompf.ember.core;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The `SessionManager` class is responsible for managing user sessions.
 * It provides methods to create, retrieve, update, and invalidate sessions.
 * Each session is identified by a unique session ID and stores attributes as key-value pairs.
 */
public class SessionManager {

    // A thread-safe map to store sessions, where each session is a map of attributes.
    private static final Map<String, Map<String, Object>> sessions = new ConcurrentHashMap<>();

    /**
     * Retrieves the session associated with the given session ID.
     * If the session does not exist, a new one is created.
     *
     * @param sessionId The unique identifier for the session.
     * @return A map representing the session attributes.
     */
    public Map<String, Object> session(String sessionId) {
        return sessions.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
    }

    /**
     * Sets an attribute in the session identified by the given session ID.
     * If the session does not exist, it is created.
     *
     * @param sessionId The unique identifier for the session.
     * @param key       The key of the attribute to set.
     * @param value     The value of the attribute to set.
     */
    public void setSessionAttribute(String sessionId, String key, Object value) {
        session(sessionId).put(key, value);
    }

    /**
     * Retrieves the value of an attribute from the session identified by the given session ID.
     *
     * @param sessionId The unique identifier for the session.
     * @param key       The key of the attribute to retrieve.
     * @return The value of the attribute, or `null` if the attribute does not exist.
     */
    public Object sessionAttribute(String sessionId, String key) {
        return session(sessionId).get(key);
    }

    /**
     * Invalidates the session identified by the given session ID.
     * This removes all attributes associated with the session.
     *
     * @param sessionId The unique identifier for the session.
     */
    public void invalidateSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
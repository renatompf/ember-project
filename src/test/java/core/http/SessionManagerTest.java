package core.http;

import io.github.renatompf.ember.core.http.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class SessionManagerTest {

    private SessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new SessionManager();
    }

    @Test
    void session_WhenNewSessionId_ShouldCreateNewSession() {
        // Act
        Map<String, Object> session = sessionManager.session("test-session-1");

        // Assert
        assertNotNull(session);
        assertTrue(session.isEmpty());
    }

    @Test
    void session_WhenExistingSessionId_ShouldReturnExistingSession() {
        // Arrange
        Map<String, Object> firstSession = sessionManager.session("test-session-2");
        firstSession.put("test-key", "test-value");

        // Act
        Map<String, Object> secondSession = sessionManager.session("test-session-2");

        // Assert
        assertSame(firstSession, secondSession);
        assertEquals("test-value", secondSession.get("test-key"));
    }

    @Test
    void setSessionAttribute_ShouldStoreAttributeInSession() {
        // Act
        sessionManager.setSessionAttribute("test-session-3", "test-key", "test-value");

        // Assert
        Map<String, Object> session = sessionManager.session("test-session-3");
        assertEquals("test-value", session.get("test-key"));
    }

    @Test
    void sessionAttribute_WhenAttributeExists_ShouldReturnValue() {
        // Arrange
        sessionManager.setSessionAttribute("test-session-4", "test-key", "test-value");

        // Act
        Object value = sessionManager.sessionAttribute("test-session-4", "test-key");

        // Assert
        assertEquals("test-value", value);
    }

    @Test
    void sessionAttribute_WhenAttributeDoesNotExist_ShouldReturnNull() {
        // Act
        Object value = sessionManager.sessionAttribute("test-session-5", "non-existent");

        // Assert
        assertNull(value);
    }

    @Test
    void invalidateSession_ShouldRemoveSession() {
        // Arrange
        sessionManager.setSessionAttribute("test-session-6", "test-key", "test-value");

        // Act
        sessionManager.invalidateSession("test-session-6");

        // Assert
        Map<String, Object> newSession = sessionManager.session("test-session-6");
        assertTrue(newSession.isEmpty());
    }

    @Test
    void multipleSessionManagers_ShouldShareSessions() {
        // Arrange
        SessionManager otherManager = new SessionManager();
        sessionManager.setSessionAttribute("test-session-7", "test-key", "test-value");

        // Act
        Object value = otherManager.sessionAttribute("test-session-7", "test-key");

        // Assert
        assertEquals("test-value", value);
    }

    @Test
    void concurrentAccess_ShouldHandleMultipleThreads() throws InterruptedException {
        // Arrange
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // Act
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    sessionManager.setSessionAttribute("concurrent-session", "key-" + index, "value-" + index);
                    endLatch.countDown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        startLatch.countDown();
        endLatch.await();
        executor.shutdown();

        // Assert
        Map<String, Object> session = sessionManager.session("concurrent-session");
        assertEquals(threadCount, session.size());
        for (int i = 0; i < threadCount; i++) {
            assertEquals("value-" + i, session.get("key-" + i));
        }
    }
}
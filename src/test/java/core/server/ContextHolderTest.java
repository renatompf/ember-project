package core.server;

import io.github.renatompf.ember.core.server.Context;
import io.github.renatompf.ember.core.server.ContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ContextHolderTest {

    @Mock
    private Context mockContext;

    @Mock
    private Context anotherMockContext;

    @AfterEach
    void tearDown() {
        // Clear context after each test to avoid interference
        ContextHolder.clearContext();
    }

    @Test
    void setContext_ShouldStoreContext() {
        // Act
        ContextHolder.setContext(mockContext);
        Context retrievedContext = ContextHolder.context();

        // Assert
        assertEquals(mockContext, retrievedContext);
    }

    @Test
    void context_WhenNoContextSet_ShouldReturnNull() {
        // Act
        Context retrievedContext = ContextHolder.context();

        // Assert
        assertNull(retrievedContext);
    }

    @Test
    void clearContext_ShouldRemoveStoredContext() {
        // Arrange
        ContextHolder.setContext(mockContext);

        // Act
        ContextHolder.clearContext();
        Context retrievedContext = ContextHolder.context();

        // Assert
        assertNull(retrievedContext);
    }

    @Test
    void multipleSetContext_ShouldOverwritePreviousContext() {
        // Arrange
        ContextHolder.setContext(mockContext);

        // Act
        ContextHolder.setContext(anotherMockContext);
        Context retrievedContext = ContextHolder.context();

        // Assert
        assertEquals(anotherMockContext, retrievedContext);
    }

    @Test
    void threadIsolation_ContextsShouldBeThreadLocal() throws InterruptedException {
        // Arrange
        ContextHolder.setContext(mockContext);
        
        AtomicReference<Context> threadContext = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        
        // Act
        Thread thread = new Thread(() -> {
            // This should be null since it's a different thread
            threadContext.set(ContextHolder.context());
            
            // Set a different context in this thread
            ContextHolder.setContext(anotherMockContext);
            
            latch.countDown();
        });
        
        thread.start();
        latch.await();
        
        // Assert
        assertNull(threadContext.get(), "Context should not be shared between threads");
        assertEquals(mockContext, ContextHolder.context(), 
                "Original thread's context should remain unchanged");
    }

    @Test
    void constructor_ShouldBeCreatable() {
        // Act
        ContextHolder contextHolder = new ContextHolder();
        
        // Assert
        assertNotNull(contextHolder);
    }
}
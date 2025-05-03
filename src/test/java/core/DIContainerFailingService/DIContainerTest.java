package core.DIContainerFailingService;

import io.github.renatompf.ember.annotations.service.Service;
import io.github.renatompf.ember.core.DIContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DIContainerTest {

    @Test
    void resolve_ShouldThrowException_WhenServiceHasFinalFieldsButNoPublicConstructor() {
        // Given
        DIContainer container = new DIContainer("core.DIContainer2");
        container.register(ServiceWithFinalField.class);

        // When/Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> container.resolve(ServiceWithFinalField.class));
        assertTrue(exception.getMessage().contains("has fields requiring injection but no public constructor"));
    }

    @Service
    public static class ServiceWithFinalField {
        private final String requiredField;

        private ServiceWithFinalField() {
            this.requiredField = "";
        }
    }
}

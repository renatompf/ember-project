import io.ember.annotations.service.Service;
import io.ember.core.DIContainer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DIContainerTest {

    @Service
    public static class TestService {

        public TestService() {
        }
    }

    @Service
    public static class AnotherTestService {
        public AnotherTestService() {

        }
    }

    @Test
    public void testServiceRegistration() {
        DIContainer container = new DIContainer();

        // Register services
        container.registerServices();

        // Verify that the services are registered
        assertNotNull(container.resolve(TestService.class),"TestService should be registered and resolved.");
        assertNotNull(container.resolve(AnotherTestService.class), "AnotherTestService should be registered and resolved.");
    }

}

import io.ember.annotations.service.Service;
import io.ember.core.DIContainer;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

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
        assertNotNull("TestService should be registered and resolved.", container.resolve(TestService.class));
        assertNotNull("AnotherTestService should be registered and resolved.", container.resolve(AnotherTestService.class));
    }

}

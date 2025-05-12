package core.di;

import core.di.mock.*;
import io.github.renatompf.ember.core.di.ComponentRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ComponentRegistryTest {

    private ComponentRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ComponentRegistry();
    }

    @Test
    void testRegister_ValidAnnotatedClass() {
        assertDoesNotThrow(() -> registry.register(SimpleService.class));
        assertTrue(registry.isRegistered(SimpleService.class));
    }

    @Test
    void testRegister_InvalidClassThrowsException() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> registry.register(UnannotatedService.class));
        assertTrue(ex.getMessage().contains("not properly annotated"));
    }

    @Test
    void testRegisterAll_BatchRegisterSuccess() {
        List<Class<?>> classes = List.of(SimpleService.class, DependentService.class);
        assertDoesNotThrow(() -> registry.registerAll(classes));
        assertTrue(registry.isRegistered(SimpleService.class));
        assertTrue(registry.isRegistered(DependentService.class));
    }

    @Test
    void testResolve_NoDependencyService() {
        registry.register(SimpleService.class);
        SimpleService service = registry.resolve(SimpleService.class);
        assertNotNull(service);
        assertInstanceOf(SimpleService.class, service);
    }

    @Test
    void testResolve_ServiceWithDependency() {
        registry.register(SimpleService.class);
        registry.register(DependentService.class);
        DependentService service = registry.resolve(DependentService.class);
        assertNotNull(service);
        assertNotNull(service.getSimpleService());
    }

    @Test
    void testResolve_ServiceNotRegisteredThrows() {
        Exception ex = assertThrows(IllegalStateException.class, () -> registry.resolve(SimpleService.class));
        assertTrue(ex.getMessage().contains("Service not registered"));
    }

    @Test
    void testResolve_ServiceWithNoPublicConstructorFails() {
        registry.register(NoPublicConstructorService.class);
        Exception ex = assertThrows(RuntimeException.class, () -> registry.resolve(NoPublicConstructorService.class));
        assertTrue(ex.getMessage().contains("Failed to resolve service"));
    }

    @Test
    void testResolveAll_ResolvesAllRegisteredServices() {
        registry.register(SimpleService.class);
        registry.register(DependentService.class);
        assertDoesNotThrow(() -> registry.resolveAll());

        assertNotNull(registry.getInstance(SimpleService.class));
        assertNotNull(registry.getInstance(DependentService.class));
    }

    @Test
    void testGetAllInstances_ReturnsCorrectSize() {
        registry.register(SimpleService.class);
        registry.resolve(SimpleService.class);
        assertEquals(1, registry.getAllInstances().size());
    }

    @Test
    void testGetInstance_ReturnsNullIfNotResolved() {
        registry.register(SimpleService.class);
        assertNull(registry.getInstance(SimpleService.class));
    }

    @Test
    void testResolve_NoPublicConstructorWithFinalFields_ThrowsException() {
        ComponentRegistry registry = new ComponentRegistry();
        registry.register(ServiceWithPrivateConstructorAndFinalFields.class);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> registry.resolve(ServiceWithPrivateConstructorAndFinalFields.class)
        );

        Throwable cause = exception.getCause();
        assertInstanceOf(IllegalStateException.class, cause);
        assertTrue(cause.getMessage().contains("has fields requiring injection but no public constructor"));
    }

    @Test
    void testResolve_NoPublicConstructorWithNoFinalFields_CreatedSuccessfully() {
        ComponentRegistry registry = new ComponentRegistry();
        registry.register(ServiceWithPrivateConstructorAndNoFields.class);
        registry.resolve(ServiceWithPrivateConstructorAndNoFields.class);

        assertNotNull(registry.getInstance(ServiceWithPrivateConstructorAndNoFields.class));
    }

}
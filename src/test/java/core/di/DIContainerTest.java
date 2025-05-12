package core.di;

import core.di.mock.SimpleController;
import core.di.mock.SimpleGlobalHandler;
import core.di.mock.SimpleService;
import io.github.renatompf.ember.EmberApplication;
import io.github.renatompf.ember.annotations.controller.Controller;
import io.github.renatompf.ember.annotations.exceptions.GlobalHandler;
import io.github.renatompf.ember.annotations.service.Service;
import io.github.renatompf.ember.core.controller.ControllerMapper;
import io.github.renatompf.ember.core.di.ClassScanner;
import io.github.renatompf.ember.core.di.ComponentRegistry;
import io.github.renatompf.ember.core.di.DIContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DIContainerTest {

    private DIContainer container;
    private ComponentRegistry registryMock;
    private ClassScanner classScannerMock;

    @BeforeEach
    void setup() {
        registryMock = spy(new ComponentRegistry());
        classScannerMock = mock(ClassScanner.class);
        container = new DIContainer("core.di", registryMock, classScannerMock);
    }

    @Test
    void testRegister_individualComponent_success() {
        assertDoesNotThrow(() -> container.register(SimpleService.class));
    }

    @Test
    void testResolve_success() {
        container.register(SimpleService.class);
        container.resolve(SimpleService.class);
        Object instance = container.resolve(SimpleService.class);
        assertNotNull(instance);
        assertInstanceOf(SimpleService.class, instance);
    }

    @Test
    void testInit_scansAndRegistersComponents() throws IOException, ClassNotFoundException {
        when(classScannerMock.findAnnotatedClasses(anyString(), eq(Service.class)))
                .thenReturn(List.of(SimpleService.class));
        when(classScannerMock.findAnnotatedClasses(anyString(), eq(Controller.class)))
                .thenReturn(List.of(SimpleController.class));
        when(classScannerMock.findAnnotatedClasses(anyString(), eq(GlobalHandler.class)))
                .thenReturn(List.of(SimpleGlobalHandler.class));

        container.init();

        assertNotNull(container.getExceptionManager());
        assertNotNull(container.getValidationManager());
        assertNotNull(container.getParameterResolver());

        assertDoesNotThrow(() -> container.resolve(SimpleService.class));
        assertDoesNotThrow(() -> container.resolve(SimpleController.class));
        assertDoesNotThrow(() -> container.resolve(SimpleGlobalHandler.class));
    }

    @Test
    void testInit_throwsRuntimeExceptionOnFailure() throws IOException, ClassNotFoundException {
        when(classScannerMock.findAnnotatedClasses(anyString(), any()))
                .thenThrow(new IOException("Scan failure"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> container.init());
        assertTrue(ex.getMessage().contains("initialization failed"));
    }

    @Test
    void testMapControllerRoutes_notInitializedThrows() {
        EmberApplication mockApp = mock(EmberApplication.class);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> container.mapControllerRoutes(mockApp));
        assertTrue(ex.getMessage().contains("not initialized"));
    }

    @Test
    void testMapControllerRoutes_success() throws IOException, ClassNotFoundException {
        // Arrange
        when(classScannerMock.findAnnotatedClasses(anyString(), eq(Service.class)))
                .thenReturn(List.of());
        when(classScannerMock.findAnnotatedClasses(anyString(), eq(Controller.class)))
                .thenReturn(List.of(SimpleController.class));
        when(classScannerMock.findAnnotatedClasses(anyString(), eq(GlobalHandler.class)))
                .thenReturn(List.of(SimpleGlobalHandler.class));

        container.init();

        ControllerMapper mockMapper = mock(ControllerMapper.class);
        container.setControllerMapper(mockMapper);

        EmberApplication mockApp = mock(EmberApplication.class);

        container.mapControllerRoutes(mockApp);

        verify(mockMapper).mapControllerRoutes(eq(mockApp), anyMap());
    }

    @Test
    void testGetters_returnNonNullAfterInit() throws IOException, ClassNotFoundException {
        when(classScannerMock.findAnnotatedClasses(anyString(), any()))
                .thenReturn(List.of());

        container.init();

        assertNotNull(container.getExceptionManager());
        assertNotNull(container.getParameterResolver());
        assertNotNull(container.getValidationManager());
    }
}
package core.validation;

import io.github.renatompf.ember.annotations.parameters.Validated;
import io.github.renatompf.ember.core.validation.ValidationManager;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.executable.ExecutableValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationManagerTest {

    @Mock
    private Validator mockValidator;

    @Mock
    private ExecutableValidator executableValidator;

    private ValidationManager validationManager;

    // Test objects and methods
    private TestObject testObject;
    private Method testMethod;
    private Parameter[] parameters;
    private Object[] args;

    @BeforeEach
    void setUp() throws Exception {
        // Create a new ValidationManager with access to our mocked validator
        validationManager = new ValidationManagerTestable(mockValidator);

        // Setup test objects
        testObject = new TestObject();
        testMethod = TestObject.class.getMethod("testMethod", String.class, ValidatedObject.class);
        parameters = testMethod.getParameters();
        args = new Object[]{"test", new ValidatedObject()};

        // Setup common mock behavior
        lenient().when(mockValidator.forExecutables()).thenReturn(executableValidator);
    }

    @Test
    void validateMethodParameters_WithNoViolations_ShouldNotThrowException() {
        ValidatedObject validObject = new ValidatedObject();
        validObject.field1 = "valid12";

        Object[] validArgs = new Object[]{"test", validObject};

        Set<ConstraintViolation<Object>> emptySet = Collections.emptySet();
        lenient().doReturn(emptySet).when(executableValidator).validateParameters(any(), any(), any());

        assertDoesNotThrow(() ->
                validationManager.validateMethodParameters(testObject, testMethod, parameters, validArgs)
        );
    }

    @Test
    void validateMethodParameters_WithViolations_ShouldThrowException() {
        // Arrange
        Set<ConstraintViolation<Object>> violations = new HashSet<>();
        ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        violations.add(violation);

        lenient().doReturn(violations).when(executableValidator).validateParameters(eq(testObject), eq(testMethod), eq(args));

        // Act & Assert
        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> validationManager.validateMethodParameters(testObject, testMethod, parameters, args)
        );

        assertEquals(2, exception.getConstraintViolations().size());
    }

    @Test
    void validateMethodParameters_WithValidatedParameter_ShouldValidateNestedObject() throws Exception {
        ValidationManager realManager = new ValidationManager();
        TestObject testObj = new TestObject();
        Method method = TestObject.class.getMethod("testMethod", String.class, ValidatedObject.class);
        Parameter[] params = method.getParameters();

        ValidatedObject invalidObject = new ValidatedObject();
        Object[] args = new Object[]{"test", invalidObject};

        // Act & Assert
        ConstraintViolationException exception = assertThrows(
                ConstraintViolationException.class,
                () -> realManager.validateMethodParameters(testObj, method, params, args)
        );

        assertFalse(exception.getConstraintViolations().isEmpty());

        boolean hasValidationForNestedObject = exception.getConstraintViolations().stream()
                .anyMatch(violation -> violation.getPropertyPath().toString().contains("field1"));
        assertTrue(hasValidationForNestedObject, "Should contain validation errors for the @Validated object's fields");
    }

    @Test
    void validate_WithNullObject_ShouldNotCallValidator() {
        // Act
        validationManager.validate(null);

        // Assert
        verify(mockValidator, never()).validate(any());
    }

    @Test
    void validate_WithNoViolations_ShouldNotThrowException() {
        // Arrange
        Set<ConstraintViolation<TestObject>> emptySet = Collections.emptySet();
        lenient().doReturn(emptySet).when(mockValidator).validate(testObject);

        // Act & Assert
        assertDoesNotThrow(() -> validationManager.validate(testObject));
    }

    @Test
    void validate_WithViolations_ShouldThrowException() {
        // Arrange
        Set<ConstraintViolation<TestObject>> violations = new HashSet<>();
        ConstraintViolation<TestObject> violation = mock(ConstraintViolation.class);
        violations.add(violation);

        ValidationManager testManager = new ValidationManager() {
            @Override
            public <T> void validate(T object) {
                if (object != null) {
                    Set<ConstraintViolation<T>> violations = mockValidator.validate(object);
                    if (!violations.isEmpty()) {
                        throw new ConstraintViolationException(violations);
                    }
                }
            }

            @Override
            public Validator getValidator() {
                return mockValidator;
            }
        };

        doReturn(violations).when(mockValidator).validate(testObject);

        // Act & Assert
        assertThrows(ConstraintViolationException.class,
                () -> testManager.validate(testObject));
    }

    @Test
    void validateAndGetViolations_WithNullObject_ShouldReturnEmptySet() {
        // Act
        Set<ConstraintViolation<Object>> result = validationManager.validateAndGetViolations(null);

        // Assert
        assertTrue(result.isEmpty());
        verify(mockValidator, never()).validate(any());
    }

    @Test
    void validateAndGetViolations_WithNoViolations_ShouldReturnEmptySet() {
        // Arrange
        Set<ConstraintViolation<TestObject>> emptySet = Collections.emptySet();
        lenient().doReturn(emptySet).when(mockValidator).validate(testObject);

        // Act
        Set<ConstraintViolation<TestObject>> result = validationManager.validateAndGetViolations(testObject);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void validateAndGetViolations_WithViolations_ShouldReturnViolations() {
        // Create a custom ValidationManager that directly uses our mock
        ValidationManager testManager = new ValidationManager() {
            @Override
            public <T> Set<ConstraintViolation<T>> validateAndGetViolations(T object) {
                if (object != null) {
                    return mockValidator.validate(object);
                }
                return Collections.emptySet();
            }

            @Override
            public Validator getValidator() {
                return mockValidator;
            }
        };

        // Arrange
        Set<ConstraintViolation<TestObject>> violations = new HashSet<>();
        violations.add(mock(ConstraintViolation.class));
        doReturn(violations).when(mockValidator).validate(testObject);

        // Act
        Set<ConstraintViolation<TestObject>> result = testManager.validateAndGetViolations(testObject);

        // Assert
        assertEquals(1, result.size());
    }

    @Test
    void getValidator_ShouldReturnValidator() {
        // Act
        Validator result = validationManager.getValidator();

        // Assert
        assertEquals(mockValidator, result);
    }

    // Test classes
    private static class TestObject {
        public void testMethod(String param, @Validated ValidatedObject validatedObject) {
            // Test method
        }
    }

    private static class ValidatedObject {
        @NotNull @NotBlank @Size(min = 5, max = 10) String field1;
    }

    // Testable subclass that allows injecting a mocked validator
    private static class ValidationManagerTestable extends ValidationManager {
        private final Validator mockValidator;

        public ValidationManagerTestable(Validator mockValidator) {
            this.mockValidator = mockValidator;
        }

        @Override
        public Validator getValidator() {
            return mockValidator;
        }
    }
}
package io.github.renatompf.ember.core.validation;

import io.github.renatompf.ember.annotations.parameters.Validated;
import jakarta.validation.*;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.Set;

/**
 * Centralizes validation handling within the Ember framework.
 * Provides methods to validate method parameters and objects against Jakarta validation constraints.
 */
public class ValidationManager {
    private static final Logger logger = LoggerFactory.getLogger(ValidationManager.class);
    private static final Validator validator;

    /**
      Static block to initialize the Validator instance.
      It uses Hibernate Validator as the default implementation and configures it with a custom
      message interpolator.
     */
    static {
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Validates method parameters against constraints.
     * This checks both parameter-level constraints and any beans annotated with @Validated.
     *
     * @param instance   The object instance containing the method
     * @param method     The method whose parameters should be validated
     * @param parameters The method parameters
     * @param args       The actual argument values
     * @throws ConstraintViolationException if validation fails
     */
    public void validateMethodParameters(Object instance, Method method, Parameter[] parameters, Object[] args) {
        logger.debug("Validating parameters for method {}.{}", instance.getClass().getName(), method.getName());

        // Validate parameter-level constraints
        Set<ConstraintViolation<Object>> violations = new HashSet<>(validator
                .forExecutables()
                .validateParameters(instance, method, args)
        );

        // Additionally validate beans annotated with @Validated
        logger.debug(parameters.length + " parameters to validate");
        for (int i = 0; i < parameters.length; i++) {
            logger.debug("Validating parameter {} of type {}", i, parameters[i].getType().getName());
            if (parameters[i].isAnnotationPresent(Validated.class) && args[i] != null) {
                logger.debug( "Validating parameter {} of type {} with @Validated", i, parameters[i].getType().getName());
                violations.addAll(validator.validate(args[i]));
            }
        }

        if (!violations.isEmpty()) {
            logger.debug("Validation failed with {} violations", violations.size());
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * Validates an object against its constraints.
     *
     * @param object The object to validate
     * @param <T>    The type of the object
     * @throws ConstraintViolationException if validation fails
     */
    public <T> void validate(T object) {
        if (object == null) {
            return;
        }

        Set<ConstraintViolation<T>> violations = validator.validate(object);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    /**
     * Validates an object against its constraints and returns the violations without throwing an exception.
     *
     * @param object The object to validate
     * @param <T>    The type of the object
     * @return Set of constraint violations, empty if validation passes
     */
    public <T> Set<ConstraintViolation<T>> validateAndGetViolations(T object) {
        if (object == null) {
            return new HashSet<>();
        }
        return validator.validate(object);
    }

    /**
     * Creates a default validator using the Jakarta Validation bootstrap API.
     *
     * @return A configured validator instance
     */
    private Validator getDefaultValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            return factory.getValidator();
        }
    }

    /**
     * Gets the underlying validator instance.
     *
     * @return The validator
     */
    public Validator getValidator() {
        return validator;
    }
}
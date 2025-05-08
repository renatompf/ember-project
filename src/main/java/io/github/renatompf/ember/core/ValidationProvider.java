package io.github.renatompf.ember.core;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;

/**
 * ValidationProvider is a utility class that provides a singleton instance of a Validator.
 * It uses Hibernate Validator as the default implementation and configures it with a custom
 * message interpolator.
 * <p>
 * This class is used to perform validation on parameters annotated with @Validated.
 */
public class ValidationProvider {
    /**
     * The singleton instance of the Validator.
     */
    private static final Validator validator;

    /**
     * Static block to initialize the Validator instance.
     * It uses Hibernate Validator as the default implementation and configures it with a custom
     * message interpolator.
     */
    static {
        ValidatorFactory factory = Validation.byDefaultProvider()
                .configure()
                .messageInterpolator(new ParameterMessageInterpolator())
                .buildValidatorFactory();
        validator = factory.getValidator();
    }

    /**
     * Returns the singleton instance of the Validator.
     * The Validator is configured with a custom message interpolator
     * using Hibernate Validator as the default implementation.
     *
     * @return The singleton Validator instance.
     */
    public static Validator getValidator() {
        return validator;
    }
}
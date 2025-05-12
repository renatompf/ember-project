package io.github.renatompf.ember.core.http;

/**
 * Interface for serializing objects into a specific format.
 * <p>
 * This interface defines a method for serializing an object into a string representation.
 * Implementations of this interface can provide different serialization formats (e.g., JSON, XML).
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * {@code
 * ResponseSerializer jsonSerializer = new JsonResponseSerializer();
 * String jsonString = jsonSerializer.serialize(myObject);
 * }
 * </pre>
 */
@FunctionalInterface
public interface ResponseSerializer {

    /**
     * Serializes the given object into a string representation.
     *
     * @param obj The object to serialize.
     * @return The serialized string representation of the object.
     * @throws Exception if serialization fails.
     */
    String serialize(Object obj) throws Exception;
}

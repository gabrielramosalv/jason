package jason.mind.api.core.application.history;

/**
 * Exception thrown when the parameters of a query are invalid.
 *
 * @author Gabriel Lima
 */
public class InvalidQueryException extends RuntimeException {

    /**
     * Creates the exception with the description of the invalid parameter.
     *
     * @param message description of what is invalid in the query
     */
    public InvalidQueryException(String message) {
        super(message);
    }
}

package mfix.core.stats.element;

public class ElementException extends Exception {
    static final String ERROR_MESSAGE = "%s is null!";

    protected ElementException(String message) {
        super(ERROR_MESSAGE.format(message));
    }
}

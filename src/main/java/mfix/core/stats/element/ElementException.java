package mfix.core.stats.element;

/**
 * @author: Luyao Ren
 * @date: 2018/12/20
 */
public class ElementException extends Exception {
    static final String ERROR_MESSAGE = "%s is null!";

    protected ElementException(String message) { super(message); }
}

package net.hulte.jmysqld;

/**
 * An exception caused by the MySQL process not behaving as expected.
 */
public final class MySqlProcessException extends RuntimeException {

    public MySqlProcessException(String message) {
        super(message);
    }

    public MySqlProcessException(String message, Throwable cause) {
        super(message, cause);
    }
}


package br.com.tresvtintas.mobile.core.security;

/**
 * Indicates that protected local session state could not be read or written safely.
 */
public final class SessionStorageException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SessionStorageException(String message) {
        super(message);
    }

    public SessionStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}

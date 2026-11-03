package co.za.unicard.exception;

/** The signed-in user may not act on this resource (e.g. someone else's child). */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}

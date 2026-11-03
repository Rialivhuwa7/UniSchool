package co.za.unicard.exception;

/** The request clashes with existing data (duplicate, already sold, already paid...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}

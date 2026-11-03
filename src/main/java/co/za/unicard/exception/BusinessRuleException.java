package co.za.unicard.exception;

/** A valid request that breaks a rule, e.g. not enough money in a wallet. Maps to 422. */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}

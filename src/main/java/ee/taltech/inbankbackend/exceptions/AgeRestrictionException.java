package ee.taltech.inbankbackend.exceptions;

public class AgeRestrictionException extends Exception {

    public AgeRestrictionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AgeRestrictionException(String message) {
        super(message);
    }
}
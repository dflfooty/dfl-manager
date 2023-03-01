package net.dflmngr.exceptions;

public class UnexpectedHtmlException extends RuntimeException {

    private static final String MSG = "HTML has not loaded as expected";

    public UnexpectedHtmlException() {
        super(MSG);
    }
    
}

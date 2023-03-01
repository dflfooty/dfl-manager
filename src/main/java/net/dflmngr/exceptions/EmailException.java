package net.dflmngr.exceptions;

public class EmailException extends RuntimeException {

    private static final String MSG = "Error sending email";

    public EmailException() {
        super(MSG);
    }
    
}

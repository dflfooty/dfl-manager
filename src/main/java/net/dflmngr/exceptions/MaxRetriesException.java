package net.dflmngr.exceptions;

public class MaxRetriesException extends RuntimeException {

    private static final String MSG = "Max retries hit; retries=";

    public MaxRetriesException(int retries) {
        super(MSG + retries);
    }
    
}

package net.dflmngr.exceptions;

public class MissingNonStandardLockoutException extends RuntimeException {

    private static final String MSG = "Non standard lockout time missing from Global table";

    public MissingNonStandardLockoutException() {
        super(MSG);
    }
    
}

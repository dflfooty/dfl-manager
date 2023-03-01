package net.dflmngr.exceptions;

public class UnknownSelectionIndicatorException extends RuntimeException {

    private static final String MSG = "Selection Indicator is not known; Position=";

    public UnknownSelectionIndicatorException(String indicator) {
        super(MSG + indicator);
    }
    
}

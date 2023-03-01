package net.dflmngr.exceptions;

public class UnknownPositionException extends RuntimeException {

    private static final String MSG = "Player postion is not known; Position=";

    public UnknownPositionException(String position) {
        super(MSG + position);
    }
    
}

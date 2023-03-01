package net.dflmngr.exceptions;

public class ProxyUrlException extends RuntimeException {

    private static final String MSG = "Proxy URL error";

    public ProxyUrlException() {
        super(MSG);
    }
    
}

package net.dflmngr.exceptions;

public class HtmlPageLoadException extends RuntimeException {

    private static final String MSG = "Error Loading page, URL: ";

    public HtmlPageLoadException(String url) {
        super(MSG + url);
    }
    
}

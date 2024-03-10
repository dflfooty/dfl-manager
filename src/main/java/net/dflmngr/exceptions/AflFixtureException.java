package net.dflmngr.exceptions;

public class AflFixtureException extends RuntimeException  {
    private static final String MSG1 = "Error AflFixture page, URL: %s, Element: %s";
    private static final String MSG2 = "Error AflFixture page, URL: %s";

    public AflFixtureException(String url, String elementClass) {
        super(String.format(MSG1, url, elementClass));
    }

    public AflFixtureException(String url) {
        super(String.format(MSG2, url));
    }

    public AflFixtureException(String url, Exception cause) {
        super(String.format(MSG2, url), cause);
    }
}

package net.dflmngr.exceptions;

public class MissingGlobalConfig extends RuntimeException {

    private static final String MSG = "Missing global config: ";

    public MissingGlobalConfig(String code, String groupCode) {
        super(MSG + "code=" + code + "; groupCode=" + groupCode);
    }
    
}

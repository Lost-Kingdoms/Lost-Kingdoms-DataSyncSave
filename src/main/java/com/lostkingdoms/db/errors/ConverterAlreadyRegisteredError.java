package com.lostkingdoms.db.errors;

public class ConverterAlreadyRegisteredError extends Error {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 442110202011522636L;

    /**
     * The reason for this error
     */
    private final Class<?> reason;

    public ConverterAlreadyRegisteredError(Class<?> reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "ConverterAlreadyRegisteredError: A converter for class " + reason.getSimpleName() + " was already registered!";
    }

}

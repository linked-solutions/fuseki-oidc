package com.vdanyliuk.jena.security;

public class AccessDeniedException extends RuntimeException {

    private static final long serialVersionUID = 2605607948156624590L;

    public AccessDeniedException(String message) {
        super(message);
    }

}

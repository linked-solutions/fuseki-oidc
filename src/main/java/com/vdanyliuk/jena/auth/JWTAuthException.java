package com.vdanyliuk.jena.auth;

import org.apache.shiro.authc.AuthenticationException;

class JWTAuthException extends AuthenticationException {

    private static final long serialVersionUID = -707427459569232323L;

    private JWTAuthException(String message) {
        super(message);
    }

    static JWTAuthException noAuthHeader() {
        return new JWTAuthException("Authorization header is not provided!");
    }

    static JWTAuthException brokenAuthHeader() {
        return new JWTAuthException("Authorization header is broken!");
    }
}

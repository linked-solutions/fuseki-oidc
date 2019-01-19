package com.vdanyliuk.jena.auth;

import lombok.Value;

@Value
class AuthError {

    private int code;

    private String message;
}

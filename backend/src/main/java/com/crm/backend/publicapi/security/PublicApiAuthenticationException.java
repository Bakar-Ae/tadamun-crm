package com.crm.backend.publicapi.security;

public class PublicApiAuthenticationException extends RuntimeException {

    public PublicApiAuthenticationException() {
        super("Invalid API key");
    }
}

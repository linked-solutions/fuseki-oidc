package com.vdanyliuk.jena.auth;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;

@Slf4j
public class JWTRealm extends AuthorizingRealm {

    private final String secretValue;

    public JWTRealm() {
        this(System.getenv("SECRET_VALUE"));
    }

    public JWTRealm(String secretValue) {
        this.secretValue = secretValue;
        if (secretValue == null || secretValue.trim().isEmpty()) {
            throw new IllegalStateException("SECRET_VALUE environment variable should be set");
        }
        System.out.println("Secret value: " + secretValue);
    }

    @Override
    public boolean supports(AuthenticationToken token) {
        System.out.println("Check if token supported: " + token);
        return token instanceof JWTAuthToken;
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
        return new SimpleAuthorizationInfo();
    }

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        try {
            JWTAuthToken jwtAuthToken = (JWTAuthToken) token;
            Algorithm algorithm = Algorithm.HMAC256(secretValue);
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer("fuseki-auth")
                    .build(); //Reusable verifier instance
            DecodedJWT jwt = verifier.verify(jwtAuthToken.getValue());
            String payload = jwt.getPayload();
            byte[] decode = Base64.getDecoder().decode(payload);
            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> principal = objectMapper.reader().forType(Map.class).readValue(decode);
            return new SimpleAuthenticationInfo(principal, jwtAuthToken.getValue(), "jwt");
        } catch (IOException e) {
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        }
    }

}

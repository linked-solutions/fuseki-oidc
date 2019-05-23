/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solutions.linked.jena.auth;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.PathConfigProcessor;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;
import org.keycloak.adapters.AdapterDeploymentContext;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.NodesRegistrationManagement;
import org.keycloak.adapters.servlet.FilterRequestAuthenticator;
import org.keycloak.adapters.servlet.OIDCFilterSessionStore;
import org.keycloak.adapters.servlet.OIDCServletHttpFacade;
import org.keycloak.adapters.spi.AuthChallenge;
import org.keycloak.adapters.spi.AuthOutcome;
import org.keycloak.adapters.spi.InMemorySessionIdMapper;
import org.keycloak.adapters.spi.SessionIdMapper;
import org.keycloak.representations.adapters.config.AdapterConfig;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Slf4j
public class KeycloakAuthenticationFilter extends AuthenticatingFilter implements PathConfigProcessor {

    private AdapterDeploymentContext deploymentContext;

    private SessionIdMapper idMapper = new InMemorySessionIdMapper();

    private NodesRegistrationManagement nodesRegistrationManagement;

    /**
     * Constructor that can be used to define a {@code KeycloakConfigResolver} that will be used at initialization to
     * provide the {@code KeycloakDeployment}.
     *
     */
    public KeycloakAuthenticationFilter() throws IOException {
        onFilterConfigSet();
    }

    @Override
    protected boolean isEnabled(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        return super.isEnabled(request, response) && isNotBasicAuth(request);
    }

    private boolean isNotBasicAuth(ServletRequest request) {
        String authHeader = ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
        return authHeader == null || !authHeader.trim().toLowerCase().startsWith("basic");
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        if (((HttpServletRequest) request).getMethod().equalsIgnoreCase("OPTIONS")) {
            ((HttpServletResponse)response).setHeader("Access-Control-Allow-Origin", ((javax.servlet.http.HttpServletRequest) request).getHeader("Origin"));
            ((HttpServletResponse)response).addHeader("Vary", "Origin");
            return true;
        } else {
            return super.isAccessAllowed(request, response, mappedValue);
        }
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        boolean loggedIn = executeLogin(request, response);
        return loggedIn || sendChallenge(response);
    }

    private boolean sendChallenge(ServletResponse response) {
        System.out.println("Authentication required: sending 401 Authentication challenge response.");

        HttpServletResponse httpResponse = WebUtils.toHttp(response);
        httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return false;
    }

    @Override
    protected AuthenticationToken createToken(ServletRequest req, ServletResponse res) throws Exception {
        System.out.println("Keycloak OIDC Filter");
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        OIDCServletHttpFacade facade = new OIDCServletHttpFacade(request, response);
        KeycloakDeployment deployment = deploymentContext.resolveDeployment(facade);
        if (deployment == null || !deployment.isConfigured()) {
            response.sendError(403);
            System.out.println("deployment not configured");
            throw new IllegalStateException("deployment not configured");
        }

        nodesRegistrationManagement.tryRegister(deployment);
        OIDCFilterSessionStore tokenStore = new OIDCFilterSessionStore(request, facade, 100000, deployment, idMapper);
        tokenStore.checkCurrentToken();


        FilterRequestAuthenticator authenticator = new FilterRequestAuthenticator(deployment, tokenStore, facade, request, 8443);
        AuthOutcome outcome = authenticator.authenticate();
        if (outcome == AuthOutcome.AUTHENTICATED) {
            System.out.println("AUTHENTICATED");
            return getAuthenticationToken(facade);
        }
        AuthChallenge challenge = authenticator.getChallenge();
        if (challenge != null) {
            System.out.println("challenge");
            challenge.challenge(facade);
            return null;
        }
        response.sendError(403);
        return null;
    }

    private AuthenticationToken getAuthenticationToken(OIDCServletHttpFacade facade) {
        String email = facade.getSecurityContext().getToken().getEmail();
        Objects.requireNonNull(email, "Token must contain email clause.");
        String token = facade.getSecurityContext().getTokenString();
        return new JWTAuthToken(token, email);
    }

    @Override
    public void onFilterConfigSet() throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("keycloak.json")){
            AdapterConfig adapterConfig = KeycloakDeploymentBuilder.loadAdapterConfig(is);
            String authServerUrl = System.getenv("AUTH_SERVER_URL");
            if (authServerUrl != null) {
                adapterConfig.setAuthServerUrl(authServerUrl);
            } else {
                System.out.println("WARNING: Environment variable AUTH_SERVER_URL not set.");
            }
            KeycloakDeployment kd = KeycloakDeploymentBuilder.build(adapterConfig);
            deploymentContext = new AdapterDeploymentContext(kd);
            System.out.println("Keycloak is using a per-deployment configuration.");

            nodesRegistrationManagement = new NodesRegistrationManagement();
        }
    }

}
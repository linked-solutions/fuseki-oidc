package solutions.linked.jena.auth;

import java.io.IOException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.PathConfigProcessor;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.apache.shiro.web.util.WebUtils;

@Slf4j
public class JWTAuthenticationFilter extends AuthenticatingFilter implements PathConfigProcessor {

    private final ObjectWriter objectWriter = new ObjectMapper().writer();

    @Override
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) {
        String authHeader = getAuthHeader((HttpServletRequest) request);
        if (authHeader == null) {
            System.out.println("No Authorization token");
            throw JWTAuthException.noAuthHeader();
        }
        if (!authHeader.startsWith("Bearer")) {
            System.out.println("Wrong Authorization token: " + authHeader);
            throw JWTAuthException.brokenAuthHeader();
        }
        String jwtContent = authHeader.substring(authHeader.indexOf(" ")).trim();
        System.out.println("Got auth token: " + jwtContent);
        return new JWTAuthToken(jwtContent, "user@mail.com");
    }

    private String getAuthHeader(HttpServletRequest request) {
        return request.getHeader("Authorization");
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
        return ((HttpServletRequest)request).getMethod().equalsIgnoreCase("OPTIONS") || super.isAccessAllowed(request, response, mappedValue);
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
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        cleanup(request, response, e);
        return false;
    }

    @Override
    protected void cleanup(ServletRequest request, ServletResponse response, Exception e) {
        if (e != null) {
            sendErrorResponse(response, e);
        }
    }

    private void sendErrorResponse(ServletResponse response, Exception e) {
        try {
            HttpServletResponse httpResponse = WebUtils.toHttp(response);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ServletOutputStream outputStream = httpResponse.getOutputStream();
            objectWriter.writeValue(outputStream, new AuthError(401, e.getMessage()));
        } catch (IOException e1) {
            System.out.println("Unable to write response: " + e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}

package solutions.linked.jena.auth;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpHeaders;
import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

public class JWTBypassingBasicHttpAuthenticationFilter extends BasicHttpAuthenticationFilter {

    @Override
    protected boolean isEnabled(ServletRequest request, ServletResponse response, String path, Object mappedValue) throws Exception {
        return super.isEnabled(request, response, path, mappedValue) && isNotBearerAuth(request);
    }

    private boolean isNotBearerAuth(ServletRequest request) {
        String authHeader = ((HttpServletRequest) request).getHeader(HttpHeaders.AUTHORIZATION);
        return authHeader == null || !authHeader.trim().toLowerCase().startsWith("bearer");
    }
}

package solutions.linked.jena.auth;

import lombok.Data;
import org.apache.shiro.authc.AuthenticationToken;

@Data
public class JWTAuthToken implements AuthenticationToken {

    private static final long serialVersionUID = -5267215013269126338L;

    private final String value;

    private final String email;

    JWTAuthToken(String value, String email) {
        this.value = value;
        this.email = email;
    }

    @Override
    public Object getPrincipal() {
        return email;
    }

    @Override
    public Object getCredentials() {
        return value;
    }
}

package solutions.linked.jena.security;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import com.jcabi.aspects.Cacheable;
import lombok.extern.slf4j.Slf4j;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Node_URI;
import org.apache.jena.graph.Triple;
import org.apache.jena.permissions.SecurityEvaluator;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.AuthenticationRequiredException;
import org.apache.jena.sparql.core.NamedGraph;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.AntPathMatcher;

@Slf4j
public class GraphSecurityEvaluator implements SecurityEvaluator {

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    private static final String OWN_GRAPH_PREFIX = "http://www.smartswissparticipation.com/graphs/users/";

    private static final String ONE_ACTION_QUERY =
            "prefix fo: <https://linked.solutions/fuseki-oidc/ontology#>  " +
            "prefix foaf:  <http://xmlns.com/foaf/0.1/> "+
            "prefix acl:  <http://www.w3.org/ns/auth/acl#> " +
                    "" +
                    "SELECT ?graph ?permission " +
                    "WHERE { " +
                    "     {"+
                    "        ?authorization fo:agentUserName ?username ." +
                    "     } UNION {"+
                    "        ?authorization acl:agentClass foaf:Agent . " +
                    "     }"+
                    "     ?authorization a acl:Authorization ;" + 
                    "           fo:accessTo ?graph ;" +
                    "           acl:mode  ?permission ." +
                    "} ";
    
    private static final String ONE_ACTION_QUERY_OLD =
            "prefix sec: <http://www.smartswissparticipation.com/security#> " +
                    "prefix users: <http://www.smartswissparticipation.com/users#> " +
                    "prefix graphs: <http://www.smartswissparticipation.com/graphs#> " +
                    "" +
                    "SELECT ?graph ?permission " +
                    "WHERE { " +
                    "        {?u users:username ?username ;" +
                    "            sec:graphAccess ?ga ." +
                    "        } UNION  " +
                    "        {<http://www.smartswissparticipation.com/users/**> sec:graphAccess ?ga .}" +
                    "        ?ga graphs:graph ?graph ;" +
                    "            sec:accessType ?permission" +
                    "} ";

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private Model securityModel;



    public GraphSecurityEvaluator(Model securityModel) {
        this.securityModel = securityModel;
    }



    @Override
    @Cacheable
    public boolean evaluate(Object o, Action action, Node graphIRI) throws AuthenticationRequiredException {
        return hasAccess((Subject) o, (Node_URI) graphIRI, action);
    }

    /**
     * This SecurityEvaluator decides only based on graph
     * so at the moment this method called user already permitted to do operation on graph
     * and this method just always returns true
     *
     * @return always true
     */
    @Override
    public boolean evaluate(Object principal, Action action, Node graphIRI, Triple triple) {
        return false;
    }


    @Override
    @Cacheable
    public boolean evaluate(Object o, Set<Action> set, Node graphIRI) throws AuthenticationRequiredException {
        return set.stream()
                .allMatch(action -> hasAccess((Subject) o, (Node_URI) graphIRI, action));
    }

    /**
     * This SecurityEvaluator decides only based on graph
     * so at the moment this method called user already permitted to do operation on graph
     * and this method just always returns true
     *
     * @return always true
     */
    @Override
    public boolean evaluate(Object o, Set<Action> set, Node graphIRI, Triple triple) throws AuthenticationRequiredException {
        return false;
    }

    @Override
    @Cacheable
    public boolean evaluateAny(Object o, Set<Action> set, Node graphIRI) throws AuthenticationRequiredException {
        return set.stream()
                .anyMatch(action -> hasAccess((Subject) o, (Node_URI) graphIRI, action));
    }

    /**
     * This SecurityEvaluator decides only based on graph
     * so at the moment this method called user already permitted to do operation on graph
     * and this method just always returns true
     *
     * @return always true
     */
    @Override
    public boolean evaluateAny(Object o, Set<Action> set, Node graphIRI, Triple triple) throws AuthenticationRequiredException {
        return false;
    }

    /**
     * This SecurityEvaluator decides only based on graph
     * so at the moment this method called user already permitted to do operation on graph
     * and this method just always returns true
     *
     * @return always true
     */
    @Override
    public boolean evaluateUpdate(Object o, Node graphIRI, Triple triple, Triple triple1) throws AuthenticationRequiredException {
        return true;
    }

    /**
     * Return the Shiro subject.  This is the subject that Shiro currently has logged in.
     */
    @Override
    public Object getPrincipal() {
        Subject subject = SecurityUtils.getSubject();
        log.debug("Principal: " + subject);
        return subject;
    }

    /**
     * Verify the Shiro subject is authenticated.
     */
    @Override
    public boolean isPrincipalAuthenticated(Object principal) {
        if (principal instanceof Subject) {
            log.debug("Principal authenticated");
            return ((Subject)principal).isAuthenticated();
        }
        log.debug("Principal NOT authenticated");
        return false;
    }

    private boolean hasAccess(Subject subject, Node_URI graphIRI, Action action) {
        String username = subject.getPrincipal().toString();
        if (isOwnGraph(username, graphIRI)) {
            log.debug("Principal: " + username + "\tAction: " + action + "\tNode:" + graphIRI + "\tAuthorized for own graph");
            return true;
        } else {
            boolean result = isSecurityGraph(graphIRI) ?
                    inOtherThread(() -> checkOtherGraphs(graphIRI, action, username)) :
                    checkOtherGraphs(graphIRI, action, username);
            log.debug("Principal: " + username + "\tAction: " + action + "\tNode:" + graphIRI + "\tAuthorized: " + result);
            return result;
        }
    }

    private boolean isSecurityGraph(Node_URI graphIRI) {
        return ((NamedGraph)securityModel.getGraph()).getGraphName().equals(graphIRI);
    }

    // This terrible hack is needed to bypass Jena transactions system
    // When write transaction is active for this graph
    // in the current thread any query returns empty result set
    // but it works from another thread.
    // This is the problem only for writes into security graph
    private boolean inOtherThread(Supplier<Boolean> checkOtherGraphs) {
        try {
            return executorService.submit(() -> securityModel.calculateInTxn(checkOtherGraphs)).get();
        } catch (InterruptedException e) {
            //ignore
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
        return false;
    }

    private boolean isOwnGraph(String username, Node_URI graphIRI) {
        String ownGraphURI = OWN_GRAPH_PREFIX + username;
        return ownGraphURI.equals(graphIRI.getURI());
    }

    @Cacheable
    private boolean checkOtherGraphs(Node_URI graphIRI, Action action, String username) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString(ONE_ACTION_QUERY);
        queryString.setLiteral("username", username);
        Query query = QueryFactory.create(queryString.toString());

        try(QueryExecution queryExecution = QueryExecutionFactory.create(query, securityModel)) {
            ResultSet resultSet = queryExecution.execSelect();
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.next();
                String graph = solution.get("graph").asLiteral().getString();
                String permission = solution.get("permission").asResource().getLocalName();
                if (grantsAccess(graph, permission, graphIRI, action)) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean grantsAccess(String graphPattern, String permission, Node_URI graphIRI, Action action) {
        return MATCHER.match(graphPattern, graphIRI.getURI()) &&
                permissionMatches(permission, action);
    }

    private boolean permissionMatches(String permission, Action action) {
        switch (permission.toUpperCase()) {
            case "READ" :
                return Action.Read.equals(action);
            case "CREATE" :
                return Action.Create.equals(action);
            case "DELETE" :
                return Action.Delete.equals(action);
            case "UPDATE" :
                return Action.Update.equals(action);
            case "WRITE" :
                return Action.Create.equals(action) ||
                    Action.Delete.equals(action) ||
                    Action.Update.equals(action);
            default: return false;
        }
    }


}

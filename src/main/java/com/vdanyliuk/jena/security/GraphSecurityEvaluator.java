package com.vdanyliuk.jena.security;

import java.util.Set;

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
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.AntPathMatcher;

@Slf4j
public class GraphSecurityEvaluator implements SecurityEvaluator {

    private static final String OWN_GRAPH_PREFIX = "http://www.smartparticipation.com/graphs/users/";

    private static final String ONE_ACTION_QUERY =
            "prefix sec: <http://www.smartparticipation.com/security#> " +
                    "prefix users: <http://www.smartparticipation.com/users#> " +
                    "prefix graphs: <http://www.smartparticipation.com/graphs#> " +
                    "" +
                    "SELECT ?graph ?permission " +
                    "WHERE { " +
                    "        ?u users:email ?email ." +
                    "        ?u sec:graphAccess ?ga .\n" +
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
        String email = subject.getPrincipal().toString();
        if (isOwnGraph(email, graphIRI)) {
            log.debug("Principal: " + email + "\tAction: " + action + "\tNode:" + graphIRI + "\tAuthorized for own graph");
            return true;
        } else {
            boolean result = checkOtherGraphs(graphIRI, action, email);
            log.debug("Principal: " + email + "\tAction: " + action + "\tNode:" + graphIRI + "\tAuthorized: " + result);
            return result;
        }
    }

    private boolean isOwnGraph(String email, Node_URI graphIRI) {
        String ownGraphURI = OWN_GRAPH_PREFIX + email;
        return ownGraphURI.equals(graphIRI.getURI());
    }

    private boolean checkOtherGraphs(Node_URI graphIRI, Action action, String email) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString(ONE_ACTION_QUERY);
        queryString.setLiteral("email", email);
        Query query = QueryFactory.create(queryString.toString());

        QueryExecution queryExecution = QueryExecutionFactory.create(query, securityModel);
        ResultSet resultSet = queryExecution.execSelect();
        while (resultSet.hasNext()) {
            QuerySolution solution = resultSet.next();
            String graph = solution.get("graph").asLiteral().getString();
            String permission = solution.get("permission").asLiteral().getString();
            if (grantsAccess(graph, permission, graphIRI, action)) {
                return true;
            }
        }
        return false;
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

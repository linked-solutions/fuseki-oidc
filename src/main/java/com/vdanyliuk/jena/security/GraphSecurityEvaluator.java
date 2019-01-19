package com.vdanyliuk.jena.security;

import java.util.Set;

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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shared.AuthenticationRequiredException;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

@Slf4j
public class GraphSecurityEvaluator implements SecurityEvaluator {

    public static final String ONE_ACTION_QUERY =
            "prefix sec: <http://www.smartparticipation.com/security#> " +
                    "prefix users: <http://www.smartparticipation.com/users#> " +
                    "prefix graphs: <http://www.smartparticipation.com/graphs#> " +
                    "" +
                    "ASK { " +
                    "?u users:email ?email ." +
                    "    ?u sec:graphAccess ?ga .\n" +
                    "    ?ga graphs:graph ?graph ;" +
                    "        sec:accessType ?permission" +
                    "} ";
    private Model securityModel;


    public GraphSecurityEvaluator(Model securityModel) {
        this.securityModel = securityModel;
    }



    @Override
    public boolean evaluate(Object o, Action action, Node graphIRI) throws AuthenticationRequiredException {
        log.info("Object: " + o + "\nAction: " + action + "\nNode:" + graphIRI);
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
        return true;
    }


    @Override
    public boolean evaluate(Object o, Set<Action> set, Node graphIRI) throws AuthenticationRequiredException {
        log.info("Object: " + o + "\nAction: " + set + "\nNode:" + graphIRI);
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
        return true;
    }

    @Override
    public boolean evaluateAny(Object o, Set<Action> set, Node graphIRI) throws AuthenticationRequiredException {
        log.info("Object: " + o + "\nAction: " + set + "\nNode:" + graphIRI);
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
        return true;
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
        log.info("Principal: " + subject);
        return subject;
    }

    /**
     * Verify the Shiro subject is authenticated.
     */
    @Override
    public boolean isPrincipalAuthenticated(Object principal) {
        if (principal instanceof Subject) {
            log.info("Principal authenticated");
            return ((Subject)principal).isAuthenticated();
        }
        log.info("Principal NOT authenticated");
        return false;
    }

    private boolean hasAccess(Subject subject, Node_URI graphIRI, Action action) {
        ParameterizedSparqlString queryString = new ParameterizedSparqlString(ONE_ACTION_QUERY);
        queryString.setLiteral("email", subject.getPrincipal().toString());
        queryString.setIri("graph", graphIRI.getURI());
        queryString.setLiteral("permission", action.toString().toUpperCase());
        Query query = QueryFactory.create(queryString.toString());

        QueryExecution queryExecution = QueryExecutionFactory.create(query, securityModel);
        return queryExecution.execAsk();
    }


}

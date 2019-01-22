package com.vdanyliuk.jena.security;

import static org.apache.jena.permissions.SecurityEvaluator.Action.Create;
import static org.apache.jena.permissions.SecurityEvaluator.Action.Delete;
import static org.apache.jena.permissions.SecurityEvaluator.Action.Read;
import static org.apache.jena.permissions.SecurityEvaluator.Action.Update;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.mem.GraphMem;
import org.apache.jena.permissions.SecurityEvaluator;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.impl.ModelCom;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled
@ExtendWith(MockitoExtension.class)
@DisplayName("Authenticated user test")
class GraphSecurityEvaluatorTest {

    private static final String GRAPH_ONE = "http://www.smartparticipation.com/graphs/1";
    private static final Node GRAPH_ONE_URI = NodeFactory.createURI(GRAPH_ONE);
    private static final String GRAPH_TWO = "http://www.smartparticipation.com/graphs/2";
    private static final String GRAPH_UNKNOWN = "http://some.other.graph/to/test";

    private static final String GRAPH_OWN_USER_ONE = "http://www.smartparticipation.com/graphs/users/user.one@mail.com";
    private static final String GRAPH_OWN_USER_TWO = "http://www.smartparticipation.com/graphs/users/user.two@mail.com";
    private static final String GRAPH_OWN_USER_THREE = "http://www.smartparticipation.com/graphs/users/user.three@mail.com";
    private static final String GRAPH_OWN_USER_FOUR = "http://www.smartparticipation.com/graphs/users/user.four@mail.com";
    private static final String GRAPH_OWN_USER_FIVE = "http://www.smartparticipation.com/graphs/users/user.five@mail.com";

    @Mock
    private Subject subject;

    private GraphSecurityEvaluator securityEvaluator;

    @BeforeEach
    void setUp() {
        //Bind Shiro subject to the thread context
        ThreadState threadState = new SubjectThreadState(subject);
        threadState.bind();

        //Create security graph
        GraphMem securityGraph = new GraphMem();
        Model securityModel = new ModelCom(securityGraph);
        securityModel.read(Objects.requireNonNull(getClass().getClassLoader().getResource("test_security_data.ttl")).toString());

        securityEvaluator = new GraphSecurityEvaluator(securityModel);
    }

    @ParameterizedTest(name = "User {0} can {1} {2} -> {3}")
    @CsvSource({
            "user.one@mail.com, Read, " + GRAPH_ONE + ", true",
            "user.one@mail.com, Create, " + GRAPH_ONE + ", false",
            "user.one@mail.com, Delete, " + GRAPH_ONE + ", false",
            "user.one@mail.com, Update, " + GRAPH_ONE + ", false",

            "user.one@mail.com, Read, " + GRAPH_TWO + ", false",
            "user.one@mail.com, Create, " + GRAPH_TWO + ", false",
            "user.one@mail.com, Update, " + GRAPH_TWO + ", false",
            "user.one@mail.com, Delete, " + GRAPH_TWO + ", false",

            "user.one@mail.com, Read, " + GRAPH_UNKNOWN + ", false",
            "user.one@mail.com, Create, " + GRAPH_UNKNOWN + ", false",
            "user.one@mail.com, Update, " + GRAPH_UNKNOWN + ", false",
            "user.one@mail.com, Delete, " + GRAPH_UNKNOWN + ", false",

            "user.two@mail.com, Read, " + GRAPH_ONE + ", true",
            "user.two@mail.com, Create, " + GRAPH_ONE + ", true",
            "user.two@mail.com, Delete, " + GRAPH_ONE + ", true",
            "user.two@mail.com, Update, " + GRAPH_ONE + ", true",

            "user.two@mail.com, Read, " + GRAPH_TWO + ", false",
            "user.two@mail.com, Create, " + GRAPH_TWO + ", false",
            "user.two@mail.com, Update, " + GRAPH_TWO + ", false",
            "user.two@mail.com, Delete, " + GRAPH_TWO + ", false",

            "user.two@mail.com, Read, " + GRAPH_UNKNOWN + ", false",
            "user.two@mail.com, Create, " + GRAPH_UNKNOWN + ", false",
            "user.two@mail.com, Update, " + GRAPH_UNKNOWN + ", false",
            "user.two@mail.com, Delete, " + GRAPH_UNKNOWN + ", false",

            "user.three@mail.com, Read, " + GRAPH_ONE + ", true",
            "user.three@mail.com, Create, " + GRAPH_ONE + ", false",
            "user.three@mail.com, Delete, " + GRAPH_ONE + ", false",
            "user.three@mail.com, Update, " + GRAPH_ONE + ", false",

            "user.three@mail.com, Read, " + GRAPH_TWO + ", true",
            "user.three@mail.com, Create, " + GRAPH_TWO + ", false",
            "user.three@mail.com, Update, " + GRAPH_TWO + ", false",
            "user.three@mail.com, Delete, " + GRAPH_TWO + ", false",

            "user.three@mail.com, Read, " + GRAPH_UNKNOWN + ", false",
            "user.three@mail.com, Create, " + GRAPH_UNKNOWN + ", false",
            "user.three@mail.com, Update, " + GRAPH_UNKNOWN + ", false",
            "user.three@mail.com, Delete, " + GRAPH_UNKNOWN + ", false",

            "user.four@mail.com, Read, " + GRAPH_ONE + ", true",
            "user.four@mail.com, Create, " + GRAPH_ONE + ", true",
            "user.four@mail.com, Delete, " + GRAPH_ONE + ", true",
            "user.four@mail.com, Update, " + GRAPH_ONE + ", true",

            "user.four@mail.com, Read, " + GRAPH_TWO + ", true",
            "user.four@mail.com, Create, " + GRAPH_TWO + ", true",
            "user.four@mail.com, Update, " + GRAPH_TWO + ", true",
            "user.four@mail.com, Delete, " + GRAPH_TWO + ", true",

            "user.four@mail.com, Read, " + GRAPH_UNKNOWN + ", false",
            "user.four@mail.com, Create, " + GRAPH_UNKNOWN + ", false",
            "user.four@mail.com, Update, " + GRAPH_UNKNOWN + ", false",
            "user.four@mail.com, Delete, " + GRAPH_UNKNOWN + ", false",

            "user.five@mail.com, Read, " + GRAPH_ONE + ", true",
            "user.five@mail.com, Create, " + GRAPH_ONE + ", true",
            "user.five@mail.com, Delete, " + GRAPH_ONE + ", true",
            "user.five@mail.com, Update, " + GRAPH_ONE + ", true",

            "user.five@mail.com, Read, " + GRAPH_TWO + ", true",
            "user.five@mail.com, Create, " + GRAPH_TWO + ", true",
            "user.five@mail.com, Update, " + GRAPH_TWO + ", true",
            "user.five@mail.com, Delete, " + GRAPH_TWO + ", true",

            "user.five@mail.com, Read, " + GRAPH_UNKNOWN + ", true",
            "user.five@mail.com, Create, " + GRAPH_UNKNOWN + ", true",
            "user.five@mail.com, Update, " + GRAPH_UNKNOWN + ", true",
            "user.five@mail.com, Delete, " + GRAPH_UNKNOWN + ", true",

            "user.one@mail.com, Read,   " + GRAPH_OWN_USER_ONE + ", true",
            "user.one@mail.com, Create, " + GRAPH_OWN_USER_ONE + ", true",
            "user.one@mail.com, Update, " + GRAPH_OWN_USER_ONE + ", true",
            "user.one@mail.com, Delete, " + GRAPH_OWN_USER_ONE + ", true",

            "user.two@mail.com, Read,   " + GRAPH_OWN_USER_TWO + ", true",
            "user.two@mail.com, Create, " + GRAPH_OWN_USER_TWO + ", true",
            "user.two@mail.com, Update, " + GRAPH_OWN_USER_TWO + ", true",
            "user.two@mail.com, Delete, " + GRAPH_OWN_USER_TWO + ", true",

            "user.three@mail.com, Read,   " + GRAPH_OWN_USER_THREE + ", true",
            "user.three@mail.com, Create, " + GRAPH_OWN_USER_THREE + ", true",
            "user.three@mail.com, Update, " + GRAPH_OWN_USER_THREE + ", true",
            "user.three@mail.com, Delete, " + GRAPH_OWN_USER_THREE + ", true",

            "user.four@mail.com, Read,   " + GRAPH_OWN_USER_FOUR + ", true",
            "user.four@mail.com, Create, " + GRAPH_OWN_USER_FOUR + ", true",
            "user.four@mail.com, Update, " + GRAPH_OWN_USER_FOUR + ", true",
            "user.four@mail.com, Delete, " + GRAPH_OWN_USER_FOUR + ", true",

            "user.five@mail.com, Read,   " + GRAPH_OWN_USER_FIVE + ", true",
            "user.five@mail.com, Create, " + GRAPH_OWN_USER_FIVE + ", true",
            "user.five@mail.com, Update, " + GRAPH_OWN_USER_FIVE + ", true",
            "user.five@mail.com, Delete, " + GRAPH_OWN_USER_FIVE + ", true",

            "user.one@mail.com, Read,   " + GRAPH_OWN_USER_TWO + ", false",
            "user.one@mail.com, Read,   " + GRAPH_OWN_USER_THREE + ", false",
            "user.one@mail.com, Read,   " + GRAPH_OWN_USER_FOUR + ", false",
            "user.one@mail.com, Read,   " + GRAPH_OWN_USER_FIVE + ", false",
            "user.one@mail.com, Create,   " + GRAPH_OWN_USER_TWO + ", false",
            "user.one@mail.com, Create,   " + GRAPH_OWN_USER_THREE + ", false",
            "user.one@mail.com, Create,   " + GRAPH_OWN_USER_FOUR + ", false",
            "user.one@mail.com, Create,   " + GRAPH_OWN_USER_FIVE + ", false",

            "user.four@mail.com, Read,   " + GRAPH_OWN_USER_TWO + ", false",
            "user.four@mail.com, Read,   " + GRAPH_OWN_USER_THREE + ", false",
            "user.four@mail.com, Read,   " + GRAPH_OWN_USER_ONE + ", false",
            "user.four@mail.com, Read,   " + GRAPH_OWN_USER_FIVE + ", false",
            "user.four@mail.com, Create,   " + GRAPH_OWN_USER_TWO + ", false",
            "user.four@mail.com, Create,   " + GRAPH_OWN_USER_THREE + ", false",
            "user.four@mail.com, Create,   " + GRAPH_OWN_USER_ONE + ", false",
            "user.four@mail.com, Create,   " + GRAPH_OWN_USER_FIVE + ", false",

            "user.five@mail.com, Read,   " + GRAPH_OWN_USER_TWO + ", true",
            "user.five@mail.com, Read,   " + GRAPH_OWN_USER_THREE + ", true",
            "user.five@mail.com, Read,   " + GRAPH_OWN_USER_FOUR + ", true",
            "user.five@mail.com, Read,   " + GRAPH_OWN_USER_FIVE + ", true",
            "user.five@mail.com, Create,   " + GRAPH_OWN_USER_TWO + ", true",
            "user.five@mail.com, Create,   " + GRAPH_OWN_USER_THREE + ", true",
            "user.five@mail.com, Create,   " + GRAPH_OWN_USER_FOUR + ", true",
            "user.five@mail.com, Create,   " + GRAPH_OWN_USER_FIVE + ", true",

    })
    void accessToGraphs(String email, SecurityEvaluator.Action action, String graph, Boolean permitted) {
        when(subject.getPrincipal()).thenReturn(email);
        Node GRAPH_URI = NodeFactory.createURI(graph);

        assertThat(securityEvaluator.evaluate(subject, action, GRAPH_URI), equalTo(permitted));
    }

    @Test
    @DisplayName("Test user.one@mail.com can only read data from graph")
    void evaluateForGraph_userOne() {
        when(subject.getPrincipal()).thenReturn("user.one@mail.com");

        assertTrue(securityEvaluator.evaluate(subject, Read, GRAPH_ONE_URI), "User One is permitted to read");
        assertFalse(securityEvaluator.evaluate(subject, Create, GRAPH_ONE_URI), "User One is not permitted to create");
        assertFalse(securityEvaluator.evaluate(subject, Update, GRAPH_ONE_URI), "User One is not permitted to update");
        assertFalse(securityEvaluator.evaluate(subject, Delete, GRAPH_ONE_URI), "User One is not permitted to delete");
    }

    @Test
    @DisplayName("Test user.one@mail.com can only read data from graph")
    void evaluateForGraph_AllOperations_userOne() {
        when(subject.getPrincipal()).thenReturn("user.one@mail.com");

        Set<SecurityEvaluator.Action> read = Collections.singleton(Read);
        HashSet<SecurityEvaluator.Action> readCreate = new HashSet<>(Arrays.asList(Read, Create));
        HashSet<SecurityEvaluator.Action> readCreateUpdateDelete = new HashSet<>(Arrays.asList(Read, Create, Update, Delete));

        assertTrue(securityEvaluator.evaluate(subject, read, GRAPH_ONE_URI), "User One is permitted to read");
        assertFalse(securityEvaluator.evaluate(subject, readCreate, GRAPH_ONE_URI), "User One is not permitted to create");
        assertFalse(securityEvaluator.evaluate(subject, readCreateUpdateDelete, GRAPH_ONE_URI), "User One is not permitted to create and update");
    }

    @Test
    @DisplayName("Test user.one@mail.com can only read data from graph")
    void evaluateForGraph_AnyOperation_userOne() {
        when(subject.getPrincipal()).thenReturn("user.one@mail.com");

        Set<SecurityEvaluator.Action> read = Collections.singleton(Read);
        HashSet<SecurityEvaluator.Action> readCreate = new HashSet<>(Arrays.asList(Read, Create));
        HashSet<SecurityEvaluator.Action> createUpdateDelete = new HashSet<>(Arrays.asList(Create, Update, Delete));

        assertTrue(securityEvaluator.evaluateAny(subject, read, GRAPH_ONE_URI), "User One is permitted to read");
        assertTrue(securityEvaluator.evaluateAny(subject, readCreate, GRAPH_ONE_URI), "User One is permitted to read");
        assertFalse(securityEvaluator.evaluateAny(subject, createUpdateDelete, GRAPH_ONE_URI), "User One is not permitted to create, update or delete");
    }

    @Test
    @DisplayName("Test user.two@mail.com can do any operation on graph")
    void evaluateForGraph_userTwo() {
        when(subject.getPrincipal()).thenReturn("user.two@mail.com");

        assertTrue(securityEvaluator.evaluate(subject, Read, GRAPH_ONE_URI), "User Two is permitted to read");
        assertTrue(securityEvaluator.evaluate(subject, Create, GRAPH_ONE_URI), "User Two is permitted to create");
        assertTrue(securityEvaluator.evaluate(subject, Update, GRAPH_ONE_URI), "User Two is permitted to update");
        assertTrue(securityEvaluator.evaluate(subject, Delete, GRAPH_ONE_URI), "User Two is permitted to delete");
    }


    @Test
    @DisplayName("Test user.one@mail.com can only read data from graph")
    void evaluateForGraph_AllOperations_userTwo() {
        when(subject.getPrincipal()).thenReturn("user.two@mail.com");

        Set<SecurityEvaluator.Action> read = Collections.singleton(Read);
        HashSet<SecurityEvaluator.Action> readCreate = new HashSet<>(Arrays.asList(Read, Create));
        HashSet<SecurityEvaluator.Action> readCreateUpdateDelete = new HashSet<>(Arrays.asList(Read, Create, Update, Delete));

        assertTrue(securityEvaluator.evaluate(subject, read, GRAPH_ONE_URI), "User Two is permitted to read");
        assertTrue(securityEvaluator.evaluate(subject, readCreate, GRAPH_ONE_URI), "User Two is permitted to read and create");
        assertTrue(securityEvaluator.evaluate(subject, readCreateUpdateDelete, GRAPH_ONE_URI), "User Two is permitted to execute all operations");
    }


    @Test
    @DisplayName("Test user.one@mail.com can only read data from graph")
    void evaluateForGraph_AnyOperation_userTwo() {
        when(subject.getPrincipal()).thenReturn("user.two@mail.com");

        Set<SecurityEvaluator.Action> read = Collections.singleton(Read);
        HashSet<SecurityEvaluator.Action> readCreate = new HashSet<>(Arrays.asList(Read, Create));
        HashSet<SecurityEvaluator.Action> createUpdateDelete = new HashSet<>(Arrays.asList(Create, Update, Delete));

        assertTrue(securityEvaluator.evaluateAny(subject, read, GRAPH_ONE_URI), "User Two is permitted to read");
        assertTrue(securityEvaluator.evaluateAny(subject, readCreate, GRAPH_ONE_URI), "User Two is permitted to read");
        assertTrue(securityEvaluator.evaluateAny(subject, createUpdateDelete, GRAPH_ONE_URI), "User Two is permitted to execute any operation");
    }

    @Test
    @DisplayName("Get principal should return bound Shiro subject")
    void getPrincipal() {
        assertSame(subject, securityEvaluator.getPrincipal());
    }

    @Test
    @DisplayName("Evaluator should use Shiro subject to check if authenticated")
    void isPrincipalAuthenticated() {
        when(subject.isAuthenticated()).thenReturn(true);
        assertTrue(securityEvaluator.isPrincipalAuthenticated(securityEvaluator.getPrincipal()));

        when(subject.isAuthenticated()).thenReturn(false);
        assertFalse(securityEvaluator.isPrincipalAuthenticated(securityEvaluator.getPrincipal()));

        assertFalse(securityEvaluator.isPrincipalAuthenticated(new Object()), "Only Shiro subject can be authenticated");
        assertFalse(securityEvaluator.isPrincipalAuthenticated(null), "Only non-null Shiro subject can be authenticated");
    }
}
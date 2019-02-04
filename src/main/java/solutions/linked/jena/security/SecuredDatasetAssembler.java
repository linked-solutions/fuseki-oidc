/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package solutions.linked.jena.security;

import static org.apache.jena.permissions.AssemblerConstants.EVALUATOR_IMPL;
import static org.apache.jena.permissions.AssemblerConstants.URI;
import static org.apache.jena.sparql.util.graph.GraphUtils.exactlyOneProperty;
import static org.apache.jena.sparql.util.graph.GraphUtils.getStringValue;
import static org.apache.jena.tdb.assembler.VocabTDB.pUnionDefaultGraph;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.assembler.Assembler;
import org.apache.jena.assembler.Mode;
import org.apache.jena.assembler.assemblers.AssemblerGroup;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.dboe.base.file.Location;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.permissions.AssemblerConstants;
import org.apache.jena.permissions.SecurityEvaluator;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sparql.core.assembler.DatasetAssembler;
import org.apache.jena.sparql.expr.NodeValue;
import org.apache.jena.sparql.util.MappingRegistry;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.tdb.TDB;
import org.apache.jena.tdb.assembler.DatasetAssemblerTDB;
import org.apache.jena.tdb2.DatabaseMgr;
import org.apache.jena.tdb2.TDB2;
import org.apache.jena.tdb2.assembler.VocabTDB2;

@Slf4j
public class SecuredDatasetAssembler extends DatasetAssembler {

    public static final Property SECURED_DATASET = ResourceFactory.createProperty(URI + "SecuredDataset");
    public static final Property SECURITY_GRAPH_NAME = ResourceFactory.createProperty(URI + "securityGraphName");
    public static final Property SECURITY_BASE_MODEL = ResourceFactory.createProperty(URI + "securityBaseModel");

    private static boolean initialized;

    static {
        JenaSystem.init();
        init();
    }

    private static synchronized void init() {
        if (initialized)
            return;
        MappingRegistry.addPrefixMapping("sec", AssemblerConstants.URI);
        registerWith(Assembler.general);
        initialized = true;
    }

    /**
     * Register this assembler in the assembler group.
     *
     * @param group The assembler group to register with.
     */
    private static void registerWith(AssemblerGroup group) {
        if (group == null)
            group = Assembler.general;
        group.implementWith(SECURED_DATASET, new SecuredDatasetAssembler());
    }

    @Override
    public Dataset createDataset(Assembler a, Resource root, Mode mode) {
        return make(a, root);
    }

    private static Dataset make(Assembler a, Resource root) {
        exactlyOneProperty(root, VocabTDB2.pLocation);
        exactlyOneProperty(root, SECURITY_GRAPH_NAME);

        String dir = getStringValue(root, VocabTDB2.pLocation) ;
        org.apache.jena.dboe.base.file.Location loc = Location.create(dir) ;
        DatasetGraph dsg = DatabaseMgr.connectDatasetGraph(loc) ;

        if ( root.hasProperty(VocabTDB2.pUnionDefaultGraph) ) {
            Node b = root.getProperty(VocabTDB2.pUnionDefaultGraph).getObject().asNode() ;
            NodeValue nv = NodeValue.makeNode(b) ;
            if ( nv.isBoolean() )
                dsg.getContext().set(TDB2.symUnionDefaultGraph, nv.getBoolean()) ;
            else
                Log.warn(org.apache.jena.tdb2.assembler.DatasetAssemblerTDB.class, "Failed to recognize value for union graph setting (ignored): " + b) ;
        }

        String securityGraphName = getStringValue(root, SECURITY_GRAPH_NAME);

        Resource modelResource = getUniqueResource(root, SECURITY_BASE_MODEL);
        if (modelResource == null) {
            throw new AssemblerException(root,
                    String.format("Property %s must be provided for %s", SECURITY_BASE_MODEL, root));
        }
        Model model = getModel(a, modelResource);

        addSecurityContent(dsg, securityGraphName, model);

        Resource evaluatorImpl = getUniqueResource(root, EVALUATOR_IMPL);
        if (evaluatorImpl == null) {
            throw new AssemblerException(root,
                    String.format("Property %s must be provided for %s", EVALUATOR_IMPL, root));
        }
        SecurityEvaluator securityEvaluator = getEvaluatorImpl(a, evaluatorImpl);

        dsg = new SecuredDatasetGraph(dsg, securityEvaluator);

        if (root.hasProperty(pUnionDefaultGraph)) {
            Node b = root.getProperty(pUnionDefaultGraph).getObject().asNode();
            NodeValue nv = NodeValue.makeNode(b);
            if (nv.isBoolean())
                dsg.getContext().set(TDB.symUnionDefaultGraph, nv.getBoolean());
            else
                Log.warn(DatasetAssemblerTDB.class, "Failed to recognize value for union graph setting (ignored): " + b);
        }

        AssemblerUtils.setContext(root, dsg.getContext());
        return DatasetFactory.wrap(dsg);
    }

    private static void addSecurityContent(DatasetGraph dsg, String securityGraphName, Model model) {
        dsg.begin(TxnType.WRITE);
        try {
            dsg.addGraph(NodeFactory.createURI(securityGraphName), model.getGraph());
            dsg.commit();
        } catch (Throwable t){
            log.warn("Error while populating security graph: " + t.getMessage());
            log.debug(t.getMessage(), t);
            dsg.abort();
        }
    }

    private static Model getModel(Assembler a, Resource model) {
        Object obj = a.open(a, model, Mode.ANY);
        if (obj instanceof Model) {
            return (Model) obj;
        }
        throw new AssemblerException(model, String.format(
                "%s does not specify a Model instance", model));
    }

    private static SecurityEvaluator getEvaluatorImpl(Assembler a, Resource evaluatorImpl) {
        Object obj = a.open(a, evaluatorImpl, Mode.ANY);
        if (obj instanceof SecurityEvaluator) {
            return (SecurityEvaluator) obj;
        }
        throw new AssemblerException(evaluatorImpl, String.format(
                "%s does not specify a SecurityEvaluator instance", evaluatorImpl));
    }

}

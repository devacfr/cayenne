/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.graph;

import java.io.Serializable;

import javax.swing.event.UndoableEditListener;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.Entity;
import org.apache.cayenne.modeler.ProjectController;
import org.apache.cayenne.util.XMLSerializable;
import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;

/**
 * Interface for building graphs which represent some prespective of a domain
 */
public interface GraphBuilder extends Serializable, XMLSerializable, UndoableEditListener {
    public static final double ZOOM_FACTOR = 1.3;
    
    /**
     * Builds graph
     */
    public void buildGraph(ProjectController mediator, DataDomain domain, boolean layout);
    
    /**
     * Invoked at destroying of the builder
     */
    public void destroy();
    
    /**
     * Returns built graph for this builder
     */
    public JGraph getGraph();
    
    /**
     * Returns domain.
     */
    public DataDomain getDataDomain();
        
    /**
     * Returns type of the graph
     */
    public GraphType getType();
    
    /**
     * Returns selected entity, <code>null</code> if none is selected
     */
    public Entity getSelectedEntity();
    
    /**
     * Returns cell of an entity
     */
    public DefaultGraphCell getEntityCell(String entityName);
}

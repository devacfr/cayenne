/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer. 
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.cayenne.project;

import java.io.PrintWriter;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.ConfigSaver;
import org.objectstyle.cayenne.conf.DriverDataSourceFactory;

/**
 * DataNodeFile is a ProjectFile abstraction of the 
 * DataNode file in a Cayenne project. 
 * 
 * @author Andrei Adamchik
 */
public class DataNodeFile extends ProjectFile {
    public static final String LOCATION_SUFFIX = ".driver.xml";

    protected DataNode nodeObj;

    public DataNodeFile() {}

    /**
     * Constructor for DataNodeFile.
     * @param name
     * @param extension
     */
    public DataNodeFile(Project project, DataNode node) {
        super(project, node.getDataSourceLocation());
        this.nodeObj = node;
    }

    /**
     * @see org.objectstyle.cayenne.project.ProjectFile#getObject()
     */
    public Object getObject() {
        return nodeObj;
    }

    /**
     * @see org.objectstyle.cayenne.project.ProjectFile#getObjectName()
     */
    public String getObjectName() {
        return nodeObj.getName();
    }

    public void save(PrintWriter out) throws Exception {
        ProjectDataSource src = (ProjectDataSource) nodeObj.getDataSource();
        new ConfigSaver().storeDataNode(out, src.getDataSourceInfo());
    }

    /**
     * @see org.objectstyle.cayenne.project.ProjectFile#canHandle(Object)
     */
    public boolean canHandle(Object obj) {
        if (obj instanceof DataNode) {
            DataNode node = (DataNode) obj;

            // only driver datasource factory requires a file
            if (DriverDataSourceFactory
                .class
                .getName()
                .equals(node.getDataSourceFactory())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Updates node location to match the name before save.
     */
    public void willSave() {
        super.willSave();

        if (nodeObj != null && canHandle(nodeObj)) {
            nodeObj.setDataSourceLocation(getLocation());
        }
    }

    /**
     * Returns ".driver.xml" that should be used as a file suffix 
     * for DataNode driver files.
     */
    public String getLocationSuffix() {
        return LOCATION_SUFFIX;
    }
}

/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2004 The ObjectStyle Group 
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
package org.objectstyle.cayenne.modeler.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.Relationship;
import org.objectstyle.cayenne.util.PropertyComparator;
import org.scopemvc.core.IntIndexSelector;
import org.scopemvc.core.ModelChangeEvent;
import org.scopemvc.core.Selector;
import org.scopemvc.model.basic.BasicModel;
import org.scopemvc.model.collection.ListModel;

/**
 * A Scope model for mapping an ObjRelationship to one or 
 * more DbRelationships.
 * 
 * @since 1.1
 * @author Andrei Adamchik
 */
public class MapObjRelationshipModel extends BasicModel {
    public static final Selector DB_RELATIONSHIP_PATH_SELECTOR =
        Selector.fromString("dbRelationshipPath");
    public static final Selector RELATIONSHIP_SELECTOR =
        Selector.fromString("relationship");
    public static final Selector RELATIONSHIP_DESCRIPTION_SELECTOR =
        Selector.fromString("relationshipDescription");
    public static final Selector SELECTED_PATH_COMPONENT_SELECTOR =
        Selector.fromString("selectedPathComponent");
    public static final Selector OBJECT_TARGET_SELECTOR =
        Selector.fromString("objectTarget");
    public static final Selector OBJECT_TARGETS_SELECTOR =
        Selector.fromString("objectTargets");

    protected ObjRelationship relationship;
    protected ListModel dbRelationshipPath;
    protected EntityRelationshipsModel selectedPathComponent;
    protected ObjEntity objectTarget;
    protected List objectTargets;

    public MapObjRelationshipModel(
        ObjRelationship relationship,
        Collection objEntities) {
        this.relationship = relationship;
        this.objectTarget = (ObjEntity) relationship.getTargetEntity();

        // prepare entities - create a copy and sort
        this.objectTargets = new ArrayList(objEntities);
        Collections.sort(objectTargets, new PropertyComparator("name", ObjEntity.class));

        // validate -
        // current limitation is that an ObjRelationship must have source 
        // and target entities present, with DbEntities chosen.
        validateCanMap();

        // wrap path
        this.dbRelationshipPath = new ListModel();
        Iterator it = relationship.getDbRelationships().iterator();
        while (it.hasNext()) {
            DbRelationship dbRelationship = (DbRelationship) it.next();
            this.dbRelationshipPath.add(new EntityRelationshipsModel(dbRelationship));
        }

        // add dummy last relationship if we are not connected
        connectTail();

        this.dbRelationshipPath.addModelChangeListener(this);
    }

    public ObjRelationship getRelationship() {
        return relationship;
    }

    public String getRelationshipDescription() {
        return EntityRelationshipsModel.displayName(
            relationship.getName(),
            relationship.getSourceEntity(),
            objectTarget);
    }

    public ListModel getDbRelationshipPath() {
        return dbRelationshipPath;
    }

    public EntityRelationshipsModel getSelectedPathComponent() {
        return selectedPathComponent;
    }

    public void setSelectedPathComponent(EntityRelationshipsModel selectedPathComponent) {
        if (this.selectedPathComponent != selectedPathComponent) {
            unlistenOldSubmodel(SELECTED_PATH_COMPONENT_SELECTOR);
            this.selectedPathComponent = selectedPathComponent;
            listenNewSubmodel(SELECTED_PATH_COMPONENT_SELECTOR);
            fireModelChange(
                ModelChangeEvent.VALUE_CHANGED,
                SELECTED_PATH_COMPONENT_SELECTOR);
        }
    }

    public ObjEntity getObjectTarget() {
        return objectTarget;
    }

    public void setObjectTarget(ObjEntity objectTarget) {
        if (this.objectTarget != objectTarget) {
            unlistenOldSubmodel(OBJECT_TARGET_SELECTOR);
            this.objectTarget = objectTarget;
            listenNewSubmodel(OBJECT_TARGET_SELECTOR);
            fireModelChange(ModelChangeEvent.VALUE_CHANGED, OBJECT_TARGET_SELECTOR);

            // "description" is a derived property, so fire it too
            fireModelChange(
                ModelChangeEvent.VALUE_CHANGED,
                RELATIONSHIP_DESCRIPTION_SELECTOR);
        }
    }

    /**
     * Returns a list of ObjEntities available for target mapping.
     */
    public List getObjectTargets() {
        return objectTargets;
    }

    public void modelChanged(ModelChangeEvent event) {

        // if a different relationship was selected, we may need to rebuild the list
        Selector selector = event.getSelector();
        while (selector != null) {
            if (selector instanceof IntIndexSelector) {
                IntIndexSelector indexSel = (IntIndexSelector) selector;
                relationshipChanged(indexSel.getIndex());
                break;
            }

            selector = selector.getNext();
        }

        super.modelChanged(event);
    }

    /**
     * Processes relationship path when path component at index was changed.
     */
    public synchronized void relationshipChanged(int index) {
        // strip everything starting from the index
        breakChain(index);

        // connect the ends
        connectTail();
        dbRelationshipPath.fireModelChange(VALUE_CHANGED, null);
    }

    public synchronized void savePath() {
        relationship.clearDbRelationships();

        Iterator it = dbRelationshipPath.iterator();
        while (it.hasNext()) {
            EntityRelationshipsModel next = (EntityRelationshipsModel) it.next();
            Relationship nextPathComponent = next.getSelectedRelationship();
            if (nextPathComponent == null) {
                break;
            }

            relationship.addDbRelationship((DbRelationship) nextPathComponent);
        }
    }

    private void breakChain(int index) {
        // strip everything starting from the index
        dbRelationshipPath.makeActive(false);

        try {
            while (dbRelationshipPath.size() > (index + 1)) {
                // remove last
                dbRelationshipPath.remove(dbRelationshipPath.size() - 1);
            }
        }
        finally {
            dbRelationshipPath.makeActive(true);
        }
    }

    private void connectTail() {
        Relationship last = null;

        int size = dbRelationshipPath.size();
        if (size > 0) {
            EntityRelationshipsModel wrapper =
                (EntityRelationshipsModel) dbRelationshipPath.get(size - 1);
            last = wrapper.getSelectedRelationship();

        }

        Entity target = getEndEntity();

        if (last == null || last.getTargetEntity() != target) {
            // try to connect automatically, if we can't use dummy connector

            Entity source = (last == null) ? getStartEntity() : last.getTargetEntity();
            Relationship anyConnector = source.getAnyRelationship(target);
            EntityRelationshipsModel connector = null;

            connector =
                (anyConnector == null)
                    ? new EntityRelationshipsModel(source, getEndEntity())
                    : new EntityRelationshipsModel(anyConnector);

            dbRelationshipPath.makeActive(false);
            try {
                dbRelationshipPath.add(connector);
            }
            finally {
                dbRelationshipPath.makeActive(true);
            }
        }
    }

    private void validateCanMap() {
        if (relationship.getSourceEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without source entity.");
        }

        if (relationship.getTargetEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without target entity.");
        }

        if (getStartEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without source DbEntity.");
        }

        if (getEndEntity() == null) {
            throw new CayenneRuntimeException("Can't map relationship without target DbEntity.");
        }
    }

    public DbEntity getStartEntity() {
        return ((ObjEntity) relationship.getSourceEntity()).getDbEntity();
    }

    public DbEntity getEndEntity() {
        return objectTarget.getDbEntity();
    }
}

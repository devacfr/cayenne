/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
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
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.dialog.datamap;

import java.awt.Component;
import java.util.Iterator;

import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.map.event.AttributeEvent;
import org.objectstyle.cayenne.map.event.EntityEvent;
import org.objectstyle.cayenne.map.event.RelationshipEvent;
import org.objectstyle.cayenne.modeler.ProjectController;
import org.objectstyle.cayenne.modeler.util.CayenneController;
import org.objectstyle.cayenne.swing.BindingBuilder;

public class LockingUpdateController extends CayenneController {

    protected LockingUpdateDialog view;
    protected DataMap dataMap;

    public LockingUpdateController(ProjectController parent, DataMap dataMap) {
        super(parent);
        this.dataMap = dataMap;
    }

    public void startup() {

        view = new LockingUpdateDialog();

        boolean on = dataMap.getDefaultLockType() == ObjEntity.LOCK_TYPE_OPTIMISTIC;
        view.setTitle(on ? "Enable Optimistic Locking" : "Disable Optimistic Locking");

        initBindings();

        view.pack();
        view.setModal(true);
        centerView();
        makeCloseableOnEscape();
        view.show();
    }

    public Component getView() {
        return view;
    }

    protected void initBindings() {
        BindingBuilder builder = new BindingBuilder(
                getApplication().getBindingFactory(),
                this);

        builder.bindToAction(view.getCancelButton(), "cancelAction()");
        builder.bindToAction(view.getUpdateButton(), "updateAction()");
    }

    public void cancelAction() {
        if (view != null) {
            view.dispose();
        }
    }

    public void updateAction() {
        int defaultLockType = dataMap.getDefaultLockType();
        boolean on = defaultLockType == ObjEntity.LOCK_TYPE_OPTIMISTIC;

        boolean updateEntities = view.getEntities().isSelected();
        boolean updateAttributes = view.getAttributes().isSelected();
        boolean updateRelationships = view.getRelationships().isSelected();
        ProjectController parent = (ProjectController) getParent();

        Iterator it = dataMap.getObjEntities().iterator();
        while (it.hasNext()) {
            ObjEntity entity = (ObjEntity) it.next();

            if (updateEntities && defaultLockType != entity.getDeclaredLockType()) {
                entity.setDeclaredLockType(defaultLockType);
                parent.fireObjEntityEvent(new EntityEvent(this, entity));
            }

            if (updateAttributes) {
                Iterator attributes = entity.getAttributes().iterator();
                while (attributes.hasNext()) {

                    ObjAttribute a = (ObjAttribute) attributes.next();
                    if (a.isUsedForLocking() != on) {
                        a.setUsedForLocking(on);
                        parent.fireObjAttributeEvent(new AttributeEvent(this, a, entity));
                    }
                }
            }

            if (updateRelationships) {
                Iterator relationships = entity.getRelationships().iterator();
                while (relationships.hasNext()) {

                    ObjRelationship r = (ObjRelationship) relationships.next();
                    if (r.isUsedForLocking() != on) {
                        r.setUsedForLocking(on);
                        parent.fireObjRelationshipEvent(new RelationshipEvent(
                                this,
                                r,
                                entity));
                    }
                }
            }
        }

        if (view != null) {
            view.dispose();
        }
    }
}

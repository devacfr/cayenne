/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

package org.objectstyle.cayenne.access;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.CayenneException;
import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObject;
import org.objectstyle.cayenne.DataRow;
import org.objectstyle.cayenne.Fault;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.PersistenceState;
import org.objectstyle.cayenne.TempObjectId;
import org.objectstyle.cayenne.access.event.DataContextEvent;
import org.objectstyle.cayenne.access.util.IteratedSelectObserver;
import org.objectstyle.cayenne.access.util.PrefetchHelper;
import org.objectstyle.cayenne.access.util.QueryUtils;
import org.objectstyle.cayenne.access.util.RelationshipDataSource;
import org.objectstyle.cayenne.access.util.SelectObserver;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.dba.PkGenerator;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.event.EventSubject;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbAttributePair;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.DbRelationship;
import org.objectstyle.cayenne.map.DeleteRule;
import org.objectstyle.cayenne.map.Entity;
import org.objectstyle.cayenne.map.ObjAttribute;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;
import org.objectstyle.cayenne.query.GenericSelectQuery;
import org.objectstyle.cayenne.query.PrefetchSelectQuery;
import org.objectstyle.cayenne.query.Query;
import org.objectstyle.cayenne.query.SelectQuery;

/** 
 * Class that provides applications with access to Cayenne persistence features. 
 * In most cases this is the only access class directly used in the application.
 * 
 * <p>
 * Most common DataContext use pattern is to create one DataContext per session. 
 * "Session" may be a an HttpSesession in a web application, or any other similar 
 * concept in a multiuser application.
 * 
 * </p><p>
 * DataObjects are registered with DataContext either <i>implicitly</i> when they are 
 * fetched via a query, or read via a relationship from another object, 
 * or <i>explicitly</i> via calling {@link #createAndRegisterNewObject(Class)} during 
 * new DataObject creation. DataContext tracks changes made to its DataObjects in memory, 
 * and flushes them to the database when {@link #commitChanges()} is called. Until 
 * DataContext is committed, changes made to its objects are not visible in other 
 * DataContexts.</p><p>
 * 
 * Each DataObject can belong only to a single DataContext. To create a replica of an object
 * from a different DataContext in a local context, use {@link #localObjects(java.util.List)} 
 * method.
 *
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 *
 * @author Andrei Adamchik
 */
public class DataContext implements QueryEngine, Serializable {
    private static Logger logObj = Logger.getLogger(DataContext.class);

    // noop delegate 
    private static final DataContextDelegate defaultDelegate =
        new DataContextDelegate() {

        public GenericSelectQuery willPerformSelect(
            DataContext context,
            GenericSelectQuery query) {
            return query;
        }

        public boolean shouldMergeChanges(DataObject object, DataRow snapshotInStore) {
            return true;
        }

        public boolean shouldProcessDelete(DataObject object) {
            return true;
        }

        public void finishedMergeChanges(DataObject object) {

        }

        public void finishedProcessDelete(DataObject object) {

        }
    };

    // DataContext events
    public static final EventSubject WILL_COMMIT =
        EventSubject.getSubject(DataContext.class, "DataContextWillCommit");
    public static final EventSubject DID_COMMIT =
        EventSubject.getSubject(DataContext.class, "DataContextDidCommit");
    public static final EventSubject DID_ROLLBACK =
        EventSubject.getSubject(DataContext.class, "DataContextDidRollback");

    // event posting default for new DataContexts
    private static boolean transactionEventsEnabledDefault;

    // enable/disable event handling for individual instances
    private boolean transactionEventsEnabled;

    // Set of DataContextDelegates to be notified.
    private DataContextDelegate delegate;

    protected boolean usingSharedSnaphsotCache;
    protected boolean validatingObjectsOnCommit;
    protected ObjectStore objectStore;

    protected transient QueryEngine parent;

    /**
     * Stores the name of parent DataDomain. Used to defer initialization 
     * of the parent QueryEngine after deserialization. This helps
     * avoid an issue with certain servlet engines (e.g. Tomcat) where
     * HttpSessions with DataContext's are deserialized at startup
     * before Cayenne stack is fully initialized.
     */
    protected transient String lazyInitParentDomainName;

    protected transient ToManyListDataSource relationshipDataSource;

    /**
      * A factory method of DataObjects. Uses Configuration ClassLoader to
      * instantiate a new instance of DataObject of a given class.
      */
    private static final DataObject newDataObject(String className) throws Exception {
        return (DataObject) Configuration
            .getResourceLoader()
            .loadClass(className)
            .newInstance();
    }

    /**
     * Factory method that creates and returns a new instance of DataContext based on default domain. If more
     * than one domain exists in the current configuration, {@link #createDataContext(String)
     * createDataContext(String domainName)} must be used instead.
     */
    public static DataContext createDataContext() {
        return Configuration.getSharedConfiguration().getDomain().createDataContext();
    }

    /**
     * @since 1.1
     */
    public static DataContext createDataContext(boolean useSharedCache) {
        return Configuration.getSharedConfiguration().getDomain().createDataContext(
            useSharedCache);
    }

    /**
     * Factory method that creates and returns a new instance of DataContext using named 
     * domain as its parent. If there is no domain matching the name argument, an exception 
     * is thrown.
     */
    public static DataContext createDataContext(String domainName) {
        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext();
    }

    /**
     * @since 1.1
     */
    public static DataContext createDataContext(
        String domainName,
        boolean useSharedCache) {

        DataDomain domain = Configuration.getSharedConfiguration().getDomain(domainName);
        if (domain == null) {
            throw new IllegalArgumentException("Non-existent domain: " + domainName);
        }
        return domain.createDataContext(useSharedCache);
    }

    /**
     * Default constructor that creates a DataContext that has no
     * association with a DataDomain. 
     */
    public DataContext() {
        this(null, null);
    }

    /**
     * Creates new DataContext and initializes it with the parent QueryEngine. 
     * Normally parent is an instance of DataDomain. DataContext will use parent
     * to execute database queries, updates, and access mapping information.
     * 
     * @deprecated since 1.1 use {@link #DataContext(QueryEngine, DataRowStore)}
     */
    public DataContext(QueryEngine parent) {
        setParent(parent);

        DataRowStore snapshotCache = null;
        if (parent instanceof DataDomain) {
            snapshotCache = ((DataDomain) parent).getSharedSnapshotCache();
        }

        this.objectStore = new ObjectStore(snapshotCache);
        this.relationshipDataSource = new RelationshipDataSource(this);
        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
    }

    /**
     * Creates a DataContext with parent QueryEngine and a DataRowStore that 
     * should be used by the ObjectStore.
     * 
     * @since 1.1
     * @param parent parent QueryEngine used to communicate with the data source.
     * @param snapshotCache DataRowStore used by DataContext's ObjectStore.
     */
    public DataContext(QueryEngine parent, DataRowStore snapshotCache) {
        setParent(parent);

        this.objectStore = new ObjectStore(snapshotCache);
        this.relationshipDataSource = new RelationshipDataSource(this);
        this.setTransactionEventsEnabled(transactionEventsEnabledDefault);
        this.usingSharedSnaphsotCache =
            getParentDataDomain() != null
                && snapshotCache == getParentDataDomain().getSharedSnapshotCache();
    }

    /**
     * Initializes parent if deserialization left it uninitialized.
     */
    private final void awakeFromDeserialization() {
        if (parent == null && lazyInitParentDomainName != null) {

            DataDomain domain =
                Configuration.getSharedConfiguration().getDomain(
                    lazyInitParentDomainName);

            this.parent = domain;

            if (isUsingSharedSnapshotCache() && domain != null) {
                this.objectStore.setDataRowCache(domain.getSharedSnapshotCache());
            }
        }
    }

    /** 
     * Returns parent QueryEngine object. In most cases returned object
     * is an instance of DataDomain.
     */
    public QueryEngine getParent() {
        awakeFromDeserialization();
        return parent;
    }

    /**
     * <i>
     * Note: currently nested DataContexts are not supported,
     * so this method simply calls "getParent()". Using this method is preferrable
     * to calling "getParent()" directly and casting it to DataDomain, since it more
     * likely to be compatible with the future releases of Cayenne.</i>
     * 
     * @return DataDomain that is a direct or indirect parent
     * of this DataContext.
     * 
     * @since 1.1
     */
    public DataDomain getParentDataDomain() {
        return (DataDomain) getParent();
    }

    /**
     * Sets direct parent of this DataContext.
     */
    public void setParent(QueryEngine parent) {
        this.parent = parent;
    }

    /**
     * Sets a DataContextDelegate for this context. Delegate
     * is notified of certain events in the DataContext lifecycle
     * and can customize DataContext behavior.
     * 
     * @since 1.1
     */
    public void setDelegate(DataContextDelegate delegate) {
        this.delegate = delegate;
    }

    /**
     * Returns a delegate currently associated with this DataContext.
     * 
     * @since 1.1
     */
    public DataContextDelegate getDelegate() {
        return delegate;
    }

    /**
     * @return a delegate instance if it is initialized, or a shared
     * noop implementation the context has no delegate. Useful to prevent
     * extra null checks and conditional logic in the code.
     * 
     * @since 1.1
     */
    DataContextDelegate nonNullDelegate() {
        return (delegate != null) ? delegate : DataContext.defaultDelegate;
    }

    /**
     * @deprecated Since 1.1 all SnapshotManager methods are defined
     * as static methods in DataRowUtils.
     */
    public SnapshotManager getSnapshotManager() {
        return new SnapshotManager(relationshipDataSource);
    }

    /**
     * Returns ToManyListDataSource instance that uses this DataContext to populate relationships.
     * 
     * @return ToManyListDataSource instance that uses this DataContext to populate relationships.
     */
    public ToManyListDataSource getRelationshipDataSource() {
        return this.relationshipDataSource;
    }

    /**
     * Returns ObjectStore associated with this DataContext.
     */
    public ObjectStore getObjectStore() {
        awakeFromDeserialization();
        return objectStore;
    }

    /**
     * Returns <code>true</code> if there are any modified,
     * deleted or new objects registered with this DataContext,
     * <code>false</code> otherwise.
     */
    public boolean hasChanges() {
        return getObjectStore().hasChanges();
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.NEW
     */
    public Collection newObjects() {
        return getObjectStore().objectsInState(PersistenceState.NEW);
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.DELETED
     */
    public Collection deletedObjects() {
        return getObjectStore().objectsInState(PersistenceState.DELETED);
    }

    /** Returns a list of objects that are registered
     *  with this DataContext and have a state PersistenceState.MODIFIED
     */
    public Collection modifiedObjects() {
        return getObjectStore().objectsInState(PersistenceState.MODIFIED);
    }

    /**
     * Returns an object for a given ObjectId.
     * If object is not registered with this context,
     * a "hollow" object fault is created, registered, 
     * and returned to the caller.
     */
    public DataObject registeredObject(ObjectId oid) {
        // must synchronize on ObjectStore since we must read and write atomically
        synchronized (getObjectStore()) {
            DataObject obj = objectStore.getObject(oid);
            if (obj == null) {
                try {
                    obj = DataContext.newDataObject(oid.getObjClass().getName());
                }
                catch (Exception ex) {
                    String entity =
                        (oid != null)
                            ? getEntityResolver()
                                .lookupObjEntity(oid.getObjClass())
                                .getName()
                            : null;
                    throw new CayenneRuntimeException(
                        "Error creating object for entity '" + entity + "'.",
                        ex);
                }

                obj.setObjectId(oid);
                obj.setPersistenceState(PersistenceState.HOLLOW);
                obj.setDataContext(this);
                objectStore.addObject(obj);
            }
            return obj;
        }
    }

    /**
     * Creates or gets from cache a DataRow reflecting current object state.
     * 
     * @since 1.1
     */
    public DataRow currentSnapshot(DataObject anObject) {
        ObjEntity entity = getEntityResolver().lookupObjEntity(anObject);

        // for a HOLLOW object return snapshot from cache
        if (anObject.getPersistenceState() == PersistenceState.HOLLOW
            && anObject.getDataContext() != null) {

            ObjectId id = anObject.getObjectId();
            return getObjectStore().getSnapshot(id, this);
        }

        DataRow snapshot = new DataRow(10);

        Map attrMap = entity.getAttributeMap();
        Iterator it = attrMap.keySet().iterator();
        while (it.hasNext()) {
            String attrName = (String) it.next();
            ObjAttribute objAttr = (ObjAttribute) attrMap.get(attrName);
            //processing compound attributes correctly
            snapshot.put(
                objAttr.getDbAttributePath(),
                anObject.readPropertyDirectly(attrName));
        }

        Map relMap = entity.getRelationshipMap();
        Iterator itr = relMap.keySet().iterator();
        while (itr.hasNext()) {
            String relName = (String) itr.next();
            ObjRelationship rel = (ObjRelationship) relMap.get(relName);

            // to-many will be handled on the other side
            if (rel.isToMany()) {
                continue;
            }

            // dependent relationship key is part of PK
            // and will be handled downstream 
            if (rel.isToDependentEntity()) {
                continue;
            }

            Object targetObject = anObject.readPropertyDirectly(relName);
            if (targetObject == null) {
                continue;
            }
            
            // if target is Fault, get id attributes from stored snapshot
            // to avoid unneeded fault triggering
            if (targetObject instanceof Fault) {
                DataRow storedSnapshot =
                    getObjectStore().getSnapshot(anObject.getObjectId(), this);
                if (storedSnapshot == null) {
                    throw new CayenneRuntimeException(
                        "No matching objects found for ObjectId "
                            + anObject.getObjectId()
                            + ". Object may have been deleted externally.");
                }
                
                DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
                Iterator joins = dbRel.getJoins().iterator();
                while(joins.hasNext()) {
                    DbAttributePair join = (DbAttributePair)joins.next();
                    String key = join.getSource().getName();
                    snapshot.put(key, storedSnapshot.get(key));
                }
                
                continue;
            }

            // target is resolved regular to-one, so extract 
            // FK from PK of the target object
            DataObject target = (DataObject) targetObject;
            Map idParts = target.getObjectId().getIdSnapshot();

            // this may happen in uncommitted objects
            if (idParts.isEmpty()) {
                continue;
            }

            DbRelationship dbRel = (DbRelationship) rel.getDbRelationships().get(0);
            Map fk = dbRel.srcFkSnapshotWithTargetSnapshot(idParts);
            snapshot.putAll(fk);
        }

        // process object id map
        // we should ignore any object id values if a corresponding attribute
        // is a part of relationship "toMasterPK", since those values have been
        // set above when db relationships where processed.
        Map thisIdParts = anObject.getObjectId().getIdSnapshot();
        if (thisIdParts != null) {
            // put only thise that do not exist in the map
            Iterator itm = thisIdParts.keySet().iterator();
            while (itm.hasNext()) {
                Object nextKey = itm.next();
                if (!snapshot.containsKey(nextKey)) {
                    snapshot.put(nextKey, thisIdParts.get(nextKey));
                }
            }
        }
        return snapshot;
    }

    /** 
     * Takes a snapshot of current object state. 
     * 
     * @deprecated Since 1.1 use "currentSnapshot"
     */
    public Map takeObjectSnapshot(DataObject anObject) {
        return currentSnapshot(anObject);
    }

    /**
     * Creates a list of DataObjects local to this DataContext from a list 
     * of DataObjects coming from a different DataContext. Note that all objects
     * in the source list must be either in COMMITTED or in HOLLOW state.
     * 
     * @since 1.0.3
     */
    public List localObjects(List objects) {
        List localObjects = new ArrayList(objects.size());

        Iterator it = objects.iterator();
        while (it.hasNext()) {
            DataObject object = (DataObject) it.next();

            // sanity check
            if (object.getPersistenceState() != PersistenceState.COMMITTED
                && object.getPersistenceState() != PersistenceState.HOLLOW) {
                throw new CayenneRuntimeException(
                    "Only COMMITTED and HOLLOW objects can be transferred between contexts. "
                        + "Invalid object state '"
                        + PersistenceState.persistenceStateName(
                            object.getPersistenceState())
                        + "', ObjectId: "
                        + object.getObjectId());
            }

            DataObject localObject =
                (object.getDataContext() != this)
                    ? registeredObject(object.getObjectId())
                    : object;
            localObjects.add(localObject);
        }

        return localObjects;
    }

    /**
     * Converts a list of data rows to a list of DataObjects. 
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(ObjEntity entity, List dataRows, boolean refresh) {

        if (dataRows == null && dataRows.size() == 0) {
            return new ArrayList(1);
        }

        // do a sanity check on ObjEntity... if it's DbEntity has no PK defined,
        // we can't build a valid ObjectId
        DbEntity dbEntity = entity.getDbEntity();
        if (dbEntity == null) {
            throw new CayenneRuntimeException(
                "ObjEntity '" + entity.getName() + "' has no DbEntity.");
        }

        if (dbEntity.getPrimaryKey().size() == 0) {
            throw new CayenneRuntimeException(
                "Can't create ObjectId for '"
                    + entity.getName()
                    + "'. Reason: DbEntity '"
                    + dbEntity.getName()
                    + "' has no Primary Key defined.");
        }

        List results = new ArrayList(dataRows.size());
        Iterator it = dataRows.iterator();

        // must do double sync...
        synchronized (getObjectStore()) {
            synchronized (getObjectStore().getDataRowCache()) {
                while (it.hasNext()) {
                    DataRow dataRow = (DataRow) it.next();
                    ObjectId anId = dataRow.createObjectId(entity);

                    // this will create a HOLLOW object if it is not registered yet
                    DataObject object = registeredObject(anId);

                    // deal with object state
                    if (refresh) {
                        // make all COMMITTED objects HOLLOW
                        if (object.getPersistenceState() == PersistenceState.COMMITTED) {
                            // TODO: temporary hack - should do lazy conversion - make an object HOLLOW
                            // and resolve on first read... unfortunately lots of other things break...

                            DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                            // object.setPersistenceState(PersistenceState.HOLLOW);
                        }
                        // merge all MODIFIED objects immediately 
                        else if (
                            object.getPersistenceState() == PersistenceState.MODIFIED) {
                            DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                        }
                        // TODO: temporary hack - should do lazy conversion - keep an object HOLLOW
                        // and resolve on first read...unfortunately lots of other things break...
                        else if (
                            object.getPersistenceState() == PersistenceState.HOLLOW) {
                            DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                        }
                    }
                    // TODO: temporary hack - this else clause must go... unfortunately lots of other things break
                    // at the moment...
                    else {
                        if (object.getPersistenceState() == PersistenceState.HOLLOW) {
                            DataRowUtils.mergeObjectWithSnapshot(entity, object, dataRow);
                        }
                    }

                    object.setSnapshotVersion(dataRow.getVersion());
                    object.fetchFinished();
                    results.add(object);
                }

                // now deal with snapshots 
                getObjectStore().snapshotsUpdatedForObjects(results, dataRows, refresh);
            }
        }

        return results;
    }

    /**
     * Creates and returns a DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext.
     *
     * <p>Internally this method calls {@link 
     * #objectsFromDataRows(org.objectstyle.cayenne.map.ObjEntity,java.util.List,boolean)
     * objectsFromDataRows(ObjEntity, List, boolean)}
     * with <code>false</code> "refersh" parameter.</p>
     * 
     * @since 1.1
     */
    public List objectsFromDataRows(Class objectClass, List dataRows, boolean refresh) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objectClass);
        return objectsFromDataRows(entity, dataRows, false);
    }

    /**
     * A convenience shortcut to {@link 
     * #objectsFromDataRows(Class,java.util.List,boolean)
     * objectsFromDataRows(Class, List, boolean)}, that allows to easily create an object
     * from a map of values.</p>
     */
    public DataObject objectFromDataRow(
        Class objectClass,
        DataRow dataRow,
        boolean refresh) {
        List list =
            objectsFromDataRows(objectClass, Collections.singletonList(dataRow), refresh);
        return (DataObject) list.get(0);
    }

    /**
      * Use {@link 
      * #objectFromDataRow(Class,org.objectstyle.cayenne.DataRow,boolean)
      * objectFromDataRow(Class, DataRow, boolean)}.
      * 
      * @deprecated Since 1.1
      */
    public DataObject objectFromDataRow(String entityName, Map dataRow) {
        // backwards compatibility... wrap this in a DataRow
        if (!(dataRow instanceof DataRow)) {
            dataRow = new DataRow(dataRow);
        }

        ObjEntity ent = this.getEntityResolver().lookupObjEntity(entityName);
        List list = objectsFromDataRows(ent, Collections.singletonList(dataRow), false);
        return (DataObject) list.get(0);
    }

    /**
     * Use {@link 
     * #objectFromDataRow(Class,org.objectstyle.cayenne.DataRow,boolean)
     * objectFromDataRow(Class, DataRow, boolean)} instead.
     * 
     * @deprecated Since 1.1
     */
    public DataObject objectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {

        // backwards compatibility... wrap this in a DataRow
        if (!(dataRow instanceof DataRow)) {
            dataRow = new DataRow(dataRow);
        }

        List list =
            objectsFromDataRows(objEntity, Collections.singletonList(dataRow), refresh);
        return (DataObject) list.get(0);
    }

    /**
     * Creates and returns a read-only DataObject from a data row (snapshot).
     * Newly created object is registered with this DataContext. This method is not
     * used in Cayenne anymore. Use
     * {@link #objectsFromDataRows(org.objectstyle.cayenne.map.ObjEntity,java.util.List,boolean)
     * objectsFromDataRows(ObjEntity, List, boolean)} instead.
     * 
     * @deprecated Since 1.1 
     */
    protected DataObject readOnlyObjectFromDataRow(
        ObjEntity objEntity,
        Map dataRow,
        boolean refresh) {

        return this.objectFromDataRow(objEntity, dataRow, refresh);
    }

    /**
     * Instantiates new object and registers it with itself. Object class
     * is determined from ObjEntity. Object class must have a default constructor.
     * 
     * <p><i>Note: preferred way to create new objects is via 
     * {@link #createAndRegisterNewObject(Class)} method. It works exactly 
     * the same way, but makes the application type-safe.
     * </i></p>
     * 
     * @see #createAndRegisterNewObject(Class)
     */
    public DataObject createAndRegisterNewObject(String objEntityName) {
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(objEntityName);

        if (entity == null) {
            throw new IllegalArgumentException("Invalid entity name: " + objEntityName);
        }

        String objClassName = entity.getClassName();
        DataObject dataObject = null;
        try {
            dataObject = DataContext.newDataObject(objClassName);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObjectWithEntity(dataObject, entity);
        return dataObject;
    }

    /**
     * Instantiates new object and registers it with itself. Object class must have a 
     * default constructor.
     * 
     * @since 1.1
     */
    public DataObject createAndRegisterNewObject(Class objectClass) {
        if (objectClass == null) {
            throw new NullPointerException("DataObject class can't be null.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(objectClass);
        if (entity == null) {
            throw new IllegalArgumentException(
                "Class is not mapped with Cayenne: " + objectClass.getName());
        }

        DataObject dataObject = null;
        try {
            dataObject = (DataObject) objectClass.newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error instantiating object.", ex);
        }

        registerNewObjectWithEntity(dataObject, entity);
        return dataObject;
    }

    /** Registers a new object (that is not yet persistent) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     * @param objEntityName a name of the ObjEntity in the map used to get
     *  persistence information for this object.
     * 
     * @deprecated since 1.1 this method is deprecated. It is misleading to think that 
     * Cayenne supports more than one class per ObjEntity. Use {@link #registerNewObject(DataObject)}
     * instead.
     */
    public void registerNewObject(DataObject dataObject, String objEntityName) {
        ObjEntity entity = getEntityResolver().lookupObjEntity(objEntityName);
        if (entity == null) {
            throw new IllegalArgumentException(
                "Invalid ObjEntity name: " + objEntityName);
        }

        registerNewObjectWithEntity(dataObject, entity);
    }

    /** Registers a new object (that is not yet persistent) with itself.
     *
     * @param dataObject new object that we want to make persistent.
     */
    public void registerNewObject(DataObject dataObject) {
        if (dataObject == null) {
            throw new NullPointerException("Can't register null object.");
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(dataObject);
        if (entity == null) {
            throw new IllegalArgumentException(
                "Can't find ObjEntity for DataObject class: "
                    + dataObject.getClass().getName()
                    + ", class is likely not mapped.");
        }

        registerNewObjectWithEntity(dataObject, entity);
    }

    private void registerNewObjectWithEntity(
        DataObject dataObject,
        ObjEntity objEntity) {
        TempObjectId tempId = new TempObjectId(dataObject.getClass());
        dataObject.setObjectId(tempId);

        // initialize to-many relationships with a fault
        Iterator it = objEntity.getRelationships().iterator();
        while (it.hasNext()) {
            ObjRelationship rel = (ObjRelationship) it.next();
            if (rel.isToMany()) {
                dataObject.writePropertyDirectly(rel.getName(), Fault.getToManyFault());
            }
        }

        getObjectStore().addObject(dataObject);
        dataObject.setDataContext(this);
        dataObject.setPersistenceState(PersistenceState.NEW);
    }

    /**
     * @deprecated Since 1.1, use 
     * {@link #unregisterObjects(java.util.Collection) unregisterObjects(Collections.singletonList(dataObject))}
     * to invalidate a single object.
     */
    public void unregisterObject(DataObject dataObject) {
        unregisterObjects(Collections.singletonList(dataObject));
    }

    /**
     * Unregisters a Collection of DataObjects from the DataContext 
     * and the underlying ObjectStore. This operation also unsets 
     * DataContext and ObjectId for each object and changes its state 
     * to TRANSIENT.
     */
    public void unregisterObjects(Collection dataObjects) {
        getObjectStore().objectsUnregistered(dataObjects);
    }

    /**
     * @deprecated Since 1.1, use 
     * {@link #invalidateObjects(java.util.Collection) invalidateObjects(Collections.singletonList(dataObject))}
     * to invalidate a single object.
     */
    public void invalidateObject(DataObject dataObject) {
        invalidateObjects(Collections.singletonList(dataObject));
    }

    /**
      * "Invalidates" a Collection of DataObject. This operation would remove 
      * each object's snapshot from cache and change object's state to HOLLOW.
      * On the next access to this object, it will be refeched.
      */
    public void invalidateObjects(Collection dataObjects) {
        getObjectStore().objectsInvalidated(dataObjects);
    }

    /**
     * Notifies data context that a registered object need to be deleted on
     * next commit.
     *
     * @param anObject data object that we want to delete.
     */

    public void deleteObject(DataObject anObject) {
        if (anObject.getPersistenceState() == PersistenceState.DELETED
            || anObject.getPersistenceState() == PersistenceState.TRANSIENT) {

            // Drop out... especially in case of DELETED we might be about to get 
            // into a horrible
            // recursive loop due to CASCADE delete rules.
            // Assume that everything must have been done correctly already
            // and *don't* do it again
            return;
        }

        // must resolve HOLLOW objects before delete... Right now this is needed
        // to process relationships, but when we add optimistic locking, this will
        // be a requirement...
        anObject.resolveFault();

        // Save the current state in case of a deny, in which case it should be reset.
        // We cannot delay setting it to deleted, as Cascade deletes might cause
        // recursion, and the "deleted" state is the best way we have of noticing that and bailing out (see above)
        int oldState = anObject.getPersistenceState();
        int newState =
            (oldState == PersistenceState.NEW)
                ? PersistenceState.TRANSIENT
                : PersistenceState.DELETED;
        anObject.setPersistenceState(newState);

        //Do the right thing with all the relationships of the deleted object
        ObjEntity entity = this.getEntityResolver().lookupObjEntity(anObject);
        Iterator relationshipIterator = entity.getRelationships().iterator();
        while (relationshipIterator.hasNext()) {
            ObjRelationship relationship = (ObjRelationship) relationshipIterator.next();

            if (relationship.getDeleteRule() == DeleteRule.NO_ACTION) {
                continue;
            }

            String thisRelationshipName = relationship.getName();

            List relatedObjects = Collections.EMPTY_LIST;
            if (relationship.isToMany()) {

                List toMany = (List) anObject.readNestedProperty(relationship.getName());

                if (toMany.size() > 0) {
                    // Get a copy of the list so that deleting objects doesn't 
                    // result in concurrent modification exceptions
                    relatedObjects = new ArrayList(toMany);
                }
            }
            else {
                Object relatedObject = anObject.readNestedProperty(thisRelationshipName);

                if (relatedObject != null) {
                    relatedObjects = Collections.singletonList(relatedObject);
                }
            }

            if (relatedObjects.size() == 0) {
                continue;
            }

            switch (relationship.getDeleteRule()) {
                case DeleteRule.NULLIFY :
                    ObjRelationship inverseRelationship =
                        relationship.getReverseRelationship();
                    if (inverseRelationship == null) {
                        // with next relationship... nothing we can do here
                        continue;
                    }

                    if (inverseRelationship.isToMany()) {
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.removeToManyTarget(
                                inverseRelationship.getName(),
                                anObject,
                                true);
                        }
                    }
                    else {
                        // Inverse is to-one - find all related objects and
                        // nullify the reverse relationship
                        Iterator iterator = relatedObjects.iterator();
                        while (iterator.hasNext()) {
                            DataObject relatedObject = (DataObject) iterator.next();
                            relatedObject.setToOneTarget(
                                inverseRelationship.getName(),
                                null,
                                true);
                        }
                    }
                    break;
                case DeleteRule.CASCADE :
                    //Delete all related objects
                    Iterator iterator = relatedObjects.iterator();
                    while (iterator.hasNext()) {
                        DataObject relatedObject = (DataObject) iterator.next();
                        this.deleteObject(relatedObject);
                    }
                    break;
                case DeleteRule.DENY :
                    int relatedObjectsCount = relatedObjects.size();
                    if (relatedObjectsCount != 0) {
                        //Clean up - we shouldn't be deleting this object
                        anObject.setPersistenceState(oldState);
                        throw new DeleteDenyException(
                            "Cannot delete a "
                                + getEntityResolver().lookupObjEntity(anObject).getName()
                                + " because it has "
                                + relatedObjectsCount
                                + " object"
                                + (relatedObjectsCount > 1 ? "s" : "")
                                + "in it's "
                                + thisRelationshipName
                                + " relationship"
                                + " and this relationship has DENY "
                                + "as it's delete rule");
                    }
                    break;
                default :
                    //Clean up - we shouldn't be deleting this object
                    anObject.setPersistenceState(oldState);
                    throw new CayenneRuntimeException(
                        "Unknown type of delete rule " + relationship.getDeleteRule());
            }
        }

        // if an object was NEW, we must throw it out of the ObjectStore
        if (oldState == PersistenceState.NEW) {
            getObjectStore().objectsUnregistered(Collections.singletonList(anObject));
            anObject.setDataContext(null);
        }
    }

    /**
     * Refetches object data for ObjectId. This method is used
     * internally by Cayenne to resolve objects in state
     * <code>PersistenceState.HOLLOW</code>. It can also be used
     * to refresh certain objects.
     *
     * @throws CayenneRuntimeException if object id doesn't match
     * any records, or if there is more than one object is fetched.
     */
    public DataObject refetchObject(ObjectId oid) {

        synchronized (getObjectStore()) {
            DataObject object = objectStore.getObject(oid);

            // clean up any cached data for this object
            if (object != null) {
                this.invalidateObject(object);
            }
        }

        SelectQuery sel = QueryUtils.selectObjectForId(oid);
        List results = this.performQuery(sel);

        if (results.size() != 1) {
            String msg =
                (results.size() == 0)
                    ? "No matching objects found for ObjectId " + oid
                    : "More than 1 object found for ObjectId "
                        + oid
                        + ". Fetch matched "
                        + results.size()
                        + " objects.";

            throw new CayenneRuntimeException(msg);
        }

        return (DataObject) results.get(0);
    }
    

    /** 
     * Delegates node lookup to parent QueryEngine.
     * 
     * @deprecated Since 1.1 use {@link #lookupDataNode(DataMap)} since
     * queries are not necessarily based on an ObjEntity.
     */
    public DataNode dataNodeForObjEntity(ObjEntity objEntity) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().dataNodeForObjEntity(objEntity);
    }
    
    /**
     * Returns a DataNode that should hanlde queries for all
     * DataMap components.
     * 
     * @since 1.1
     */
    public DataNode lookupDataNode(DataMap dataMap) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().lookupDataNode(dataMap);
    }


    /**
     * Reverts any changes that have occurred to objects registered with DataContext. 
     */
    public void rollbackChanges() {
        getObjectStore().objectsRolledBack();
    }

    /**
     * Synchronizes object graph with the database. Executes needed
     * insert, update and delete queries (generated internally).
     */
    public void commitChanges() throws CayenneRuntimeException {
        commitChanges(null);
    }

    /**
     * Synchronizes object graph with the database. Executes needed
     * insert, update and delete queries (generated internally).
     *
     * @param logLevel if logLevel is higher or equals to the level
     * set for QueryLogger, statements execution will be logged.
     */
    public void commitChanges(Level logLevel) throws CayenneRuntimeException {

        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        // prevent multiple commits occuring simulteneously 
        synchronized (getObjectStore()) {
            // is there anything to do?
            if (!this.hasChanges()) {
                return;
            }

            if (isValidatingObjectsOnCommit()) {
                getObjectStore().validateUncommittedObjects();
            }

            ContextCommit worker = new ContextCommit(this);

            try {
                worker.commit(logLevel);
            }
            catch (CayenneException ex) {
                throw new CayenneRuntimeException(ex);
            }
        }
    }

    /**
     * Performs a single database select query.
     *
     * @return A list of DataObjects or a list of data rows
     * depending on the value returned by <code>query.isFetchingDataRows()</code>.
     */
    public List performQuery(GenericSelectQuery query) {

        // check if result pagination is requested
        // let a list handle fetch in this case
        if (query.getPageSize() > 0) {
            return new IncrementalFaultList(this, query);
        }

        SelectObserver observer = new SelectObserver(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);

        return (query.isFetchingDataRows())
            ? observer.getResults(query)
            : observer.getResultsAsObjects(this, query);
    }

    /**
     * Performs a single database select query returning result as a ResultIterator.
     * Returned ResultIterator will provide access to DataRows.
     */
    public ResultIterator performIteratedQuery(GenericSelectQuery query)
        throws CayenneException {

        IteratedSelectObserver observer = new IteratedSelectObserver();
        observer.setLoggingLevel(query.getLoggingLevel());
        performQueries(Collections.singletonList(query), observer);
        return observer.getResultIterator();
    }

    /** 
     * Delegates queries execution to parent QueryEngine. If there are select
     * queries that require prefetching relationships, will create additional
     * queries to perform necessary prefetching.
     */
    public void performQueries(Collection queries, OperationObserver observer) {
        // note - use external transaction for iterated queries;
        // other types of transactions won't be safe in this case
        Transaction transaction =
            (observer.isIteratedResult())
                ? Transaction.externalTransaction(
                    getParentDataDomain().getTransactionDelegate())
                : getParentDataDomain().createTransaction();

        transaction.performQueries(this, queries, observer);
    }

    /**
     * Delegates queries execution to parent QueryEngine. If there are select
     * queries that require prefetching relationships, will create additional
     * queries to perform necessary prefetching.
     * 
     * @since 1.1
     */
    public void performQueries(
        Collection queries,
        OperationObserver resultConsumer,
        Transaction transaction) {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }

        DataContextDelegate localDelegate = nonNullDelegate();
        List finalQueries = new ArrayList(queries.size());
        boolean hasPrefetches = false;

        Iterator it = queries.iterator();
        while (it.hasNext()) {
            Object query = it.next();

            if (query instanceof GenericSelectQuery) {
                GenericSelectQuery genericSelect = (GenericSelectQuery) query;

                // filter via a delegate
                GenericSelectQuery filteredSelect =
                    localDelegate.willPerformSelect(this, genericSelect);

                // suppressed by the delegate
                if (filteredSelect != null) {
                    finalQueries.add(filteredSelect);

                    // check if prefetching is required
                    if (!hasPrefetches && (filteredSelect instanceof SelectQuery)) {
                        hasPrefetches =
                            !((SelectQuery) filteredSelect).getPrefetches().isEmpty();
                    }
                }
            }
            else {
                finalQueries.add(query);
            }
        }

        if (!resultConsumer.isIteratedResult() && hasPrefetches) {
            // do a second pass to add prefetches (prefetches must go after all main queries)
            it = queries.iterator();
            while (it.hasNext()) {
                SelectQuery select = (SelectQuery) it.next();
                Collection prefetchRels = select.getPrefetches();
                if (prefetchRels.size() > 0) {
                    Iterator prIt = prefetchRels.iterator();
                    while (prIt.hasNext()) {
                        PrefetchSelectQuery prefetchQuery =
                            QueryUtils.selectPrefetchPath(
                                this,
                                select,
                                (String) prIt.next());

                        // filter via a delegate
                        GenericSelectQuery filteredPrefetch =
                            localDelegate.willPerformSelect(this, prefetchQuery);

                        // if not suppressed by delegate
                        if (filteredPrefetch != null) {
                            finalQueries.add(filteredPrefetch);
                        }
                    }
                }
            }
        }

        this.getParent().performQueries(finalQueries, resultConsumer, transaction);
    }

    /**
     * Performs prefetching. Prefetching would resolve a set of relationships
     * for a list of DataObjects in the most optimized way (preferrably in
     * a single query per relationship).
     *
     * <p><i>WARNING: Currently supports only "one-step" to one relationships. This is an
     * arbitrary limitation and will be removed eventually.</i></p>
     */
    public void prefetchRelationships(SelectQuery query, List objects) {
        Collection prefetches = query.getPrefetches();

        if (objects == null || objects.size() == 0 || prefetches.size() == 0) {
            return;
        }

        ObjEntity entity = getEntityResolver().lookupObjEntity(query);
        Iterator prefetchesIt = prefetches.iterator();
        while (prefetchesIt.hasNext()) {
            String prefetchKey = (String) prefetchesIt.next();
            if (prefetchKey.indexOf(Entity.PATH_SEPARATOR) >= 0) {
                throw new CayenneRuntimeException(
                    "Only one-step relationships are "
                        + "supported at the moment, this will be fixed soon. "
                        + "Unsupported path : "
                        + prefetchKey);
            }

            ObjRelationship relationship =
                (ObjRelationship) entity.getRelationship(prefetchKey);
            if (relationship == null) {
                throw new CayenneRuntimeException("Invalid relationship: " + prefetchKey);
            }

            if (relationship.isToMany()) {
                throw new CayenneRuntimeException(
                    "Only to-one relationships are supported at the moment. "
                        + "Can't prefetch to-many: "
                        + prefetchKey);
            }

            PrefetchHelper.resolveToOneRelations(this, objects, prefetchKey);
        }

    }

    /** 
     * Delegates query execution to parent QueryEngine.
     *  
     * @deprecated Since 1.1 use performQueries(List, OperationObserver).
     * This method is redundant and doesn't add value.
     */
    public void performQuery(Query query, OperationObserver operationObserver) {
        this.performQueries(Collections.singletonList(query), operationObserver);
    }

    /**
     *  Populates the <code>map</code> with ObjectId values from master objects
     *  related to this object.
     */
    private void appendPkFromMasterRelationships(Map map, DataObject dataObject) {
        ObjEntity objEntity = this.getEntityResolver().lookupObjEntity(dataObject);
        DbEntity dbEntity = objEntity.getDbEntity();

        Iterator it = dbEntity.getRelationshipMap().values().iterator();
        while (it.hasNext()) {
            DbRelationship dbRel = (DbRelationship) it.next();
            if (!dbRel.isToMasterPK()) {
                continue;
            }

            ObjRelationship rel = objEntity.getRelationshipForDbRelationship(dbRel);
            if (rel == null) {
                continue;
            }

            DataObject targetDo =
                (DataObject) dataObject.readPropertyDirectly(rel.getName());
            if (targetDo == null) {
                // this is bad, since we will not be able to obtain PK in any other way
                // throw an exception
                throw new CayenneRuntimeException("Null master object, can't create primary key.");
            }

            Map idMap = targetDo.getObjectId().getIdSnapshot();
            if (idMap == null) {
                // this is bad, since we will not be able to obtain PK in any other way
                // provide a detailed error message
                StringBuffer msg =
                    new StringBuffer("Can't create primary key, master object has no PK snapshot.");
                msg
                    .append("\nrelationship name: ")
                    .append(dbRel.getName())
                    .append(", src object: ")
                    .append(dataObject.getObjectId().getObjClass().getName())
                    .append(", target obj: ")
                    .append(targetDo.getObjectId().getObjClass().getName());
                throw new CayenneRuntimeException(msg.toString());
            }

            map.putAll(dbRel.srcFkSnapshotWithTargetSnapshot(idMap));
        }
    }

    /** Creates permanent ObjectId for <code>anObject</code>.
     *  Object must already have a temporary ObjectId.
     *
     *  <p>This method is called when we are about to save a new object to
     *  the database. Primary key columns are populated assigning values
     *  in the following sequence:
     *  <ul>
     *     <li>Object attribute values are used.</li>
     *     <li>Values from ObjectId's propagated from master relationshop
     *     are used. <i>If master object does not have a permanent id
     *     created yet, an exception is thrown.</i></li>
     *     <li>Values generated from the database provided by DbAdapter.
     *     <i>Autogeneration only works for a single column. If more than
     *     one column requires an autogenerated primary key, an exception is
     *     thrown</i></li>
     *   </ul>
     *
     *   @return Newly created ObjectId.
     * 
     * @deprecated Since 1.1 this method is no longer used.
     */
    public ObjectId createPermId(DataObject anObject) throws CayenneRuntimeException {
        ObjectId id = anObject.getObjectId();
        if (!(id instanceof TempObjectId)) {
            return id;
            //If the id is not a temp, then it must be permanent.  Return it and do nothing else
        }

        if (id.getReplacementId() != null) {
            return id.getReplacementId();
        }

        ObjEntity objEntity = this.getEntityResolver().lookupObjEntity(id.getObjClass());
        DbEntity dbEntity = objEntity.getDbEntity();
        DataNode aNode = this.dataNodeForObjEntity(objEntity);

        Map idMap = new HashMap();
        // first get values delivered via relationships
        appendPkFromMasterRelationships(idMap, anObject);

        boolean autoPkDone = false;
        Iterator it = dbEntity.getPrimaryKey().iterator();
        while (it.hasNext()) {
            DbAttribute attr = (DbAttribute) it.next();

            // see if it is there already
            if (idMap.get(attr.getName()) != null) {
                continue;
            }

            // try object value as PK
            ObjAttribute objAttr = objEntity.getAttributeForDbAttribute(attr);
            if (objAttr != null) {
                idMap.put(
                    attr.getName(),
                    anObject.readPropertyDirectly(objAttr.getName()));
                continue;
            }

            // run PK autogeneration
            if (autoPkDone) {
                throw new CayenneRuntimeException("Primary Key autogeneration only works for a single attribute.");
            }

            try {
                PkGenerator gen = aNode.getAdapter().getPkGenerator();
                Object pk = gen.generatePkForDbEntity(aNode, objEntity.getDbEntity());
                autoPkDone = true;
                idMap.put(attr.getName(), pk);
            }
            catch (Exception ex) {
                throw new CayenneRuntimeException("Error generating PK", ex);
            }
        }

        ObjectId permId = new ObjectId(anObject.getClass(), idMap);

        // note that object registration did not change (new id is not attached to context, only to temp. oid)
        id.setReplacementId(permId);
        return permId;
    }

    // serialization support
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();

        // If the "parent" of this datacontext is a DataDomain, then just write the
        // name of it.  Then when deserialization happens, we can get back the DataDomain by name,
        // from the shared configuration (which will either load it if need be, or return
        // an existing one.

        if (this.parent == null && this.lazyInitParentDomainName != null) {
            out.writeObject(lazyInitParentDomainName);
        }
        else if (this.parent instanceof DataDomain) {
            DataDomain domain = (DataDomain) this.parent;
            out.writeObject(domain.getName());
        }
        else {
            // Hope that whatever this.parent is, that it is Serializable
            out.writeObject(this.parent);
        }

        // Serialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            out.writeObject(objectStore.getDataRowCache());
        }
    }

    //serialization support
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {

        // 1. read non-transient properties
        in.defaultReadObject();

        // 2. read parent or its name
        Object value = in.readObject();
        if (value instanceof QueryEngine) {
            // A real QueryEngine object - use it
            this.parent = (QueryEngine) value;
        }
        else if (value instanceof String) {
            // The name of a DataDomain - use it
            this.lazyInitParentDomainName = (String) value;
        }
        else {
            throw new CayenneRuntimeException(
                "Parent attribute of DataContext was neither a QueryEngine nor "
                    + "the name of a valid DataDomain:"
                    + value);
        }

        // 3. Deserialize local snapshots cache
        if (!isUsingSharedSnapshotCache()) {
            DataRowStore cache = (DataRowStore) in.readObject();
            objectStore.setDataRowCache(cache);
        }

        // initialized new relationship datasource
        this.relationshipDataSource = new RelationshipDataSource(this);

        // CayenneDataObjects have a transient datacontext
        // because at deserialize time the datacontext may need to be different
        // than the one at serialize time (for programmer defined reasons).
        // So, when a dataobject is resurrected because it's datacontext was
        // serialized, it will then set the objects datacontext to the correctone
        // If deserialized "otherwise", it will not have a datacontext (good)

        synchronized (getObjectStore()) {
            Iterator it = objectStore.getObjectIterator();
            while (it.hasNext()) {
                DataObject object = (DataObject) it.next();
                object.setDataContext(this);
            }
        }
    }

    /**
     * Returns EntityResolver object used to resolve and route queries.
     */
    public EntityResolver getEntityResolver() {
        if (this.getParent() == null) {
            throw new CayenneRuntimeException("Cannot use a DataContext without a parent");
        }
        return this.getParent().getEntityResolver();
    }

    /**
     * Sets default for posting transaction events by new DataContexts.
     */
    public static void setTransactionEventsEnabledDefault(boolean flag) {
        transactionEventsEnabledDefault = flag;
    }

    /**
     * Enables or disables posting of transaction events by this DataContext.
     */
    public void setTransactionEventsEnabled(boolean flag) {
        this.transactionEventsEnabled = flag;
    }

    public boolean isTransactionEventsEnabled() {
        return this.transactionEventsEnabled;
    }

    /**
     * Returns <code>true</code> if the ObjectStore uses
     * shared cache of a parent DataDomain.
     * 
     * @since 1.1
     */
    public boolean isUsingSharedSnapshotCache() {
        return usingSharedSnaphsotCache;
    }

    /**
     * Returns whether this DataContext performs object validation before
     * commit is executed.
     * 
     * @since 1.1
     */
    public boolean isValidatingObjectsOnCommit() {
        return validatingObjectsOnCommit;
    }

    /**
     * Sets the property defining whether this DataContext should perform 
     * object validation before commit is executed.
     * 
     * @since 1.1
     */
    public void setValidatingObjectsOnCommit(boolean flag) {
        this.validatingObjectsOnCommit = flag;
    }

    public Collection getDataMaps() {
        return (parent != null) ? parent.getDataMaps() : Collections.EMPTY_LIST;
    }

    void fireWillCommit() {
        // post event: WILL_COMMIT
        if (this.transactionEventsEnabled) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.WILL_COMMIT);
        }
    }

    void fireTransactionRolledback() {
        // post event: DID_ROLLBACK
        if ((this.transactionEventsEnabled)) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.DID_ROLLBACK);
        }
    }

    void fireTransactionCommitted() {
        // post event: DID_COMMIT
        if ((this.transactionEventsEnabled)) {
            EventManager eventMgr = EventManager.getDefaultManager();
            DataContextEvent commitChangesEvent = new DataContextEvent(this);
            eventMgr.postEvent(commitChangesEvent, DataContext.DID_COMMIT);
        }
    }

    /**
     * @deprecated Since 1.1 this method is not used in Cayenne. 
     * All flattened relationship logic was moved to the ObjectStore
     */
    protected void clearFlattenedUpdateQueries() {
        objectStore.flattenedDeletes.clear();
        objectStore.flattenedInserts.clear();
    }

    /**
     * @deprecated Since 1.1 this method is not used in Cayenne. 
     * All flattened relationship logic was moved to the ObjectStore
     */
    public void registerFlattenedRelationshipDelete(
        DataObject source,
        ObjRelationship relationship,
        DataObject destination) {
        objectStore.flattenedRelationshipUnset(source, relationship, destination);
    }

    /**
     * @deprecated Since 1.1 this method is not used in Cayenne. 
     * All flattened relationship logic was moved to the ObjectStore
     */
    public void registerFlattenedRelationshipInsert(
        DataObject source,
        ObjRelationship relationship,
        DataObject destination) {
        objectStore.flattenedRelationshipSet(source, relationship, destination);
    }
}

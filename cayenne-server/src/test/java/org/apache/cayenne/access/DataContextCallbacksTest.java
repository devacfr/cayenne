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
package org.apache.cayenne.access;

import org.apache.cayenne.configuration.server.ServerRuntime;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.reflect.LifecycleCallbackRegistry;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.testing.CayenneConfiguration;
import org.apache.cayenne.unit.di.server.ServerCase;
import org.junit.Test;

@CayenneConfiguration(ServerCase.TESTMAP_PROJECT)
public class DataContextCallbacksTest extends ServerCase {

    @Inject
    private DataContext context;

    @Inject
    private ServerRuntime runtime;

    @Inject
    private DBHelper dbHelper;

    @Override
    public void setUp() throws Exception {
    	super.setUp();
        dbHelper.deleteAll("PAINTING_INFO");
        dbHelper.deleteAll("PAINTING");
        dbHelper.deleteAll("ARTIST_EXHIBIT");
        dbHelper.deleteAll("ARTIST_GROUP");
        dbHelper.deleteAll("ARTIST");
    }

    @Override
    public void tearDown() throws Exception {
    	super.tearDown();
        EntityResolver resolver = runtime.getDataDomain().getEntityResolver();
        resolver.getCallbackRegistry().clear();
    }

    @Test
    public void testPostAddCallbacks() {
        LifecycleCallbackRegistry registry = runtime
                .getDataDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        // no callbacks
        Artist a1 = context.newObject(Artist.class);
        assertNotNull(a1);
        assertFalse(a1.isPostAdded());

        registry.addCallback(LifecycleEvent.POST_ADD, Artist.class, "postAddCallback");

        Artist a2 = context.newObject(Artist.class);
        assertNotNull(a2);
        assertTrue(a2.isPostAdded());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.POST_ADD,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a3 = context.newObject(Artist.class);
        assertNotNull(a3);
        assertTrue(a3.isPostAdded());

        assertSame(a3, listener2.getPublicCalledbackEntity());

        Painting p3 = context.newObject(Painting.class);
        assertNotNull(p3);
        assertFalse(p3.isPostAdded());
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void testPrePersistCallbacks() {
        LifecycleCallbackRegistry registry = runtime
                .getDataDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        // no callbacks
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("1");
        assertFalse(a1.isPrePersisted());
        context.commitChanges();
        assertFalse(a1.isPrePersisted());

        registry.addCallback(
                LifecycleEvent.PRE_PERSIST,
                Artist.class,
                "prePersistCallback");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("2");
        assertFalse(a2.isPrePersisted());
        context.commitChanges();
        assertTrue(a2.isPrePersisted());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.PRE_PERSIST,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("3");
        assertNull(listener2.getPublicCalledbackEntity());
        context.commitChanges();
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }

    @Test
    public void testPreRemoveCallbacks() {
        LifecycleCallbackRegistry registry = runtime
                .getDataDomain()
                .getEntityResolver()
                .getCallbackRegistry();

        // no callbacks
        Artist a1 = context.newObject(Artist.class);
        a1.setArtistName("XX");
        context.commitChanges();
        context.deleteObjects(a1);
        assertFalse(a1.isPostAdded());
        assertFalse(a1.isPreRemoved());

        registry
                .addCallback(LifecycleEvent.PRE_REMOVE, Artist.class, "preRemoveCallback");

        Artist a2 = context.newObject(Artist.class);
        a2.setArtistName("XX");
        context.commitChanges();
        context.deleteObjects(a2);
        assertFalse(a2.isPostAdded());
        assertTrue(a2.isPreRemoved());

        MockCallingBackListener listener2 = new MockCallingBackListener();
        registry.addListener(
                LifecycleEvent.PRE_REMOVE,
                Artist.class,
                listener2,
                "publicCallback");

        Artist a3 = context.newObject(Artist.class);
        a3.setArtistName("XX");
        context.commitChanges();
        context.deleteObjects(a3);
        assertFalse(a3.isPostAdded());
        assertTrue(a3.isPreRemoved());

        assertSame(a3, listener2.getPublicCalledbackEntity());

        Painting p3 = context.newObject(Painting.class);
        p3.setPaintingTitle("XX");
        context.commitChanges();
        context.deleteObjects(p3);
        assertFalse(p3.isPostAdded());
        assertFalse(p3.isPreRemoved());
        assertSame(a3, listener2.getPublicCalledbackEntity());
    }
}
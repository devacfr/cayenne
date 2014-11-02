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
package org.apache.cayenne.testing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.cayenne.testing.support.AbstractTestExecutionListener;
import org.apache.cayenne.testing.support.DependencyInjectionTestExecutionListener;
import org.apache.cayenne.testing.support.DirtiesRuntimeTestExecutionListener;
import org.junit.Test;

/**
 * Unit tests for the {@link TestExecutionListeners @TestExecutionListeners}
 * annotation, which verify:
 * <ul>
 * <li>Proper registering of {@link TestExecutionListener listeners} in
 * conjunction with a {@link CayenneTestContextManager}</li>
 * <li><em>Inheritance</em></li>
 * </ul>
 *
 * @since 4.0
 */
public class TestExecutionListenersTest extends TestCase {

    private List<Class<?>> classes(CayenneTestContextManager testContextManager) {
        List<Class<?>> l = new ArrayList<Class<?>>();
        for (TestExecutionListener listener : testContextManager.getTestExecutionListeners()) {
            l.add(listener.getClass());
        }
        return l;
    }

    private List<String> names(List<Class<?>> classes) {
        List<String> l = new ArrayList<String>();
        for (Class<?> cl : classes) {
            l.add(cl.getSimpleName());
        }
        return l;
    }

    private void assertRegisteredListeners(Class<?> testClass, List<Class<?>> expected) {
        CayenneTestContextManager testContextManager = new CayenneTestContextManager(testClass);
        assertEquals("Listeners registered for " + testClass.getSimpleName(),
            names(expected),
            names(classes(testContextManager)));
    }

    @Test
    public void defaultListeners() {
        List<Class<?>> expected =
                Arrays.<Class<?>> asList(DependencyInjectionTestExecutionListener.class,
                    DirtiesRuntimeTestExecutionListener.class);
        assertRegisteredListeners(DefaultListenersTestCase.class, expected);
    }

    @Test
    public void nonInheritedDefaultListeners() {
        assertRegisteredListeners(NonInheritedDefaultListenersTestCase.class,
            Arrays.<Class<?>> asList(XyzTestExecutionListener.class));
    }

    @Test
    public void inheritedDefaultListeners() {
        assertRegisteredListeners(InheritedDefaultListenersTestCase.class,
            Arrays.<Class<?>> asList(XyzTestExecutionListener.class));
        assertRegisteredListeners(SubInheritedDefaultListenersTestCase.class,
            Arrays.<Class<?>> asList(XyzTestExecutionListener.class));
        assertRegisteredListeners(SubSubInheritedDefaultListenersTestCase.class,
            Arrays.<Class<?>> asList(XyzTestExecutionListener.class, AbcTestExecutionListener.class));
    }

    @Test
    public void customListeners() {
        CayenneTestContextManager testContextManager = new CayenneTestContextManager(ExplicitListenersTestCase.class);
        assertEquals("Number registered Listeners for ExplicitListenersTestCase.",
            3,
            testContextManager.getTestExecutionListeners().size());
    }

    @Test
    public void nonInheritedListeners() {
        CayenneTestContextManager testContextManager =
                new CayenneTestContextManager(NonInheritedListenersTestCase.class);
        assertEquals("Number registered Listeners for NonInheritedListenersTestCase.",
            1,
            testContextManager.getTestExecutionListeners().size());
    }

    @Test
    public void inheritedListeners() {
        CayenneTestContextManager testContextManager = new CayenneTestContextManager(InheritedListenersTestCase.class);
        assertEquals("Number registered Listeners for InheritedListenersTestCase.",
            4,
            testContextManager.getTestExecutionListeners().size());
    }

    @Test(expected = IllegalStateException.class)
    public void listenersAndValueAttributesDeclared() {
        new CayenneTestContextManager(DuplicateListenersConfigTestCase.class);
    }

    // -------------------------------------------------------------------

    static class DefaultListenersTestCase {
    }

    @TestExecutionListeners(XyzTestExecutionListener.class)
    static class InheritedDefaultListenersTestCase extends DefaultListenersTestCase {
    }

    static class SubInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
    }

    @TestExecutionListeners(AbcTestExecutionListener.class)
    static class SubSubInheritedDefaultListenersTestCase extends SubInheritedDefaultListenersTestCase {
    }

    @TestExecutionListeners(listeners = { XyzTestExecutionListener.class }, inheritListeners = false)
    static class NonInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
    }

    @TestExecutionListeners({ FooTestExecutionListener.class, BarTestExecutionListener.class,
        BazTestExecutionListener.class })
    static class ExplicitListenersTestCase {
    }

    @TestExecutionListeners(XyzTestExecutionListener.class)
    static class InheritedListenersTestCase extends ExplicitListenersTestCase {
    }

    @TestExecutionListeners(listeners = XyzTestExecutionListener.class, inheritListeners = false)
    static class NonInheritedListenersTestCase extends InheritedListenersTestCase {
    }

    @TestExecutionListeners(listeners = FooTestExecutionListener.class, value = BarTestExecutionListener.class)
    static class DuplicateListenersConfigTestCase {
    }

    static class FooTestExecutionListener extends AbstractTestExecutionListener {
    }

    static class BarTestExecutionListener extends AbstractTestExecutionListener {

    }

    static class BazTestExecutionListener extends AbstractTestExecutionListener {

    }

    static class XyzTestExecutionListener extends AbstractTestExecutionListener {

    }

    static class AbcTestExecutionListener extends AbstractTestExecutionListener {
    }

}
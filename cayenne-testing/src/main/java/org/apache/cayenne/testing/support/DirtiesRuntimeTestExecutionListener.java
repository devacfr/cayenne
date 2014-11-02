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

package org.apache.cayenne.testing.support;

import java.lang.reflect.Method;

import org.apache.cayenne.testing.CayenneTestContext;
import org.apache.cayenne.testing.ClassMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@code DirtiesRuntimeTestExecutionListener} which provides support for
 * marking the {@code CayenneRuntime} associated with a test as <em>dirty</em>
 * for both test classes and test methods configured with the {@link DirtyMode
 * @DirtyMode} annotation.
 *
 * @since 4.0
 *
 */
public class DirtiesRuntimeTestExecutionListener extends AbstractTestExecutionListener {

    private static final Log logger = LogFactory.getLog(DirtiesRuntimeTestExecutionListener.class);

    /**
     * Marks the {@link CayenneRuntime Cayenne Runtime} of the supplied
     * {@link CayenneTestContext test context} as
     * {@link CayenneTestContext#markCayenneRuntimeDirty() dirty}, and sets the
     * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
     * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context to {@code true}.
     *
     * @param testContext
     *            a test context
     */
    protected void dirtyContext(CayenneTestContext testContext) {
        testContext.markCayenneRuntimeDirty();
        testContext.setAttribute(DependencyInjectionTestExecutionListener.REINJECT_DEPENDENCIES, Boolean.TRUE);
    }

    /**
     * If the current test method of the supplied {@link CayenneTestContext test
     * context} is annotated with {@link DirtyMode &#064;DirtyMode}, or if the
     * test class is annotated with {@link DirtyMode &#064;DirtyMode} and the
     * {@link DirtyMode#classMode() class mode} is set to
     * {@link ClassMode#AfterTestMethod AfterTestMethod} , the
     * {@link CayenneRuntime Cayenne Runtime} of the test context will be
     * {@link CayenneTestContext#markCayenneRuntimeDirty() marked as dirty} and
     * the
     * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
     * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
     * {@code true}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterTestMethod(CayenneTestContext testContext) throws Exception {
        Class<?> testClass = testContext.getTestClass();
        Method testMethod = testContext.getTestMethod();

        final Class<DirtyMode> annotationType = DirtyMode.class;

        boolean methodDirtyMode = testMethod.isAnnotationPresent(annotationType);
        boolean classDirtyMode = testClass.isAnnotationPresent(annotationType);
        DirtyMode classDirtiesContextAnnotation = testClass.getAnnotation(annotationType);
        ClassMode classMode = classDirtyMode ? classDirtiesContextAnnotation.classMode() : null;

        if (logger.isDebugEnabled()) {
            logger.debug("After test method: context [" + testContext + "], class dirties context [" + classDirtyMode
                + "], class mode [" + classMode + "], method dirties context [" + methodDirtyMode + "].");
        }

        if (methodDirtyMode || classDirtyMode && classMode == ClassMode.AfterTestMethod) {
            dirtyContext(testContext);
        }
    }

    /**
     * If the test class of the supplied {@link CayenneTestContext test context}
     * is annotated with {@link DirtyMode &#064;DirtyMode}, the
     * {@link CayenneRuntime Cayenne Runtime} of the test context will be
     * {@link CayenneTestContext#markCayenneRuntimeDirty() marked as dirty} ,
     * and the
     * {@link DependencyInjectionTestExecutionListener#REINJECT_DEPENDENCIES_ATTRIBUTE
     * REINJECT_DEPENDENCIES_ATTRIBUTE} in the test context will be set to
     * {@code true}.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void afterTestClass(CayenneTestContext testContext) throws Exception {
        Class<?> testClass = testContext.getTestClass();

        boolean dirtiesRuntime = testClass.isAnnotationPresent(DirtyMode.class);
        if (logger.isDebugEnabled()) {
            logger.debug("After test class: runtime [" + testContext + "], dirtiesRuntime [" + dirtiesRuntime + "].");
        }
        if (dirtiesRuntime) {
            dirtyContext(testContext);
        }
    }

}

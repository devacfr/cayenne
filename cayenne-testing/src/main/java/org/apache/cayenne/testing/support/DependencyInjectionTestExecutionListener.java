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

import org.apache.cayenne.di.Injector;
import org.apache.cayenne.testing.CayenneTestContext;
import org.apache.cayenne.testing.ClassMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * {@code DependencyInjectionTestExecutionListener} provides support for
 * dependency injection and initialization of test instances.
 *
 * @since 4.0
 *
 */
public class DependencyInjectionTestExecutionListener extends AbstractTestExecutionListener {

    public static final String REINJECT_DEPENDENCIES = DependencyInjectionTestExecutionListener.class.getName()
            + "reinjectDependencies";

    private static final Log logger = LogFactory.getLog(DependencyInjectionTestExecutionListener.class);

    /**
     * Injects dependencies after creation of test class instance.
     * <p>
     * {@inheritDoc}
     */
    @Override
    public void prepareTestInstance(final CayenneTestContext testContext) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Performing dependency injection for test context [" + testContext + "].");
        }
        injectDependencies(testContext);
    }

    /**
     * Injects dependencies in current test instance before test method call
     * whether {@link InjectMode &#064;InjectMode} annotation is present on
     * current method of test class and the {@link InjectMode#classMode() class
     * mode} is set to {@link ClassMode#AfterTestMethod AfterTestMethod}.
     * <p>
     * {@inheritDoc}
     *
     * @see InjectMode
     */
    @Override
    public void afterTestMethod(CayenneTestContext testContext) throws Exception {
        Class<?> classTest = testContext.getTestClass();
        InjectMode injectMode = classTest.getAnnotation(InjectMode.class);
        if (injectMode == null) {
            injectMode = testContext.getTestMethod().getAnnotation(InjectMode.class);
        } else if (ClassMode.AfterClass.equals(injectMode.classMode())) {
            injectMode = null;
        }
        if (injectMode != null || Boolean.TRUE.equals(testContext.getAttribute(REINJECT_DEPENDENCIES))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Reinjecting dependencies for test context [" + testContext + "].");
            }
            injectDependencies(testContext);
        }
    }

    /**
     * Injects member in test class instance.
     *
     * @param testContext
     *            a test context.
     * @throws Exception
     *             allows any exception to propagate
     */
    protected void injectDependencies(final CayenneTestContext testContext) throws Exception {
        Object bean = testContext.getTestInstance();
        Injector injector = testContext.getCayenneRuntime().getInjector();
        injector.injectMembers(bean);
    }

}
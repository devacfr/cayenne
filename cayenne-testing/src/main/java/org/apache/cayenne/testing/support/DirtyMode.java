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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.cayenne.testing.ClassMode;

/**
 * This annotation associated to {@link DirtiesRuntimeTestExecutionListener}
 * provides support for marking the {@code CayenneRuntime} associated with a
 * test as <em>dirty</em> for both test classes and test methods configured with
 * the {@code @DirtyMode} annotation.
 *
 * @since 4.0
 *
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DirtyMode {

    /**
     * The <i>mode</i> to use when a test class is annotated with
     * {@code @DirtyMode}</code>.
     * <p>
     * Defaults to {@link ClassMode#AfterClass AfterClass}.
     * <p>
     * Note: Setting the class mode on an annotated test method has no meaning,
     * since the mere presence of the {@code DirtyMode} annotation on a test
     * method is sufficient.
     */
    ClassMode classMode() default ClassMode.AfterClass;

}
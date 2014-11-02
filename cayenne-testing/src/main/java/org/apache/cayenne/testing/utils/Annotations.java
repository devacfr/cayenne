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

package org.apache.cayenne.testing.utils;

import java.lang.annotation.Annotation;
import java.util.Arrays;

/**
 *
 * @since 4.0
 */
public abstract class Annotations {

    /**
     * Find a single {@link Annotation} of {@code annotationType} from the
     * supplied {@link Class}, traversing its interfaces and superclasses if no
     * annotation can be found on the given class itself.
     *
     * @param clazz
     *            the class to look for annotations on
     * @param annotationType
     *            the annotation class to look for
     * @return the annotation found, or {@code null} if none found
     */
    public static <A extends Annotation> A findAnnotation(Class<?> clazz, Class<A> annotationType) {
        Assert.notNull(clazz, "Class must not be null");
        A annotation = clazz.getAnnotation(annotationType);
        if (annotation != null) {
            return annotation;
        }
        for (Class<?> ifc : clazz.getInterfaces()) {
            annotation = findAnnotation(ifc, annotationType);
            if (annotation != null) {
                return annotation;
            }
        }
        if (!Annotation.class.isAssignableFrom(clazz)) {
            for (Annotation ann : clazz.getAnnotations()) {
                annotation = findAnnotation(ann.annotationType(), annotationType);
                if (annotation != null) {
                    return annotation;
                }
            }
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass == null || superClass == Object.class) {
            return null;
        }
        return findAnnotation(superClass, annotationType);
    }

    /**
     * Find the first {@link Class} in the inheritance hierarchy of the
     * specified {@code clazz} (including the specified {@code clazz} itself)
     * which declares an annotation for the specified {@code annotationType}, or
     * {@code null} if not found. If the supplied {@code clazz} is {@code null},
     * {@code null} will be returned.
     *
     * @param annotationType
     *            the Class object corresponding to the annotation type
     * @param clazz
     *            the Class object corresponding to the class on which to check
     *            for the annotation, or {@code null}
     * @return the first {@link Class} in the inheritance hierarchy of the
     *         specified {@code clazz} which declares an annotation for the
     *         specified {@code annotationType}, or {@code null} if not found
     * @see Class#isAnnotationPresent(Class)
     * @see Class#getDeclaredAnnotations()
     */
    public static Class<?> findAnnotationDeclaringClass(Class<? extends Annotation> annotationType, Class<?> clazz) {
        if (clazz == null || clazz.equals(Object.class)) {
            return null;
        }
        return isAnnotationDeclaredLocally(annotationType, clazz) ? clazz
                : findAnnotationDeclaringClass(annotationType, clazz.getSuperclass());
    }

    /**
     * Determine whether an annotation for the specified {@code annotationType}
     * is declared locally on the supplied {@code clazz}. The supplied
     * {@link Class} may represent any type.
     *
     * @param annotationType
     *            the Class object corresponding to the annotation type
     * @param clazz
     *            the Class object corresponding to the class on which to check
     *            for the annotation
     * @return {@code true} if an annotation for the specified
     *         {@code annotationType} is declared locally on the supplied
     *         {@code clazz}.
     * @see Class#getDeclaredAnnotations()
     */
    public static boolean isAnnotationDeclaredLocally(Class<? extends Annotation> annotationType, Class<?> clazz) {
        boolean declaredLocally = false;
        for (Annotation annotation : Arrays.asList(clazz.getDeclaredAnnotations())) {
            if (annotation.annotationType().equals(annotationType)) {
                declaredLocally = true;
                break;
            }
        }
        return declaredLocally;
    }
}

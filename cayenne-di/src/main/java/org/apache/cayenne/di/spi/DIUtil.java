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
package org.apache.cayenne.di.spi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;

import javax.inject.Named;
import javax.inject.Scope;
import javax.inject.Singleton;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.di.Key;

/**
 * A helper class used by Cayenne DI implementation.
 *
 * @since 3.1
 */
public abstract class DIUtil {

    /**
     * list of recognize annotation declaring the dependency injection.
     */
    @SuppressWarnings("unchecked")
    private static final List<Class<? extends Annotation>> injectAnnotationTypes = Arrays.asList(Inject.class,
            javax.inject.Inject.class);

    /**
     * list of recognize annotation declaring annotation as scope annotation.
     */
    @SuppressWarnings("unchecked")
    private static final List<Class<? extends Annotation>> scopeAnnotationTypes = Arrays
            .<Class<? extends Annotation>> asList(Scope.class);

    /**
     * Gets the first class argument of the parameterized {@code type} such as
     * List<String>.
     * <p>
     * {@code type} parameter must be a {@link ParameterizedType} only.
     *
     * @param type
     *            the type (can be {@code null}).
     * @return Returns the first class of type argument of the parameterized
     *         {@code type}, otherwise {@code null}.
     */
    static Class<?> parameterClass(Type type) {

        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type[] parameters = parameterizedType.getActualTypeArguments();

            if (parameters.length == 1) {
                return (Class<?>) parameters[0];
            }
        }

        return null;
    }

    /**
     * Gets indicating whether the parameter {@code annotations} enables the
     * injection dependency.
     *
     * @param annotation
     *            the annotation to test
     * @return Returns <code>true</code> whether <code>annotation</code>
     *         parameter enables the injection dependency.
     */
    public static boolean isInjectedAnnotations(Annotation... annotations) {
        for (Annotation annotation : annotations) {
            if (isInjectedAnnotation(annotation)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets indicating whether the parameter {@code annotation} enables the
     * injection dependency.
     *
     * @param annotation
     *            the annotation to test
     * @return Returns <code>true</code> whether <code>annotation</code>
     *         parameter enables the injection dependency.
     */
    public static boolean isInjectedAnnotation(Annotation annotation) {
        for (Class<? extends Annotation> annotationType : injectAnnotationTypes) {
            if (annotationType.equals(annotation.annotationType()))
                return true;
        }
        return false;
    }

    /**
     * Gets the annotation declaring the dependency injection on field or
     * method.
     *
     * @param ao
     *            a accessible object
     * @return Returns the annotation declaring the dependency injection,
     *         otherwise <code>null</code>.
     */
    public static Annotation getInjectAnnotation(final AccessibleObject ao) {
        for (Class<? extends Annotation> annotationType : injectAnnotationTypes) {
            Annotation annotation = ao.getAnnotation(annotationType);
            if (annotation != null)
                return annotation;
        }
        return null;
    }

    /**
     * Gets or creates new {@link Key} with binding name already defined in
     * {@code key} parameter or by naming annotation.
     * <p>
     * This function allows setting the binding name for key without already
     * defined binding name.
     *
     * @param key
     *            the reference key used.
     * @return Returns {@code Key} with appropriate binding name.
     * @see DIUtil#determineBindingName(Annotation[]))
     */
    public static <T> Key<T> named(Key<T> key) {
        return named(key, key.getType());
    }

    /**
     * Gets or creates new {@link Key} with binding name already defined in
     * {@code key} parameter or by naming annotation.
     * <p>
     * This function allows setting the binding name for key without already
     * defined binding name.
     *
     * @param key
     *            the reference key used.
     * @param implementedType
     *            the implemented binding type associated to the key parameter
     * @return Returns {@code Key} with appropriate binding name.
     * @see DIUtil#determineBindingName(Annotation[]))
     */
    public static <T> Key<T> named(Key<T> key, Class<? extends T> implementedType) {
        if (implementedType == null) {
            return key;
        }
        if (key.getBindingName() == null) {
            String bindingName = DIUtil.determineBindingName(implementedType.getAnnotations());
            if (bindingName != null && bindingName.length() > 0) {
                return Key.get(key.getType(), bindingName);
            }
        }
        return key;
    }

    /**
     * Finds and determines the binding name declared on field or method.
     *
     * @param ao
     *            a accessible object
     * @return Returns the binding name declared on field or method, otherwise
     *         <code>null</null>.
     */
    public static String determineBindingName(final AccessibleObject ao) {
        return determineBindingName(ao.getAnnotations());
    }

    /**
     * Finds and determines the binding name in list of annotations
     *
     * @param annotations
     *            list of annotations
     * @return Returns the binding name declared in list of annotation,
     *         otherwise <code>null</null>.
     */
    public static String determineBindingName(final Annotation[] annotations) {
        for (Annotation annotation : annotations) {
            String name = determineBindingName(annotation);
            if (name != null && name.length() > 0)
                return name;
        }
        return null;
    }

    /**
     * Finds and determines the binding name declared in the annotation
     *
     * @param annotation
     *            the annotation
     * @return Returns String representing the binding name declared in
     *         annotation, otherwise <code>null</null>.
     */
    static String determineBindingName(final Annotation annotation) {
        if (annotation instanceof Inject) {
            return ((Inject) annotation).value();
        } else if (annotation instanceof Named) {
            return ((Named) annotation).value();
        }
        return null;
    }

    /**
     * Finds scope annotation declared on {@code implementationClass} parameter.
     *
     * @param implementationClass
     *            the class on which find scope annotation (never {@code null}).
     * @return Returns scope annotation associated to
     *         {@code implementationClass} class whether exists, otherwise
     *         {@code null}.
     * @see Scope
     * @throws DIRuntimeException
     *             occurs if exists more than one scope annotation on the class.
     */
    public static Class<? extends Annotation> findScopeAnnotation(Class<?> implementationClass) {
        Annotation[] annotations = implementationClass.getAnnotations();
        Class<? extends Annotation> found = null;

        for (Annotation annotation : annotations) {
            Class<? extends Annotation> annotationType = annotation.annotationType();
            if (isScopeAnnotation(annotationType)) {
                if (found != null) {
                    throw new DIRuntimeException("Find duplicate scope annotation on annotation '%s'",
                            implementationClass);
                } else {
                    found = annotationType;
                }
            }
        }

        return found;
    }

    /**
     * Gets indicating whether the {@code annotationType} annotation is a scope
     * annotation.
     *
     * @param annotationType
     *            annotation to check (never {@code null}).
     * @return Returns {@code true} whether {@code annotationType} is a scope
     *         annotation.
     * @see Scope
     * @see Singleton
     */
    public static boolean isScopeAnnotation(Class<? extends Annotation> annotationType) {
        for (Annotation annotation : annotationType.getAnnotations()) {
            if (scopeAnnotationTypes.contains(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

}

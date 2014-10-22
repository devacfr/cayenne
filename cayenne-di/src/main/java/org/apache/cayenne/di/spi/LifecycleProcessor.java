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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.di.DIRuntimeException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class allows generate lifecycle metadata associated of a class. Metadata
 * use common annotations {@link PostConstruct} and {@link PreDestroy} of java
 * specification request <a
 * href="http://en.wikipedia.org/wiki/JSR_250">JSR-250</a> and specific Cayenne
 * DI annotation {@link BeforeScopeEnd} and {@link AfterScopeEnd}.
 * <p>
 * Note: Internally {@code BeforeScopeEnd} annotation is similar to
 * {@code PreDestroy}, {@code AfterScopeEnd} is for internal use only.
 *
 * <p>
 * <b>There are two main lifecycle:</b><br>
 * - Initializing Bean Lifecycle:
 * <ol>
 * <li>Creating new instance bean using the best constructor
 * {@link ConstructorInjectingProvider see} and inject parameters. (All scope)</li>
 * <li>field injecting dependency {@link FieldInjectingProvider see}. (All
 * scope)</li>
 * <li>PostConstruct method calling. (All scope)</li>
 * </ol>
 * - Disposal Bean Lifecycle begin the injector shutdown:
 * <ol>
 * <li>The default (singleton) scope shutdown</li>
 * <li>Call PredDestroy or BeforeScopeEnd method. (Singleton only)</li>
 * <li>Call AfterScopeEnd method. (Singleton provider only)</li>
 * </ol>
 * </p>
 *
 * @since 3.1
 * @see PostConstruct
 * @see PreDestroy
 * @see BeforeScopeEnd
 * @see AfterScopeEnd
 * @see ConstructorInjectingProvider
 * @see FieldInjectingProvider
 */
public class LifecycleProcessor {

    private static Log LOGGER = LogFactory.getLog(LifecycleProcessor.class);

    /**
     * Contains all type of annotations on a method that needs to be executed
     * after dependency injection is done.
     */
    @SuppressWarnings("unchecked")
    private Collection<Class<? extends Annotation>> initAnnotationTypes = new HashSet<Class<? extends Annotation>>(
            Arrays.asList(PostConstruct.class));

    /**
     * Contains all type of annotations on a method to signal that the instance
     * is in the process of being removed by the DI container.
     */
    @SuppressWarnings("unchecked")
    private Collection<Class<? extends Annotation>> preDestroyAnnotationTypes = new HashSet<Class<? extends Annotation>>(
            Arrays.asList(PreDestroy.class, BeforeScopeEnd.class));

    /**
     * Contains all type of annotations on a method to signal that the instance
     * has been removed by the DI container.
     */
    @SuppressWarnings("unchecked")
    private Collection<Class<? extends Annotation>> postDestroyAnnotationTypes = new HashSet<Class<? extends Annotation>>(
            Arrays.asList(AfterScopeEnd.class));

    /**
     * lifecycle metadata cache.
     */
    private transient Map<Class<?>, LifecycleMetadata> lifecycleMetadataCache = new ConcurrentHashMap<Class<?>, LifecycleMetadata>();

    public void clear() {
        this.lifecycleMetadataCache.clear();
    }

    /**
     * Creates or gets the lifecycle metadata associated to {@code clazz}.
     * parameter.
     *
     * @param clazz
     *            the class
     * @return Returns the lifecycle metadata associated to {@code clazz}
     *         parameter (never {@code null}).
     */
    public LifecycleMetadata findLifecycleMetadata(Class<?> clazz) {
        if (this.lifecycleMetadataCache == null) {
            // Happens after deserialization, during destruction...
            return buildLifecycleMetadata(clazz);
        }
        // Quick check on the concurrent map first, with minimal locking.
        LifecycleMetadata metadata = this.lifecycleMetadataCache.get(clazz);
        if (metadata == null) {
            synchronized (this.lifecycleMetadataCache) {
                metadata = this.lifecycleMetadataCache.get(clazz);
                if (metadata == null) {
                    metadata = buildLifecycleMetadata(clazz);
                    this.lifecycleMetadataCache.put(clazz, metadata);
                }
                return metadata;
            }
        }
        return metadata;
    }

    /**
     * Builds new instance {@link LifecycleMetadata} containing lifecycle
     * metadata of class {@code clazz}.
     *
     * @param clazz
     *            the class containing metadata
     * @return Returns new instance {@link LifecycleMetadata} containing
     *         lifecycle (never {@code null}).
     */
    private LifecycleMetadata buildLifecycleMetadata(Class<?> clazz) {
        LinkedList<LifecycleElement> initMethods = new LinkedList<LifecycleElement>();
        LinkedList<LifecycleElement> preDestroyMethods = new LinkedList<LifecycleElement>();
        LinkedList<LifecycleElement> postDestroyMethods = new LinkedList<LifecycleElement>();
        Class<?> targetClass = clazz;

        do {
            LinkedList<LifecycleElement> currInitMethods = new LinkedList<LifecycleElement>();
            LinkedList<LifecycleElement> currPreDestroyMethods = new LinkedList<LifecycleElement>();
            LinkedList<LifecycleElement> currPostDestroyMethods = new LinkedList<LifecycleElement>();
            for (Method method : targetClass.getDeclaredMethods()) {
                if (this.initAnnotationTypes != null) {
                    for (Class<? extends Annotation> annotation : initAnnotationTypes) {
                        if (method.getAnnotation(annotation) != null) {
                            LifecycleElement element = new LifecycleElement(method);
                            currInitMethods.add(element);
                        }
                    }
                }
                if (this.preDestroyAnnotationTypes != null) {
                    for (Class<? extends Annotation> annotation : this.preDestroyAnnotationTypes) {
                        if (method.getAnnotation(annotation) != null) {
                            currPreDestroyMethods.add(new LifecycleElement(method));
                        }
                    }
                }
                if (this.postDestroyAnnotationTypes != null) {
                    for (Class<? extends Annotation> annotation : this.postDestroyAnnotationTypes) {
                        if (method.getAnnotation(annotation) != null) {
                            currPostDestroyMethods.add(new LifecycleElement(method));
                        }
                    }
                }
            }
            initMethods.addAll(0, currInitMethods);
            preDestroyMethods.addAll(currPreDestroyMethods);
            postDestroyMethods.addAll(currPostDestroyMethods);
            targetClass = targetClass.getSuperclass();
        } while (targetClass != null && targetClass != Object.class);

        return new LifecycleMetadata(clazz, initMethods, preDestroyMethods, postDestroyMethods);
    }

    /**
     * This class contains all lifecycle methods of a class.
     *
     */
    public static class LifecycleMetadata {

        private final Set<LifecycleElement> initMethods;

        private final Set<LifecycleElement> preDestroyMethods;

        private final Set<LifecycleElement> postDestroyMethods;

        public LifecycleMetadata(Class<?> targetClass, Collection<LifecycleElement> initMethods,
                Collection<LifecycleElement> preDestroyMethods, Collection<LifecycleElement> postDestroyMethods) {

            this.initMethods = Collections.synchronizedSet(new LinkedHashSet<LifecycleElement>());
            for (LifecycleElement element : initMethods) {
                this.initMethods.add(element);
            }

            this.preDestroyMethods = Collections.synchronizedSet(new LinkedHashSet<LifecycleElement>());
            for (LifecycleElement element : preDestroyMethods) {
                this.preDestroyMethods.add(element);
            }

            this.postDestroyMethods = Collections.synchronizedSet(new LinkedHashSet<LifecycleElement>());
            for (LifecycleElement element : postDestroyMethods) {
                this.postDestroyMethods.add(element);
            }
        }

        public boolean isEmpty() {
            return this.initMethods.isEmpty() && this.preDestroyMethods.isEmpty() && this.postDestroyMethods.isEmpty();
        }

        public void invokeInitMethods(Object target) {
            if (!this.initMethods.isEmpty()) {
                try {
                    for (LifecycleElement element : this.initMethods) {
                        element.invoke(target);
                    }
                } catch (InvocationTargetException ex) {
                    throw new DIRuntimeException("Invocation of init method failed on bean'%s'.",
                            ex.getTargetException(), target.getClass());
                } catch (Throwable ex) {
                    throw new DIRuntimeException("Couldn't invoke init method on bean'%s'.", ex, target.getClass());
                }
            }
        }

        public void invokePreDestroyMethods(Object target) {
            if (!this.preDestroyMethods.isEmpty()) {
                try {
                    for (LifecycleElement element : this.preDestroyMethods) {
                        element.invoke(target);
                    }
                } catch (InvocationTargetException ex) {
                    String msg = "Invocation of destroy method failed on bean '" + target.getClass() + "'";
                    LOGGER.error(msg, ex);
                } catch (Throwable ex) {
                    // noop
                }

            }
        }

        public void invokePostDestroyMethods(Object target) {
            if (!this.postDestroyMethods.isEmpty()) {
                try {
                    for (LifecycleElement element : this.postDestroyMethods) {
                        element.invoke(target);
                    }
                } catch (InvocationTargetException ex) {
                    String msg = "Invocation of destroy method failed on bean '" + target.getClass() + "'";
                    LOGGER.error(msg, ex);
                } catch (Throwable ex) {
                    // noop
                }
            }
        }
    }

    /**
     * Class representing injection information about an annotated method.
     */
    private static class LifecycleElement {

        private final Method method;

        private final String identifier;

        public LifecycleElement(Method method) {
            if (method.getParameterTypes().length != 0) {
                throw new IllegalStateException("Lifecycle method annotation requires a no-arg method: " + method);
            }
            this.method = method;
            this.identifier = (Modifier.isPrivate(method.getModifiers()) ? method.getDeclaringClass() + "."
                    + method.getName() : method.getName());
        }

        public void invoke(Object target) throws Throwable {
            makeAccessible(this.method);
            this.method.invoke(target, (Object[]) null);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LifecycleElement)) {
                return false;
            }
            LifecycleElement otherElement = (LifecycleElement) other;
            return (this.identifier.equals(otherElement.identifier));
        }

        @Override
        public int hashCode() {
            return this.identifier.hashCode();
        }

        public static void makeAccessible(Method method) {
            if ((!Modifier.isPublic(method.getModifiers()) || !Modifier.isPublic(method.getDeclaringClass()
                    .getModifiers())) && !method.isAccessible()) {
                method.setAccessible(true);
            }
        }
    }
}

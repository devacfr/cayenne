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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cayenne.testing.statement.RunAfterTestClassCallbacks;
import org.apache.cayenne.testing.statement.RunBeforeTestClassCallbacks;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

/**
 * <p>
 * The cayenne specific runner <code>Parameterized</code> implements
 * parameterized tests. When running a parameterized test class, instances are
 * created for the cross-product of the test methods and the test data elements.
 * </p>
 *
 * For example, to test the serialization of ROP connection , write:
 *
 * <pre>
 * &#064;RunWith(CayenneParameterizedJUnit4SuiteRunner.class)
 * public class ROPSerializationClientTest {
 *     &#064;org.junit.runners.Parameterized.Parameters(name = &quot;{index}: serializationPolicy={0}&quot;)
 *     public static Iterable&lt;Object[]&gt; data() {
 *         return Arrays.asList(new Object[] { { LocalConnection.HESSIAN_SERIALIZATION }, { LocalConnection.JAVA_SERIALIZATION}, { LocalConnection.NO_SERIALIZATION } });
 *     }
 * 
 *     private int serializationPolicy;
 * 
 * 
 *     public ROPSerializationClientTest(int serializationPolicy) {
 *         this.serializationPolicy = serializationPolicy;
 *     }
 *     
 *     protected CayenneContext createROPContext() {
 *         ClientServerChannel clientServerChannel = new ClientServerChannel(serverContext);
 *         UnitLocalConnection connection = new UnitLocalConnection(clientServerChannel,serializationPolicy);
 *         ClientChannel channel = new ClientChannel(connection, false,new DefaultEventManager(0),false);
 *         CayenneContext context = new CayenneContext(channel, true, true);
 *         context.setQueryCache(new MapQueryCache(10));
 *         return context;
 *     }
 *     
 *     ...
 * 
 *     &#064;Test
 *     public void test() {
 *         ObjectContext context = createContext();
 *         Continent continent = context.newObject(Continent.class);
 *         continent.setName("Europe");
 * 
 *         Country country = new Country();
 *         context.registerNewObject(country);
 * 
 *         country.setName("Russia");
 * 
 *         country.setContinent(continent);
 *         assertEquals(continent.getCountries().size(), 1);
 * 
 *         context.commitChanges();
 * 
 *         context.deleteObjects(country);
 *         assertEquals(continent.getCountries().size(), 0);
 *         continent.setName("Australia");
 * 
 *         context.commitChanges();
 *         context.performQuery(new RefreshQuery());
 * 
 *         assertEquals(context.performQuery(new SelectQuery&lt;Country&gt;(Country.class)).size(), 0);
 *         assertEquals(context.performQuery(new SelectQuery&lt;Continent&gt;(Continent.class)).size(), 1);
 *     }
 * }
 * </pre>
 *
 * <p>
 * Each instance of <code>ROPSerializationClientTest</code> will be constructed
 * using the one-argument constructor and the data values in the
 * <code>&#064;Parameters</code> method.
 *
 * <p>
 * In order that you can easily identify the individual tests, you may provide a
 * name for the <code>&#064;Parameters</code> annotation. This name is allowed
 * to contain placeholders, which are replaced at runtime. The placeholders are
 * <dl>
 * <dt>{index}</dt>
 * <dd>the current parameter index</dd>
 * <dt>{0}</dt>
 * <dd>the first parameter value</dd>
 * <dt>{1}</dt>
 * <dd>the second parameter value</dd>
 * <dt>...</dt>
 * <dd></dd>
 * </dl>
 * In the example given above, the <code>Parameterized</code> runner creates
 * names like <code>[1: serializationPolicy=2]</code>. If you don't use the name
 * parameter, then the current parameter index is used as name.
 * </p>
 *
 * You can also write:
 *
 * <pre>
 * &#064;RunWith(CayenneParameterizedJUnit4SuiteRunner.class)
 * public class ROPSerializationClientTest {
 *     &#064;org.junit.runners.Parameterized.Parameters
 *     public static Iterable&lt;Object[]&gt; data() {
 *         return Arrays.asList(new Object[][] { { LocalConnection.HESSIAN_SERIALIZATION }, { LocalConnection.JAVA_SERIALIZATION}, { LocalConnection.NO_SERIALIZATION } });
 *     }
 *
 *     &#064;org.junit.runners.Parameterized.Parameter(0)
 *     public int serializationPolicy;
 *
 *     ...
 *
 *     &#064;Test
 *     public void test() {
 *         ...
 *     }
 * }
 * </pre>
 *
 * <p>
 * Each instance of <code>ROPSerializationClientTest</code> will be constructed
 * with the default constructor and fields annotated by
 * <code>&#064;Parameter</code> will be initialized with the data values in the
 * <code>&#064;Parameters</code> method.
 * </p>
 *
 * @since 3.2
 */
public class CayenneParameterizedJUnit4SuiteRunner extends Suite {

    private static final Log logger = LogFactory.getLog(CayenneParameterizedJUnit4SuiteRunner.class);

    private final CayenneTestContextManager testContextManager;

    private static final List<Runner> NO_RUNNERS = Collections.<Runner> emptyList();

    private final ArrayList<Runner> runners = new ArrayList<Runner>();

    public CayenneParameterizedJUnit4SuiteRunner(Class<?> clazz) throws Throwable {
        super(clazz, NO_RUNNERS);
        if (logger.isDebugEnabled()) {
            logger.debug("CayenneParameterizedJUnit4ClassRunner constructor called with [" + clazz + "].");
        }
        Parameters parameters = getParametersMethod().getAnnotation(Parameters.class);
        createRunnersForParameters(allParameters(), parameters.name());

        this.testContextManager = createTestContextManager(clazz);
    }

    protected CayenneTestContextManager createTestContextManager(Class<?> clazz) {
        return new CayenneTestContextManager(clazz);
    }

    protected final CayenneTestContextManager getTestContextManager() {
        return this.testContextManager;
    }

    @Override
    protected List<Runner> getChildren() {
        return runners;
    }

    @Override
    protected Statement withBeforeClasses(Statement statement) {
        Statement junitBeforeClasses = super.withBeforeClasses(statement);
        return new RunBeforeTestClassCallbacks(junitBeforeClasses, getTestContextManager());
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        Statement junitAfterClasses = super.withAfterClasses(statement);
        return new RunAfterTestClassCallbacks(junitAfterClasses, getTestContextManager());
    }

    @SuppressWarnings("unchecked")
    private Iterable<Object[]> allParameters() throws Throwable {
        Object parameters = getParametersMethod().invokeExplosively(null);
        if (parameters instanceof Iterable) {
            return (Iterable<Object[]>) parameters;
        } else {
            throw parametersMethodReturnedWrongType();
        }
    }

    private FrameworkMethod getParametersMethod() throws Exception {
        List<FrameworkMethod> methods = getTestClass().getAnnotatedMethods(Parameters.class);
        for (FrameworkMethod each : methods) {
            if (each.isStatic() && each.isPublic()) {
                return each;
            }
        }

        throw new Exception("No public static parameters method on class " + getTestClass().getName());
    }

    private void createRunnersForParameters(Iterable<Object[]> allParameters, String namePattern)
            throws InitializationError, Exception {
        try {
            int i = 0;
            for (Object[] parametersOfSingleTest : allParameters) {
                String name = nameFor(namePattern, i, parametersOfSingleTest);
                TestClassRunnerForParameters runner = new TestClassRunnerForParameters(getTestClass().getJavaClass(),
                        parametersOfSingleTest, name, this.testContextManager);
                runners.add(runner);
                ++i;
            }
        } catch (ClassCastException e) {
            throw parametersMethodReturnedWrongType();
        }
    }

    private String nameFor(String namePattern, int index, Object[] parameters) {
        String finalPattern = namePattern.replaceAll("\\{index\\}", Integer.toString(index));
        String name = MessageFormat.format(finalPattern, parameters);
        return "[" + name + "]";
    }

    private Exception parametersMethodReturnedWrongType() throws Exception {
        String className = getTestClass().getName();
        String methodName = getParametersMethod().getName();
        String message = MessageFormat.format("{0}.{1}() must return an Iterable of arrays.", className, methodName);
        return new Exception(message);
    }

    private List<FrameworkField> getAnnotatedFieldsByParameter() {
        return getTestClass().getAnnotatedFields(Parameter.class);
    }

    private boolean fieldsAreAnnotated() {
        return !getAnnotatedFieldsByParameter().isEmpty();
    }

    private class TestClassRunnerForParameters extends CayenneBlockJUnit4ClassRunner {

        private final Object[] fParameters;

        private final String fName;

        TestClassRunnerForParameters(final Class<?> type, final Object[] parameters, final String name,
                final CayenneTestContextManager testContextManager) throws InitializationError {
            super(type, testContextManager);
            fParameters = parameters;
            fName = name;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object createTest() throws Exception {
            Object testInstance = null;
            if (fieldsAreAnnotated()) {
                testInstance = createTestUsingFieldInjection();
            } else {
                testInstance = createTestUsingConstructorInjection();
            }
            getTestContextManager().prepareTestInstance(testInstance);
            return testInstance;
        }

        private Object createTestUsingConstructorInjection() throws Exception {
            Constructor<?> cotr = getTestClass().getOnlyConstructor();
            return cotr.newInstance(fParameters);
        }

        private Object createTestUsingFieldInjection() throws Exception {
            List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
            if (annotatedFieldsByParameter.size() != fParameters.length) {
                throw new Exception("Wrong number of parameters and @Parameter fields."
                        + " @Parameter fields counted: " + annotatedFieldsByParameter.size()
                        + ", available parameters: " + fParameters.length + ".");
            }
            Object testClassInstance = getTestClass().getJavaClass().newInstance();
            for (FrameworkField each : annotatedFieldsByParameter) {
                Field field = each.getField();
                Parameter annotation = field.getAnnotation(Parameter.class);
                int index = annotation.value();
                try {
                    field.set(testClassInstance, fParameters[index]);
                } catch (IllegalArgumentException iare) {
                    throw new Exception(getTestClass().getName() + ": Trying to set " + field.getName()
                            + " with the value " + fParameters[index] + " that is not the right type ("
                            + fParameters[index].getClass().getSimpleName() + " instead of "
                            + field.getType().getSimpleName() + ").", iare);
                }
            }
            return testClassInstance;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String getName() {
            return fName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected String testName(FrameworkMethod method) {
            return method.getName() + getName();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateConstructor(List<Throwable> errors) {
            validateOnlyOneConstructor(errors);
            if (fieldsAreAnnotated()) {
                validateZeroArgConstructor(errors);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected void validateFields(List<Throwable> errors) {
            super.validateFields(errors);
            if (fieldsAreAnnotated()) {
                List<FrameworkField> annotatedFieldsByParameter = getAnnotatedFieldsByParameter();
                int[] usedIndices = new int[annotatedFieldsByParameter.size()];
                for (FrameworkField each : annotatedFieldsByParameter) {
                    int index = each.getField().getAnnotation(Parameter.class).value();
                    if (index < 0 || index > annotatedFieldsByParameter.size() - 1) {
                        errors.add(new Exception("Invalid @Parameter value: " + index + ". @Parameter fields counted: "
                                + annotatedFieldsByParameter.size() + ". Please use an index between 0 and "
                                + (annotatedFieldsByParameter.size() - 1) + "."));
                    } else {
                        usedIndices[index]++;
                    }
                }
                for (int index = 0; index < usedIndices.length; index++) {
                    int numberOfUse = usedIndices[index];
                    if (numberOfUse == 0) {
                        errors.add(new Exception("@Parameter(" + index + ") is never used."));
                    } else if (numberOfUse > 1) {
                        errors.add(new Exception("@Parameter(" + index + ") is used more than once (" + numberOfUse
                                + ")."));
                    }
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Statement classBlock(RunNotifier notifier) {
            return childrenInvoker(notifier);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected Annotation[] getRunnerAnnotations() {
            return new Annotation[0];
        }
    }
}
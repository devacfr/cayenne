package org.apache.cayenne.di.testing;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * This class allow to migrate form JUnit 3.x syntax to JUnit 4.</p>
 * @author devacfr<christophefriederich@mac.com>
 *
 */
@RunWith(BlockJUnit4ClassRunner.class)
public abstract class TestCase extends Assert {

    public TestCase() {

    }

    public final String getPackageName() {
        return this.getClass().getPackage().getName().replace('.', '/');
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {

    }


}

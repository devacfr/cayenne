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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.testing.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

@RunWith(BlockJUnit4ClassRunner.class)
public class AssertTest extends TestCase {

    @Test
    public void isTrue() {
        try {
            Assert.isTrue(false);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.isTrue(true);
    }

    @Test
    public void isNull() {
        try {
            Assert.isNull(new Object());
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.isNull(null);
    }

    @Test
    public void notNull() {
        try {
            Assert.notNull(null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.notNull(new Object());
    }

    @Test
    public void hasLength() {
        try {
            Assert.hasLength(null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.hasLength("");
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.hasLength(" ");
        Assert.hasLength("Hello");
    }

    @Test
    public void hasText() {
        try {
            Assert.hasText(null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.hasText("");
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.hasText(" ");
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.hasText("12345");
        Assert.hasText(" 12345 ");
    }

    @Test
    public void doesNotContain() {
        try {
            Assert.doesNotContain("The latest fishing rods are made of fibre glass.", "rod");
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.doesNotContain("Grandma and Grandpa like sitting in their rocking chairs on the veranda.", "rod");
    }

    @Test
    public void notEmptyArray() {
        try {
            Assert.notEmpty((Object[]) null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.notEmpty(new String[] {});
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.notEmpty(new String[] { "1234" });
    }

    @Test
    public void noNullElements() {

        try {
            Assert.noNullElements(new String[] { null });
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.noNullElements((Object[]) null);
        Assert.noNullElements(new String[] {});
        Assert.noNullElements(new String[] { "1234" });
    }

    @Test
    public void notEmptyCollection() {
        try {
            Assert.notEmpty((Collection<?>) null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.notEmpty(new ArrayList<String>());
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.notEmpty(new ArrayList<String>(Arrays.asList("1234")));
    }

    @Test
    public void notEmptyMap() {
        try {
            Assert.notEmpty((Map<?, ?>) null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.notEmpty(new HashMap<String, String>());
            fail();
        } catch (IllegalArgumentException ex) {

        }
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("key", "123");
        Assert.notEmpty(map);
    }

    @Test
    public void isInstanceOf() {
        try {
            Assert.isInstanceOf(String.class, null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.isInstanceOf(null, null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.isInstanceOf(String.class, new Object());
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.isInstanceOf(String.class, "1234");
    }

    @Test
    public void isAssignable() {
        try {
            Assert.isAssignable(String.class, null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.isAssignable(null, null);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.isAssignable(String.class, Object.class);
            fail();
        } catch (IllegalArgumentException ex) {

        }
        Assert.isAssignable(Object.class, String.class);
    }

    @Test
    public void state() {
        try {
            Assert.state(1 == 2);
            fail();
        } catch (IllegalStateException ex) {

        }
        Assert.state(true);
    }

    @Test
    public void isEqual() {
        try {
            Assert.equals("value", "1234", "0");
            fail();
        } catch (IllegalArgumentException ex) {

        }
        try {
            Assert.equals("value", null, null);
            fail();
        } catch (NullPointerException ex) {

        }
        Assert.equals("value", "1234", "1234");
    }
}

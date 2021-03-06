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

package org.apache.cayenne.util;

import org.apache.cayenne.map.naming.NameConverter;
import org.apache.cayenne.testing.TestCase;
import org.junit.Test;


public class NameConverterTest extends TestCase {

	@Test
    public void testUnderscoredToJava1() throws Exception {
        String expected = "ClassNameIdentifier";
        assertEquals(expected, NameConverter.underscoredToJava(
                "_CLASS_NAME_IDENTIFIER_",
                true));
    }

	@Test
    public void testUnderscoredToJava2() throws Exception {
        String expected = "propNameIdentifier123";
        assertEquals(expected, NameConverter.underscoredToJava(
                "_prop_name_Identifier_123",
                false));
    }

	@Test
    public void testUnderscoredToJava3() throws Exception {
        String expected = "lastName";
        assertEquals(expected, NameConverter.underscoredToJava("lastName", false));
    }

	@Test
    public void testUnderscoredToJava4() throws Exception {
        String expected = "lastName";
        assertEquals(expected, NameConverter.underscoredToJava("LastName", false));
    }

	@Test
    public void testUnderscoredToJava5() throws Exception {
        String expected = "LastName";
        assertEquals(expected, NameConverter.underscoredToJava("LastName", true));
    }

	@Test
    public void testUnderscoredToJavaSpecialChars() throws Exception {
        assertEquals("ABCpoundXyz", NameConverter.underscoredToJava("ABC#_XYZ", true));
    }

	@Test
    public void testJavaToUnderscored1() throws Exception {
        String expected = "LAST_NAME";
        assertEquals(expected, NameConverter.javaToUnderscored("LastName"));
    }

	@Test
    public void testJavaToUnderscored2() throws Exception {
        String expected = "A_CLASS";
        assertEquals(expected, NameConverter.javaToUnderscored("aClass"));
    }

	@Test
    public void testJavaToUnderscored3() throws Exception {
        String expected = "VAR_A";
        assertEquals(expected, NameConverter.javaToUnderscored("varA"));
    }

	@Test
    public void testJavaToUnderscored4() throws Exception {
        String expected = "LAST_NAME";
        assertEquals(expected, NameConverter.javaToUnderscored("LAST_NAME"));
    }

	@Test
    public void testJavaToUnderscored5() throws Exception {
        String expected = "ABC_A";
        assertEquals(expected, NameConverter.javaToUnderscored("abc_A"));
    }

	@Test
    public void testJavaToUnderscored6() throws Exception {
        String expected = "A123";
        assertEquals(expected, NameConverter.javaToUnderscored("a123"));
    }

	@Test
    public void testJavaToUnderscored7() throws Exception {
        String expected = "AB_CDEF";
        assertEquals(expected, NameConverter.javaToUnderscored("abCDEF"));
    }

	@Test
    public void testJavaToUnderscored8() throws Exception {
        String expected = "AB_CE";
        assertEquals(expected, NameConverter.javaToUnderscored("abCe"));
    }
}

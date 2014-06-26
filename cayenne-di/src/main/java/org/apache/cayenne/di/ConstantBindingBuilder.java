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
package org.apache.cayenne.di;

/**
 * This builder allows binding constant value.
 *
 * @since 3.2
 * @param <T>
 *            Represent the type of value to bind.
 */
public interface ConstantBindingBuilder<T> {

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(String value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(int value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(long value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(boolean value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(double value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(float value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(short value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(char value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(byte value);

    /**
     * Binds constant to the given String value.
     *
     * @param value
     *            constant value to bind.
     */
    void to(Class<?> value);

}
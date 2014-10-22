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
package org.apache.cayenne.di.spi.event;

import java.util.Set;

public interface ListenerInvoker {

    /**
     * Gets the listener to invoke.
     * 
     * @return Returns a bean listener to invoke.
     */
    Object getListener();

    /**
     * The types of events supported by this invoker. I.e.
     * {@link #invoke(Object)} can be safely called with any object that is an
     * instance of at least one of those types.
     *
     * @return the set of supported event types.
     */
    Set<Class<?>> getSupportedEventTypes();

    /**
     * Invokes the underlying listener for the given event.
     *
     * @param event
     *            the event to tell the listener about.
     * @throws IllegalArgumentException
     *             if the event is not an instance of any of the types returned
     *             by {@link #getSupportedEventTypes()}
     */
    void invoke(Object event);

}
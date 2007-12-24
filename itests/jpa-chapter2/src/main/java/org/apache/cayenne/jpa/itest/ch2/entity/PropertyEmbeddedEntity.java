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
package org.apache.cayenne.jpa.itest.ch2.entity;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class PropertyEmbeddedEntity {

    protected int xid;
    protected PropertyEmbeddable xembeddable;

    @Embedded
    public PropertyEmbeddable getEmbeddable() {
        return xembeddable;
    }

    public void setEmbeddable(PropertyEmbeddable embeddable) {
        this.xembeddable = embeddable;
    }

    @Id
    public int getId() {
        return xid;
    }

    public void setId(int id) {
        this.xid = id;
    }
}

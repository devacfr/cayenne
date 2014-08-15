/**
 * Copyright 2014 devacfr<christophefriederich@mac.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cayenne.tx;

import javax.sql.DataSource;

import org.apache.cayenne.tx.support.TransactionManager;

/**
 * @author devacfr<christophefriederich@mac.com>
 *
 */
public interface TransactionManagerFactory {

    TransactionManager getTransactionManager(DataSource datasource);

}

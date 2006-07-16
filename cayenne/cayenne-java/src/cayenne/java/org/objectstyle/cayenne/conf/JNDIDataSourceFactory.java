/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */

package org.objectstyle.cayenne.conf;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.objectstyle.cayenne.access.QueryLogger;
import org.objectstyle.cayenne.util.Util;

/**
 * Looks up DataSource objects via JNDI.
 * 
 * @author Andrei Adamchik
 */
public class JNDIDataSourceFactory implements DataSourceFactory {

    private static final Logger logObj = Logger.getLogger(JNDIDataSourceFactory.class);

    protected Configuration parentConfig;

    public void initializeWithParentConfiguration(Configuration conf) {
        this.parentConfig = conf;
    }

    /**
     * Returns DataSource object corresponding to <code>location</code>. Location is
     * expected to be a path mapped in JNDI InitialContext.
     * 
     * @deprecated since 1.2
     */
    public DataSource getDataSource(String location, Level logLevel) throws Exception {
        return getDataSource(location);
    }

    /**
     * Attempts to load DataSource using JNDI. In case of failure tries to get the
     * DataSource with the same name from CayenneModeler preferences.
     */
    public DataSource getDataSource(String location) throws Exception {

        try {
            return loadViaJNDI(location);
        }
        catch (Exception ex) {

            logObj.info("failed JNDI lookup, attempt to load "
                    + "from local preferences. Location key:"
                    + location);

            // failover to preferences loader to allow local development
            try {
                return loadFromPreferences(location);
            }
            catch (Exception preferencesException) {

                logObj.info("failed loading from local preferences", Util
                        .unwindException(preferencesException));

                // giving up ... rethrow original exception...
                QueryLogger.logConnectFailure(ex);
                throw ex;
            }
        }
    }

    DataSource loadViaJNDI(String location) throws NamingException {
        QueryLogger.logConnect(location);

        Context initCtx = new InitialContext();
        DataSource ds;
        try {
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            ds = (DataSource) envCtx.lookup(location);
        }
        catch (NamingException namingEx) {
            // try looking up the location directly...
            ds = (DataSource) initCtx.lookup(location);
        }

        QueryLogger.logConnectSuccess();
        return ds;
    }

    DataSource loadFromPreferences(String location) throws Exception {
        // as we don't want compile dependencies on the Modeler, instantiate factory via
        // reflection ...

        DataSourceFactory prefsFactory = (DataSourceFactory) Class
                .forName(
                        "org.objectstyle.cayenne.modeler.pref.PreferencesDataSourceFactory")
                .newInstance();

        prefsFactory.initializeWithParentConfiguration(parentConfig);
        return prefsFactory.getDataSource(location);
    }
}

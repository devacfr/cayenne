/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002 The ObjectStyle Group 
 * and individual authors of the software.  All rights reserved.
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
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:  
 *       "This product includes software developed by the 
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne" 
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written 
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
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
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */ 

package org.objectstyle.cayenne.access;

import java.util.List;

import org.objectstyle.cayenne.query.Query;

/**
 * Defines a set of callback methods for a QueryEngine to notify interested 
 * object about different stages of queries execution. Superinterface, OperationHints,
 * defines information methods that are needed to define query execution 
 * strategy. This Interface adds callback methods.
 *
 * <p>Implementing objects are passed to a QueryEngine that will execute
 * one or more queries. QueryEngine will pass results of the execution 
 * of any kind of queries - selects, updates, store proc. calls, etc..
 * to the interested objects. This includes result counts, created objects, 
 * thrown exceptions, etc.</p>
 * 
 * <p><i>For more information see <a href="../../../../../../userguide/index.html"
 * target="_top">Cayenne User Guide.</a></i></p>
 * 
 * @see org.objectstyle.cayenne.access.QueryEngine
 * 
 * @author Andrei Adamchik
 */
public interface OperationObserver extends OperationHints {
	
	/** 
	 * Invoked after the update (can be insert, delete or update query) is executed.
	 */
    public void nextCount(Query query, int resultCount);
    
	/** 
	 * Invoked after the batch update is executed
	 */
	public void nextBatchCount(Query query, int[] resultCount);
    
    
    /** Invoked after the next query results are read. */
    public void nextDataRows(Query query, List dataRows);
    
    
   	/** 
	 * Invoked after the next query is invoked, if a query required
	 * results to be returned as a ResultIterator. OperationObserver 
	 * is responsible for closing the ResultIterator.
	 */
    public void nextDataRows(Query q, ResultIterator it);
	
	
    /** Invoked when an exception occurs during query execution. */
    public void nextQueryException(Query query, Exception ex);
    
    /** 
     * Invoked when a "global" exception occurred, such as JDBC
     * connection exception, etc.
     */
    public void nextGlobalException(Exception ex);
    
    
    /** 
     * Invoked when a batch of queries was processed as a single transaction,
     * and this transaction was successfully committed.
     */
    public void transactionCommitted();
    
    
    /** 
     * Invoked when a batch of queries was processed as a single transaction,
     *  and this transaction was failed and was rolled back. 
     */
    public void transactionRolledback();
    
    
    /** 
     * @deprecated Since 1.0 Beta 1, Ashwood-based implementation is used for
     * sorting. In the future we may implement other types of delegate methods
     * to notify of the query processing start. This method is no longer called
     * by the DataNode.
     */
    public List orderQueries(DataNode aNode, List queryList);
}


/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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
 */package org.objectstyle.cayenne.query;

import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.map.ObjRelationship;

/**
 * 
 * @author Craig Miskell
 */
public class PrefetchSelectQuery extends SelectQuery {
	/** The query that provides the "root" objects of the prefetch*/
	protected SelectQuery rootQuery;
	
	/** The relationship path from root objects to the objects being prefetched*/
	protected String prefetchPath;
	
	protected ObjRelationship singleStepToManyRelationship=null;
	
	/**
	 * Constructor for PrefetchSelectQuery.
	 */
	public PrefetchSelectQuery() {
		super();
	}

	/**
	 * Constructor for PrefetchSelectQuery.
	 * @param root
	 */
	public PrefetchSelectQuery(ObjEntity root) {
		super(root);
	}

	/**
	 * Constructor for PrefetchSelectQuery.
	 * @param root
	 * @param qualifier
	 */
	public PrefetchSelectQuery(ObjEntity root, Expression qualifier) {
		super(root, qualifier);
	}

	/**
	 * Constructor for PrefetchSelectQuery.
	 * @param rootClass
	 */
	public PrefetchSelectQuery(Class rootClass) {
		super(rootClass);
	}

	/**
	 * Constructor for PrefetchSelectQuery.
	 * @param rootClass
	 * @param qualifier
	 */
	public PrefetchSelectQuery(Class rootClass, Expression qualifier) {
		super(rootClass, qualifier);
	}

	/**
	 * Constructor for PrefetchSelectQuery.
	 * @param objEntityName
	 */
	public PrefetchSelectQuery(String objEntityName) {
		super(objEntityName);
	}

	/**
	 * Constructor for PrefetchSelectQuery.
	 * @param objEntityName
	 * @param qualifier
	 */
	public PrefetchSelectQuery(String objEntityName, Expression qualifier) {
		super(objEntityName, qualifier);
	}
	
	/**
	 * Returns the prefetchPath.
	 * @return String
	 */
	public String getPrefetchPath() {
		return prefetchPath;
	}

	/**
	 * Sets the prefetchPath.
	 * @param prefetchPath The prefetchPath to set
	 */
	public void setPrefetchPath(String prefetchPath) {
		this.prefetchPath = prefetchPath;
	}

	/**
	 * @return SelectQuery
	 */
	public SelectQuery getRootQuery() {
		return rootQuery;
	}

	/**
	 * Sets the rootQuery.
	 * @param rootQuery The rootQuery to set
	 */
	public void setRootQuery(SelectQuery rootQuery) {
		this.rootQuery = rootQuery;
	}

	/**
	 * @return ObjRelationship
	 */
	public ObjRelationship getSingleStepToManyRelationship() {
		return singleStepToManyRelationship;
	}

	/**
	 * Sets the singleStepToManyRelationship.
	 * @param singleStepToManyRelationship The singleStepToManyRelationship to set
	 */
	public void setSingleStepToManyRelationship(ObjRelationship singleStepToManyRelationship) {
		this.singleStepToManyRelationship = singleStepToManyRelationship;
	}
}

/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group
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

package org.objectstyle.cayenne.map;

import java.util.Iterator;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.lang.StringUtils;
import org.objectstyle.cayenne.CayenneException;

/**
 * An ObjAttribute is a mapping descriptor of a Java class property.
 *
 * @author Misha Shengaout
 * @author Andrei Adamchik
 */
public class ObjAttribute extends Attribute {
    // Full name of Java class representing the property type.
    protected String attrType;

    // The name of the corresponding database table column
    private String dbAttributePath;

	public ObjAttribute() {}

	public ObjAttribute(String name) {
		super(name);
	}


	public ObjAttribute(String name, String type, ObjEntity entity) {
		setName(name);
		setType(type);
		setEntity(entity);
	}


	/** Gets the type of the data object property.
     * Type returned is a string that specifies full name of a Java class
     * used to represent this attribute. */
	public String getType() {
        return attrType;
    }


	/** Sets the type of the data object property.*/
    public void setType(String type) {
        this.attrType = type;
    }


	/**
	 * Returns an attribute describing a mapped database
	 * table column.
	 */
    public DbAttribute getDbAttribute() {
        Iterator pathIterator = getDbPathIterator();
        Object o = null;
        while (pathIterator.hasNext()) {
            o = pathIterator.next();
        }
        return (DbAttribute)o;
    }

    public Iterator getDbPathIterator() {
        if(dbAttributePath == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        ObjEntity ent = (ObjEntity)getEntity();
        if(ent == null) {
            return IteratorUtils.EMPTY_ITERATOR;
        }

        DbEntity dbEnt = ent.getDbEntity();
        if(dbEnt == null) {
            return IteratorUtils.EMPTY_ITERATOR;
    	}

        int lastPartStart = dbAttributePath.lastIndexOf('.');
        if (lastPartStart < 0) {
            Attribute attribute = dbEnt.getAttribute(dbAttributePath);
            if (attribute == null) {
            	return IteratorUtils.EMPTY_ITERATOR;
            }
            return IteratorUtils.singletonIterator(attribute);
        }

        return dbEnt.resolvePathComponents(dbAttributePath);
    }


	/** Set the corresponding database table column.
     *  @see org.objectstyle.cayenne.map.DbAttribute#getName() */
    public void setDbAttribute(DbAttribute dbAttribute) {
    	if(dbAttribute == null) {
    		this.setDbAttributePath(null);
    	}
    	else {
    		this.setDbAttributePath(dbAttribute.getName());
    	}
    }


	/**
	 * Returns the dbAttributeName.
	 * @return String
	 */
	public String getDbAttributeName() {
        if (dbAttributePath == null) return null;
        int lastPartStart = dbAttributePath.lastIndexOf('.');
        String lastPart = StringUtils.substring(
                dbAttributePath,
                lastPartStart + 1,
                dbAttributePath.length());
		return lastPart;
	}


	/**
	 * Sets the dbAttributeName.
	 * @param dbAttributeName The dbAttributeName to set
	 */
	public void setDbAttributeName(String dbAttributeName) {
        if (dbAttributePath == null || dbAttributeName == null) {
            dbAttributePath = dbAttributeName;
            return;
        }
        int lastPartStart = dbAttributePath.lastIndexOf('.');
        String newPath = (lastPartStart > 0
        					? StringUtils.chomp(dbAttributePath, ".")
        					: "");
        newPath += (newPath.length() > 0 ? "." : "") + dbAttributeName;
		this.dbAttributePath = newPath;
	}

    public void setDbAttributePath(String dbAttributePath) {
        this.dbAttributePath = dbAttributePath;
    }

    public String getDbAttributePath() {
        return dbAttributePath;
    }

    public boolean isCompound() {
        return (dbAttributePath != null && dbAttributePath.indexOf('.') >= 0);
    }

    public boolean mapsToDependentDbEntity() {
        Iterator i = getDbPathIterator();
        if (!i.hasNext()) return false;
        Object o = i.next();
        if (!i.hasNext()) return false;
        Object o1 = i.next();
        if (!(o1 instanceof DbAttribute)) return false;
        DbRelationship toDependent = (DbRelationship)o;
        return toDependent.isToDependentPK();
    }

    public void validate() throws CayenneException {
        String head = "ObjAttribute: " + getName() + " ";
        ObjEntity ent = (ObjEntity)getEntity();
        if(ent == null) {
            throw new CayenneException(head + "Parent ObjEntity not defined.");
        }
        head += "ObjEntity: " + ent.getName() + " ";

        if (getName() == null)
            throw new CayenneException(head + "ObjAttribute's name not defined.");

        if (getDbAttributePath() == null)
            throw new CayenneException(head + "dbAttributePath not defined.");

        try {
            Iterator i = getDbPathIterator();
            boolean dbAttributeFound = false;
            while (i.hasNext()) {
                Object pathPart = i.next();
                if (pathPart instanceof DbRelationship) {
                    DbRelationship r = (DbRelationship)pathPart;
                    if (r.isToMany())
                        throw new CayenneException(
                                head + "DbRelationship: " + r.getName() + " is to-many.");
                } else if (pathPart instanceof DbAttribute) {
                    dbAttributeFound = true;
                }
            }
            if (!dbAttributeFound)
                throw new CayenneException(head + "DbAttribute not found.");
        } catch (CayenneException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CayenneException(head + ex.getMessage(), ex);
        }
    }
}

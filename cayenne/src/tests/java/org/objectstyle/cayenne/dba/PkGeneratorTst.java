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
package org.objectstyle.cayenne.dba;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class PkGeneratorTst extends CayenneTestCase {
	
	protected PkGenerator pkGen;
	protected DataNode node;
	protected DbEntity paintEnt;

	protected void setUp() throws java.lang.Exception {
		getDatabaseSetup().cleanTableData();
		node = (DataNode) getDomain().getDataNodes().iterator().next();
		pkGen = node.getAdapter().getPkGenerator();
		paintEnt =
			node.getEntityResolver().lookupObjEntity("Painting").getDbEntity();
		List list = new ArrayList();
		list.add(paintEnt);
		pkGen.createAutoPk(node, list);
		pkGen.reset();
	}

	public void testGeneratePkForDbEntity() throws Exception {
		List pkList = new ArrayList();
    
        int testSize = (pkGen instanceof JdbcPkGenerator) ? ((JdbcPkGenerator)pkGen).getPkCacheSize() * 2 : 25;
        if(testSize < 25) {
        	testSize = 25;
        }
 
		for (int i = 0; i < testSize; i++) {
			Object pk = pkGen.generatePkForDbEntity(node, paintEnt);
			assertNotNull(pk);
			assertTrue(pk instanceof Number);
			assertFalse(pkList.contains(pk));

			// check that the number is continuous
			// of course this assumes a single-threaded test
			if (pkList.size() > 0) {
               Number last = (Number)pkList.get(pkList.size() - 1);
               assertEquals(last.intValue() + 1, ((Number)pk).intValue());
			}

			pkList.add(pk);
		}
	}
    
    public void testBinaryPK1() throws Exception {
        if (!(pkGen instanceof JdbcPkGenerator)) {
            return;
        }

        DbEntity artistEntity = getDomain().getEntityResolver().lookupDbEntity("Artist");
        assertNull(((JdbcPkGenerator) pkGen).binaryPK(artistEntity));
    }

    public void testBinaryPK2() throws Exception {
        if (!(pkGen instanceof JdbcPkGenerator)) {
            return;
        }

        DbEntity binPKEntity =
            getDomain().getEntityResolver().lookupDbEntity("BinaryPKTest1");
        assertNotNull(((JdbcPkGenerator) pkGen).binaryPK(binPKEntity));
    }
}
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
package org.objectstyle.cayenne;

import org.apache.log4j.Logger;
import org.objectstyle.cayenne.unittest.*;

/**
 * Tests issues not directly related to Cayenne.
 * 
 * @author Andrei Adamchik
 */
public class MiscTst extends CayenneTestCase {
	static Logger logObj = Logger.getLogger(MiscTst.class);

    /**
     * Constructor for MiscTest.
     * @param arg0
     */
    public MiscTst(String arg0) {
        super(arg0);
    }

    public void setUp() throws Exception {
        getDatabaseSetup().cleanTableData();
    }

    public void testNothing() throws Exception {
    	// noop to keep class from failing
    }
    
   /* public void testUTFJDBC() throws Exception {        
        Connection c = super.getSharedConnection();
        try {
            PreparedStatement st =
                c.prepareStatement(
                    "insert into ARTIST (ARTIST_ID, ARTIST_NAME) " + "values (1, ?)");

            try {
                st.setString(1, "\u0424");
                st.execute();
            } finally {
                st.close();
            }

            Statement st1 = c.createStatement();

            try {
                ResultSet rs = st1.executeQuery("select ARTIST_NAME from ARTIST");
                rs.next();
                
                
                String inStr = rs.getString(1);
                assertEquals("Not a unicode.", 1, inStr.length());
                
                char c1 = inStr.charAt(0);
                assertTrue("Bad UNICODE char: " + charToHex(c1), c1 == '\u0424');
            } finally {
                st1.close();
            }

        } finally {
            c.close();
        }
    }
*/
    static public String byteToHex(byte b) {
        // Returns hex String representation of byte b

        char hexDigit[] =
            {
                '0',
                '1',
                '2',
                '3',
                '4',
                '5',
                '6',
                '7',
                '8',
                '9',
                'a',
                'b',
                'c',
                'd',
                'e',
                'f' };
        char[] array = { hexDigit[(b >> 4) & 0x0f], hexDigit[b & 0x0f] };
        return new String(array);
    }

    static public String charToHex(char c) {
        // Returns hex String representation of char c
        byte hi = (byte) (c >>> 8);
        byte lo = (byte) (c & 0xff);
        return byteToHex(hi) + byteToHex(lo);
    }
}

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
 */
package org.objectstyle.cayenne.exp;

import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.unittest.CayenneTestCase;

public class ExpressionFactoryTst extends CayenneTestCase {
    // non-existent type
    private static final int badType = -50;

    public void testUnaryExp() throws Exception {
        Object o1 = new Object();
        Expression e1 = ExpressionFactory.unaryExp(Expression.NOT, o1);
        assertTrue(e1 instanceof UnaryExpression);
        assertSame(o1, e1.getOperand(0));
        assertEquals(Expression.NOT, e1.getType());
    }

    public void testBinaryExp() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();
        Expression e1 = ExpressionFactory.binaryExp(Expression.EQUAL_TO, o1, o2);
        assertTrue(e1 instanceof BinaryExpression);
        assertSame(o1, e1.getOperand(0));
        assertSame(o2, e1.getOperand(1));
        assertEquals(Expression.EQUAL_TO, e1.getType());
    }

    public void testTernaryExp() throws Exception {
        Object o1 = new Object();
        Object o2 = new Object();
        Object o3 = new Object();
        Expression e1 = ExpressionFactory.ternaryExp(Expression.BETWEEN, o1, o2, o3);
        assertTrue(e1 instanceof TernaryExpression);
        assertSame(o1, e1.getOperand(0));
        assertSame(o2, e1.getOperand(1));
        assertSame(o3, e1.getOperand(2));
        assertEquals(Expression.BETWEEN, e1.getType());
    }

    public void testExpressionOfType() throws java.lang.Exception {
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.AND) instanceof ListExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.OR) instanceof ListExpression);

        assertTrue(
            ExpressionFactory.expressionOfType(Expression.NOT)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.EQUAL_TO)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.NOT_EQUAL_TO)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.LESS_THAN)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.GREATER_THAN)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.LESS_THAN_EQUAL_TO)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.GREATER_THAN_EQUAL_TO)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.BETWEEN)
                instanceof TernaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.IN)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.LIKE)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.LIKE_IGNORE_CASE)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.EXISTS)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.ADD)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.SUBTRACT)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.MULTIPLY)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.DIVIDE)
                instanceof BinaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.NEGATIVE)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.POSITIVE)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.ALL)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.SOME)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.ANY)
                instanceof UnaryExpression);

        assertTrue(
            ExpressionFactory.expressionOfType(Expression.RAW_SQL)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.OBJ_PATH)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.DB_PATH)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.LIST)
                instanceof UnaryExpression);

        assertTrue(
            ExpressionFactory.expressionOfType(Expression.SUBQUERY)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.COUNT)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.AVG)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.SUM)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.MAX)
                instanceof UnaryExpression);
        assertTrue(
            ExpressionFactory.expressionOfType(Expression.MIN)
                instanceof UnaryExpression);

    }

    public void testExpressionOfBadType() throws Exception {
        try {
            ExpressionFactory.expressionOfType(badType);
            fail();
        }
        catch (ExpressionException ex) {
            // exception expected   
        }
    }

    public void testBetweenExp() throws Exception {
        Object v1 = new Object();
        Object v2 = new Object();
        Expression exp = ExpressionFactory.betweenExp("abc", v1, v2);
        assertEquals(Expression.BETWEEN, exp.getType());
    }
    
	public void testNotBetweenExp() throws Exception {
		 Object v1 = new Object();
		 Object v2 = new Object();
		 Expression exp = ExpressionFactory.notBetweenExp("abc", v1, v2);
		 assertEquals(Expression.NOT_BETWEEN, exp.getType());
	 }


    public void testGreaterExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterExp("abc", v);
        assertEquals(Expression.GREATER_THAN, exp.getType());
    }

    public void testGreaterOrEqualExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.greaterOrEqualExp("abc", v);
        assertEquals(Expression.GREATER_THAN_EQUAL_TO, exp.getType());
    }

    public void testLessExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessExp("abc", v);
        assertEquals(Expression.LESS_THAN, exp.getType());
    }

    public void testLessOrEqualExp() throws Exception {
        Object v = new Object();
        Expression exp = ExpressionFactory.lessOrEqualExp("abc", v);
        assertEquals(Expression.LESS_THAN_EQUAL_TO, exp.getType());
    }

    public void testInExp1() throws Exception {
        Object[] v = new Object[] { "a", "b" };
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.IN, exp.getType());
    }

    public void testInExp2() throws Exception {
        List v = new ArrayList();
        Expression exp = ExpressionFactory.inExp("abc", v);
        assertEquals(Expression.IN, exp.getType());
    }

    public void testLikeExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeExp("abc", v);
        assertEquals(Expression.LIKE, exp.getType());
    }

    public void testLikeIgnoreCaseExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.likeIgnoreCaseExp("abc", v);
        assertEquals(Expression.LIKE_IGNORE_CASE, exp.getType());
    }

    public void testNotLikeIgnoreCaseExp() throws Exception {
        String v = "abc";
        Expression exp = ExpressionFactory.notLikeIgnoreCaseExp("abc", v);
        assertEquals(Expression.NOT_LIKE_IGNORE_CASE, exp.getType());
    }
}

/* Generated By:JJTree: Do not edit this line. ASTList.java */

package org.objectstyle.cayenne.exp.parser;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.objectstyle.cayenne.exp.Expression;

class ASTList extends SimpleNode {

    public ASTList(int id) {
        super(id);
    }

    public ASTList(ExpressionParser p, int id) {
        super(p, id);
    }

    public int getType() {
        return Expression.LIST;
    }

    protected String getExpressionOperator(int index) {
        return ",";
    }

    public void encodeAsString(PrintWriter pw) {
        pw.print('(');

        if ((children != null) && (children.length > 0)) {
            for (int i = 0; i < children.length; ++i) {
                if (i > 0) {
                    pw.print(getExpressionOperator(i));
                    pw.print(' ');
                }

                ((SimpleNode) children[i]).encodeAsString(pw);
            }
        }

        pw.print(')');
    }

    public int getOperandCount() {
        return 1;
    }

    public Object getOperand(int index) {
        if (index == 0) {
            return value;
        }

        throw new ArrayIndexOutOfBoundsException(index);
    }

    public void jjtClose() {
        // For backwards compatibility set a List value wrapping the nodes.
        int size = jjtGetNumChildren();
        List listValue = new ArrayList(size);
        for (int i = 0; i < size; i++) {
            listValue.add(unwrapChild(jjtGetChild(i)));
        }

        this.value = listValue;
        super.jjtClose();
    }
}

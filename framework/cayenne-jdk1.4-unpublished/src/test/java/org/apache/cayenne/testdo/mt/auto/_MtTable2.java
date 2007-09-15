package org.apache.cayenne.testdo.mt.auto;

/** Class _MtTable2 was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public abstract class _MtTable2 extends org.apache.cayenne.CayenneDataObject {

    public static final String GLOBAL_ATTRIBUTE_PROPERTY = "globalAttribute";
    public static final String TABLE1_PROPERTY = "table1";

    public static final String TABLE2_ID_PK_COLUMN = "TABLE2_ID";

    public void setGlobalAttribute(String globalAttribute) {
        writeProperty("globalAttribute", globalAttribute);
    }
    public String getGlobalAttribute() {
        return (String)readProperty("globalAttribute");
    }
    
    
    public void setTable1(org.apache.cayenne.testdo.mt.MtTable1 table1) {
        setToOneTarget("table1", table1, true);
    }

    public org.apache.cayenne.testdo.mt.MtTable1 getTable1() {
        return (org.apache.cayenne.testdo.mt.MtTable1)readProperty("table1");
    } 
    
    
}

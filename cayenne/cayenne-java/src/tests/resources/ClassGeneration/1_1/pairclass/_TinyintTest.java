package org.apache.art.auto;

/** Class _TinyintTest was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _TinyintTest extends org.apache.cayenne.CayenneDataObject {

    public static final String TINYINT_COL_PROPERTY = "tinyintCol";

    public static final String ID_PK_COLUMN = "ID";

    public void setTinyintCol(Byte tinyintCol) {
        writeProperty("tinyintCol", tinyintCol);
    }
    public Byte getTinyintCol() {
        return (Byte)readProperty("tinyintCol");
    }
    
    
}

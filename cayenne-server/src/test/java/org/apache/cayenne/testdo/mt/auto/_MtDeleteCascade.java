package org.apache.cayenne.testdo.mt.auto;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.testdo.mt.MtDeleteRule;

/**
 * Class _MtDeleteCascade was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _MtDeleteCascade extends CayenneDataObject {

    public static final String NAME_PROPERTY = "name";
    public static final String CASCADE_PROPERTY = "cascade";

    public static final String DELETE_CASCADE_ID_PK_COLUMN = "DELETE_CASCADE_ID";

    public void setName(String name) {
        writeProperty(NAME_PROPERTY, name);
    }
    public String getName() {
        return (String)readProperty(NAME_PROPERTY);
    }

    public void setCascade(MtDeleteRule cascade) {
        setToOneTarget(CASCADE_PROPERTY, cascade, true);
    }

    public MtDeleteRule getCascade() {
        return (MtDeleteRule)readProperty(CASCADE_PROPERTY);
    }


}

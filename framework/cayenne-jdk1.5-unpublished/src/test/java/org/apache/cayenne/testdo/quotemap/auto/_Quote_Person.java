package org.apache.cayenne.testdo.quotemap.auto;

import java.util.Date;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.testdo.quotemap.QuoteAdress;

/**
 * Class _Quote_Person was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually,
 * since it may be overwritten next time code is regenerated.
 * If you need to make any customizations, please use subclass.
 */
public abstract class _Quote_Person extends CayenneDataObject {

    public static final String ADDRESS_ID_PROPERTY = "addressId";
    public static final String D_ATE_PROPERTY = "dAte";
    public static final String F_ULL_NAME_PROPERTY = "fULL_name";
    public static final String GROUP_PROPERTY = "group";
    public static final String NAME_PROPERTY = "name";
    public static final String SALARY_PROPERTY = "salary";
    public static final String ADDRESS_REL_PROPERTY = "addressRel";

    public static final String ID_PK_COLUMN = "id";

    public void setAddressId(Integer addressId) {
        writeProperty("addressId", addressId);
    }
    public Integer getAddressId() {
        return (Integer)readProperty("addressId");
    }

    public void setDAte(Date dAte) {
        writeProperty("dAte", dAte);
    }
    public Date getDAte() {
        return (Date)readProperty("dAte");
    }

    public void setFULL_name(String fULL_name) {
        writeProperty("fULL_name", fULL_name);
    }
    public String getFULL_name() {
        return (String)readProperty("fULL_name");
    }

    public void setGroup(String group) {
        writeProperty("group", group);
    }
    public String getGroup() {
        return (String)readProperty("group");
    }

    public void setName(String name) {
        writeProperty("name", name);
    }
    public String getName() {
        return (String)readProperty("name");
    }

    public void setSalary(Integer salary) {
        writeProperty("salary", salary);
    }
    public Integer getSalary() {
        return (Integer)readProperty("salary");
    }

    public void setAddressRel(QuoteAdress addressRel) {
        setToOneTarget("addressRel", addressRel, true);
    }

    public QuoteAdress getAddressRel() {
        return (QuoteAdress)readProperty("addressRel");
    }


}

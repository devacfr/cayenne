package org.objectstyle.cayenne.examples.aggregate.auto;

/** Class _Painting was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _Painting extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String ESTIMATED_PRICE_PROPERTY = "estimatedPrice";
    public static final String PAINTING_TITLE_PROPERTY = "paintingTitle";
    public static final String ARTIST_PROPERTY = "artist";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setEstimatedPrice(java.math.BigDecimal estimatedPrice) {
        writeProperty("estimatedPrice", estimatedPrice);
    }
    public java.math.BigDecimal getEstimatedPrice() {
        return (java.math.BigDecimal)readProperty("estimatedPrice");
    }
    
    
    public void setPaintingTitle(String paintingTitle) {
        writeProperty("paintingTitle", paintingTitle);
    }
    public String getPaintingTitle() {
        return (String)readProperty("paintingTitle");
    }
    
    
    public void setArtist(org.objectstyle.cayenne.examples.aggregate.Artist artist) {
        setToOneTarget("artist", artist, true);
    }
    
    public org.objectstyle.cayenne.examples.aggregate.Artist getArtist() {
        return (org.objectstyle.cayenne.examples.aggregate.Artist)readProperty("artist");
    } 
    
    
}

package org.apache.art.auto;

import org.apache.art.Painting;
import org.apache.cayenne.CayenneDataObject;

/** 
 * Class _PaintingInfo was generated by Cayenne.
 * It is probably a good idea to avoid changing this class manually, 
 * since it may be overwritten next time code is regenerated. 
 * If you need to make any customizations, please use subclass. 
 */
public class _PaintingInfo extends CayenneDataObject {

    public static final String IMAGE_BLOB_PROPERTY = "imageBlob";
    public static final String TEXT_REVIEW_PROPERTY = "textReview";
    public static final String PAINTING_PROPERTY = "painting";

    public static final String PAINTING_ID_PK_COLUMN = "PAINTING_ID";

    public void setImageBlob(byte[] imageBlob) {
        writeProperty("imageBlob", imageBlob);
    }
    public byte[] getImageBlob() {
        return (byte[])readProperty("imageBlob");
    }
    
    
    public void setTextReview(String textReview) {
        writeProperty("textReview", textReview);
    }
    public String getTextReview() {
        return (String)readProperty("textReview");
    }
    
    
    public void setPainting(Painting painting) {
        setToOneTarget("painting", painting, true);
    }

    public Painting getPainting() {
        return (Painting)readProperty("painting");
    } 
    
    
}

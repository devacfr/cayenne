package cayenne.tutorial.tapestry.domain.auto;

import java.util.List;

/** Class _Gallery was generated by Cayenne.
  * It is probably a good idea to avoid changing this class manually, 
  * since it may be overwritten next time code is regenerated. 
  * If you need to make any customizations, please use subclass. 
  */
public class _Gallery extends org.objectstyle.cayenne.CayenneDataObject {

    public static final String GALLERY_NAME_PROPERTY = "galleryName";
    public static final String PAINTING_ARRAY_PROPERTY = "paintingArray";

    public static final String GALLERY_ID_PK_COLUMN = "GALLERY_ID";

    public void setGalleryName(String galleryName) {
        writeProperty("galleryName", galleryName);
    }
    public String getGalleryName() {
        return (String)readProperty("galleryName");
    }
    
    
    public void addToPaintingArray(cayenne.tutorial.tapestry.domain.Painting obj) {
        addToManyTarget("paintingArray", obj, true);
    }
    public void removeFromPaintingArray(cayenne.tutorial.tapestry.domain.Painting obj) {
        removeToManyTarget("paintingArray", obj, true);
    }
    public List getPaintingArray() {
        return (List)readProperty("paintingArray");
    }
    
    
}

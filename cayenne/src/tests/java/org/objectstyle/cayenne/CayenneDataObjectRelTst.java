/* ====================================================================
 * 
 * The ObjectStyle Group Software License, Version 1.0 
 *
 * Copyright (c) 2002-2003 The ObjectStyle Group 
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

import java.util.List;

import org.objectstyle.art.ArtGroup;
import org.objectstyle.art.Artist;
import org.objectstyle.art.FlattenedTest1;
import org.objectstyle.art.FlattenedTest2;
import org.objectstyle.art.FlattenedTest3;
import org.objectstyle.art.Gallery;
import org.objectstyle.art.Painting;
import org.objectstyle.art.PaintingInfo;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

public class CayenneDataObjectRelTst extends CayenneDOTestBase {

    private void prepareNestedProperties() throws Exception {
        Artist a1 = super.newArtist();
        Painting p1 = super.newPainting();
        PaintingInfo pi1 = super.newPaintingInfo();
        Gallery g1 = super.newGallery();

        p1.setToArtist(a1);
        p1.setToPaintingInfo(pi1);
        p1.setToGallery(g1);
        ctxt.commitChanges();
        resetContext();
    }

    public void testReadNestedProperty1() throws Exception {
        prepareNestedProperties();

        Painting p1 = fetchPainting();
        assertEquals(artistName, p1.readNestedProperty("toArtist.artistName"));
    }

    public void testReadNestedProperty2() throws Exception {
        prepareNestedProperties();

        Painting p1 = fetchPainting();
        assertTrue(p1.getToArtist().readNestedProperty("paintingArray") instanceof List);
    }

    public void testReciprocalRel1() throws Exception {
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Painting p1 = fetchPainting();
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(artistName, a1.getArtistName());

        List paintings = a1.getPaintingArray();
        assertEquals(1, paintings.size());
        Painting p2 = (Painting) paintings.get(0);
        assertSame(p1, p2);
    }

    public void testReadToOneRel1() throws Exception {
        // read to-one relationship
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Painting p1 = fetchPainting();
        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(artistName, a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testReadToOneRel2() throws Exception {
        // test chained calls to read relationships
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            true);

        PaintingInfo pi1 = fetchPaintingInfo(paintingName);
        Painting p1 = pi1.getPainting();
        p1.getPaintingTitle();

        Artist a1 = p1.getToArtist();

        assertNotNull(a1);
        assertEquals(PersistenceState.HOLLOW, a1.getPersistenceState());
        assertEquals(artistName, a1.getArtistName());
        assertEquals(PersistenceState.COMMITTED, a1.getPersistenceState());
    }

    public void testReadToOneRel3() throws Exception {
        // test null relationship destination
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Painting p1 = fetchPainting();
        Gallery g1 = p1.getToGallery();
        assertNull(g1);
    }

    public void testReadToManyRel1() throws Exception {
        TestCaseDataFactory.createArtistWithPainting(
            artistName,
            new String[] { paintingName },
            false);

        Artist a1 = fetchArtist();
        List plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(1, plist.size());
        assertEquals(
            PersistenceState.COMMITTED,
            ((Painting) plist.get(0)).getPersistenceState());
        assertEquals(paintingName, ((Painting) plist.get(0)).getPaintingTitle());
    }

    public void testReadToManyRel2() throws Exception {
        // test empty relationship
        TestCaseDataFactory.createArtistWithPainting(artistName, new String[] {
        }, false);

        Artist a1 = fetchArtist();
        List plist = a1.getPaintingArray();

        assertNotNull(plist);
        assertEquals(0, plist.size());
    }

    public void testReadFlattenedRelationship() throws Exception {
        //Test no groups
        TestCaseDataFactory.createArtistBelongingToGroups(artistName, new String[] {
        });

        Artist a1 = fetchArtist();
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(0, groupList.size());
    }

    public void testReadFlattenedRelationship2() throws Exception {
        //Test no groups
        TestCaseDataFactory.createArtistBelongingToGroups(
            artistName,
            new String[] { groupName });

        Artist a1 = fetchArtist();
        List groupList = a1.getGroupArray();
        assertNotNull(groupList);
        assertEquals(1, groupList.size());
        assertEquals(
            PersistenceState.COMMITTED,
            ((ArtGroup) groupList.get(0)).getPersistenceState());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());
    }

    public void testAddToFlattenedRelationship() throws Exception {
        TestCaseDataFactory.createArtist(artistName);
        TestCaseDataFactory.createUnconnectedGroup(groupName);

        Artist a1 = fetchArtist();
        assertEquals(0, a1.getGroupArray().size());

        SelectQuery q =
            new SelectQuery(
                ArtGroup.class,
                ExpressionFactory.matchExp("name", groupName));
        List results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        assertFalse(ctxt.hasChanges());
        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
		assertTrue(ctxt.hasChanges());
		
        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());

        //Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        //and check again
		assertFalse(ctxt.hasChanges());
        groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());
    }

    //Test case to show up a bug in committing more than once
    public void testDoubleCommitAddToFlattenedRelationship() throws Exception {
        TestCaseDataFactory.createArtistBelongingToGroups(artistName, new String[] {
        });
        TestCaseDataFactory.createUnconnectedGroup(groupName);
        Artist a1 = fetchArtist();

        SelectQuery q =
            new SelectQuery(
                ArtGroup.class,
                ExpressionFactory.binaryPathExp(Expression.EQUAL_TO, "name", groupName));
        List results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(1, groupList.size());
        assertEquals(groupName, ((ArtGroup) groupList.get(0)).getName());

        //Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        try {
            //The bug caused the second commit to fail (the link record
            // was inserted again)
            a1.getDataContext().commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown an exception");
        }

    }

    public void testRemoveFromFlattenedRelationship() throws Exception {
        TestCaseDataFactory.createArtistBelongingToGroups(
            artistName,
            new String[] { groupName });
        Artist a1 = fetchArtist();

        ArtGroup group = (ArtGroup) a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group);

        List groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());

        //Ensure that the commit doesn't fail
        a1.getDataContext().commitChanges();

        //and check again
        groupList = a1.getGroupArray();
        assertEquals(0, groupList.size());
    }

    //Shows up a possible bug in ordering of deletes, when a flattened relationships link record is deleted
    // at the same time (same transaction) as one of the record to which it links.
    public void testRemoveFlattenedRelationshipAndRootRecord() throws Exception {
        TestCaseDataFactory.createArtistBelongingToGroups(
            artistName,
            new String[] { groupName });
        Artist a1 = fetchArtist();
        DataContext dc = a1.getDataContext();

        ArtGroup group = (ArtGroup) a1.getGroupArray().get(0);
        a1.removeFromGroupArray(group); //Cause the delete of the link record

        dc.deleteObject(a1); //Cause the deletion of the artist

        try {
            dc.commitChanges();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Should not have thrown the exception :" + e.getMessage());
        }
    }

    /* Catches a bug in the flattened relationship registration which just inserted/deleted willy-nilly, 
     * even if unneccessary */
    public void testAddRemoveAddFlattenedRelationship() throws Exception {
        String specialGroupName = "Special Group2";
        TestCaseDataFactory.createArtistBelongingToGroups(artistName, new String[] {
        });
        TestCaseDataFactory.createUnconnectedGroup(specialGroupName);
        Artist a1 = fetchArtist();

        SelectQuery q =
            new SelectQuery(
                ArtGroup.class,
                ExpressionFactory.binaryPathExp(
                    Expression.EQUAL_TO,
                    "name",
                    specialGroupName));
        List results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        ArtGroup group = (ArtGroup) results.get(0);
        a1.addToGroupArray(group);
        group.removeFromArtistArray(a1);
        //a1.addToGroupArray(group);

        try {
            ctxt.commitChanges();
        }
        catch (Exception e) {
            Util.unwindException(e).printStackTrace();
            fail("Should not have thrown the exception " + e.getMessage());
        }

        this.resetContext();
        results = ctxt.performQuery(q);
        assertEquals(1, results.size());

        group = (ArtGroup) results.get(0);
        assertEquals(0, group.getArtistArray().size());
        //a1 = fetchArtist();
        //assertTrue(group.getArtistArray().contains(a1));
    }

    public void testToOneSeriesFlattenedRel() {
        FlattenedTest1 ft1 =
            (FlattenedTest1) ctxt.createAndRegisterNewObject("FlattenedTest1");
        ft1.setName("FT1Name");
        FlattenedTest2 ft2 =
            (FlattenedTest2) ctxt.createAndRegisterNewObject("FlattenedTest2");
        ft2.setName("FT2Name");
        FlattenedTest3 ft3 =
            (FlattenedTest3) ctxt.createAndRegisterNewObject("FlattenedTest3");
        ft3.setName("FT3Name");

        ft2.setToFT1(ft1);
        ft2.addToFt3Array(ft3);
        ctxt.commitChanges();

        this.resetContext(); //We need a new context
        SelectQuery q = new SelectQuery(FlattenedTest3.class);
        q.setQualifier(ExpressionFactory.matchExp("name", "FT3Name"));
        List results = ctxt.performQuery(q);

        assertEquals(1, results.size());

        FlattenedTest3 fetchedFT3 = (FlattenedTest3) results.get(0);
        FlattenedTest1 fetchedFT1 = fetchedFT3.getToFT1();
        assertEquals("FT1Name", fetchedFT1.getName());

    }

    public void testReflexiveRelationshipInsertOrder1() {
        DataContext dc = this.createDataContext();
        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder2() {
        //Create in a different order and see what happens
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder3() {
        //Tey multiple children, one created before parent, one after
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        dc.commitChanges();
    }

    public void testReflexiveRelationshipInsertOrder4() {
        //Tey multiple children, one created before parent, one after
        DataContext dc = this.createDataContext();
        ArtGroup childGroup1 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup1.setName("child1");

        ArtGroup parentGroup = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        parentGroup.setName("parent");

        childGroup1.setToParentGroup(parentGroup);

        ArtGroup childGroup2 = (ArtGroup) dc.createAndRegisterNewObject("ArtGroup");
        childGroup2.setName("subchild");
        childGroup2.setToParentGroup(childGroup1);

        dc.commitChanges();
    }

    public void testCrossContextRelationshipException() {
        DataContext otherContext = getDomain().createDataContext();
        //Create this object in one context...
        Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
        //...and this object in another context
        Painting painting =
            (Painting) otherContext.createAndRegisterNewObject("Painting");

        //Check setting a toOne relationship
        try {
            painting.setToArtist(artist);
            fail("Should have failed to set a cross-context relationship");
        }
        catch (CayenneRuntimeException e) {
            //Fine.. it should  throw an exception
        }

        assertNull(painting.getToArtist()); //Make sure it wasn't set

        //Now try the reverse (toMany) relationship
        try {
            artist.addToPaintingArray(painting);
            fail("Should have failed to add a cross-context relationship");
        }
        catch (CayenneRuntimeException e) {
            //Fine.. it should  throw an exception
        }

        assertEquals(0, artist.getPaintingArray().size());

    }

    public void testComplexInsertUpdateOrdering() {
        Artist artist = (Artist) ctxt.createAndRegisterNewObject("Artist");
        artist.setArtistName("a name");

        ctxt.commitChanges();

        //Cause an update and an insert that need correct ordering
        Painting painting = (Painting) ctxt.createAndRegisterNewObject("Painting");
        painting.setPaintingTitle("a painting");
        artist.addToPaintingArray(painting);

        ctxt.commitChanges();

        ctxt.deleteObject(artist);
        ctxt.commitChanges();
    }

    private PaintingInfo fetchPaintingInfo(String name) {
        SelectQuery q =
            new SelectQuery(
                "PaintingInfo",
                ExpressionFactory.binaryPathExp(
                    Expression.EQUAL_TO,
                    "painting.paintingTitle",
                    name));
        List pts = ctxt.performQuery(q);
        return (pts.size() > 0) ? (PaintingInfo) pts.get(0) : null;
    }
}

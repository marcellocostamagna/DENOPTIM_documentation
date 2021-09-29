package denoptim.molecule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
 *   and Marco Foscato <marco.foscato@uib.no>
 * 
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMVertex.BBType;

/**
 * Unit test for DENOPTIMAttachmentPoint
 * 
 * @author Marco Foscato
 */

public class DENOPTIMAttachmentPointTest
{
	private final int ATMID = 6;
	private final int APCONN = 3;
	private final String APRULE = "MyRule";
	private final String APSUBRULE = "1";
	private final String APCLASS = APRULE
			+ DENOPTIMConstants.SEPARATORAPPROPSCL + APSUBRULE;
	private final double[] DIRVEC = new double[]{1.1, 2.2, 3.3};
	private final EmptyVertex dummyVertex = new EmptyVertex();
	
//-----------------------------------------------------------------------------
	
	@Test
	public void testAvailableThrougout() throws Exception
	{
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMVertex vA = new EmptyVertex(0);
        vA.addAP(0,1,1);
        vA.addAP(1,1,1);
        vA.addAP(2,1,1);
        vA.addAP(3,1,1);
        vA.addAP(4,1,1);
        vA.addAP(5,1,1);
        vA.addAP(6,1,1);
        DENOPTIMVertex vB = new EmptyVertex(1);
        vB.addAP(0,1,1);
        vB.addAP(0,1,1);
        
        DENOPTIMGraph gL0 = new DENOPTIMGraph();
        gL0.addVertex(vA);
        gL0.appendVertexOnAP(vA.getAP(0), vB.getAP(1));
        
        DENOPTIMTemplate tL0 = new DENOPTIMTemplate(BBType.NONE);
        tL0.setInnerGraph(gL0);
        
        int[] expectedN = {6,6,6,6};
        int[] expectedNThroughout = {6,5,4,3};
        
        checkAvailNT(expectedN[0], expectedNThroughout[0], 0, vA);

        DENOPTIMVertex old = tL0;
        for (int i=1; i<4; i++)
        {
            DENOPTIMVertex vNew = new EmptyVertex(1);
            vNew.addAP(0,1,1);
            DENOPTIMGraph gNew = new DENOPTIMGraph();
            gNew.addVertex(old);
            gNew.appendVertexOnAP(old.getAP(0), vNew.getAP(0));
            DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
            template.setInnerGraph(gNew);
            checkAvailNT(expectedN[i], expectedNThroughout[i], i, vA);
            old = template;
        }
	}
	
//-----------------------------------------------------------------------------
	
    private void checkAvailNT(int expN, int expNTm, int level, DENOPTIMVertex v)
    {
        int n = 0;
        int nThroughout = 0;
        for (DENOPTIMAttachmentPoint apA : v.getAttachmentPoints())
        {
            if (apA.isAvailable())
                n++;
            if (apA.isAvailableThroughout())
                nThroughout++;
        }
        assertEquals(expN,n,"Number of level-available ("+level+"");
        assertEquals(expNTm,nThroughout,"Number of throughout-available ("+level+")");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testGetEdbeUserThrougout() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMVertex vA = new EmptyVertex(0);
        vA.addAP(0,1,1);
        vA.addAP(1,1,1);
        vA.addAP(2,1,1);
        vA.addAP(3,1,1);
        vA.addAP(4,1,1);
        
        DENOPTIMGraph gL0 = new DENOPTIMGraph();
        gL0.addVertex(vA);
        DENOPTIMTemplate tL0 = new DENOPTIMTemplate(BBType.NONE);
        tL0.setInnerGraph(gL0);
        
        List<DENOPTIMVertex> newVrtxs = new ArrayList<DENOPTIMVertex>();
        
        DENOPTIMVertex old = tL0;
        for (int i=0; i<5; i++)
        {
            DENOPTIMVertex vNew = new EmptyVertex(1);
            vNew.addAP(i,1,1);
            newVrtxs.add(vNew);
            DENOPTIMGraph gNew = new DENOPTIMGraph();
            gNew.addVertex(old);
            gNew.appendVertexOnAP(old.getAP(0), vNew.getAP(0));
            DENOPTIMTemplate template = new DENOPTIMTemplate(BBType.NONE);
            template.setInnerGraph(gNew);
            checkGetEdgeUserThroughput(vA, newVrtxs);
            old = template;
        }
    }
    
//-----------------------------------------------------------------------------
    
    private void checkGetEdgeUserThroughput(DENOPTIMVertex v,
            List<DENOPTIMVertex> onion)
    {
        int i = -1;
        for (DENOPTIMAttachmentPoint apA : v.getAttachmentPoints())
        {
            i++;
            assertTrue(apA.isAvailable(), "APs of vA are all free within the "
                    + "graph owning vA.");
            DENOPTIMEdge e = apA.getEdgeUserThroughout();
            if (e != null)
            {
                DENOPTIMAttachmentPoint inAP = e.getSrcAP();
                while (true)
                {
                    DENOPTIMVertex src = inAP.getOwner();
                    if (src instanceof DENOPTIMTemplate)
                    {
                        inAP = ((DENOPTIMTemplate) src).getInnerAPFromOuterAP(
                                inAP);
                    } else {
                        break;
                    }
                }
                /*
                System.out.println(">>>> "+apA);
                System.out.println("     "+inAP);
                System.out.println("---- "+onion.get(i).getAP(0));
                System.out.println("     "+e.getTrgAP());
                */
                assertEquals(apA,inAP,"Src AP identity");
                assertEquals(onion.get(i).getAP(0),e.getTrgAP(), "Trg AP identity");
            } else {
                //System.out.println(apA.toString()+" free");
            }
        }
    }
    
//-----------------------------------------------------------------------------
	
	@Test
	public void testIsSrcInUser() throws Exception
	{
	    // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        EmptyVertex v1 = new EmptyVertex();
        v1.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        v1.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap1A = v1.getAP(0);
        DENOPTIMAttachmentPoint ap1B = v1.getAP(1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        v2.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap2A = v2.getAP(0);
        DENOPTIMAttachmentPoint ap2B = v2.getAP(1);
        g.addVertex(v1);
        g.appendVertexOnAP(ap1A, ap2B);
        
        assertTrue(ap1A.isSrcInUser(), "Check AP used as src.");
        assertFalse(ap2B.isSrcInUser(), "Check AP used as trg.");
        assertFalse(ap1B.isSrcInUser(), "Check AP free on src side.");
        assertFalse(ap2A.isSrcInUser(), "Check AP free on trg side.");
	}

//-----------------------------------------------------------------------------
	
	@Test
	public void testGetLinkedAP() throws Exception
	{
	    // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);
        
	    DENOPTIMGraph g = new DENOPTIMGraph();
	    EmptyVertex v1 = new EmptyVertex();
        v1.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        v1.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap1A = v1.getAP(0);
        DENOPTIMAttachmentPoint ap1B = v1.getAP(1);
        EmptyVertex v2 = new EmptyVertex();
        v2.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        v2.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint ap2A = v2.getAP(0);
        DENOPTIMAttachmentPoint ap2B = v2.getAP(1);
        g.addVertex(v1);
        g.appendVertexOnAP(ap1A, ap2B);
        
        assertTrue(ap1A.getLinkedAP() == ap2B, "Get AP on other side of ap1A");
        assertTrue(ap2B.getLinkedAP() == ap1A, "Get AP on other dice of ap2");
        assertNull(ap1B.getLinkedAP(), "Free AP 1B should return null");
        assertNull(ap2A.getLinkedAP(), "Free AP 2A should return null");
	}
	
//-----------------------------------------------------------------------------
	
    @Test
    public void testConstructorsAndSDFString() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);

		dummyVertex.addAP(ATMID, APCONN, APCONN, DIRVEC, APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap = dummyVertex.getAP(0);
    	
    	String str2 = ap.getSingleAPStringSDF(true);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, str2);
    	
    	String str3 = (ATMID+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex
				, str3);
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toStringNoId(),ap3.toStringNoId());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testConstructorsAndSDFStringNoDirVec() throws Exception
    {
		dummyVertex.addAP(ATMID, APCONN, APCONN, APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap = dummyVertex.getAP(0);

    	String str2 = ap.getSingleAPStringSDF(true);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, str2);
    	
    	String str3 = (ATMID+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex
				, str3);
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toStringNoId(),ap3.toStringNoId());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSortAPs() throws Exception
    {
    	dummyVertex.addAP(0,2,1);
    	dummyVertex.addAP(0,1,1);
		DENOPTIMAttachmentPoint ap1 = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint ap2 = dummyVertex.getAP(1);

		dummyVertex.addAP(4, APCONN, APCONN, DIRVEC, APClass.make("AA:0"));
		DENOPTIMAttachmentPoint ap3 = dummyVertex.getAP(2);

		dummyVertex.addAP(4, APCONN, APCONN, DIRVEC, APClass.make("AA:1"));
		DENOPTIMAttachmentPoint ap4 = dummyVertex.getAP(3);

		dummyVertex.addAP(5, APCONN, APCONN, new double[]{1.1, 2.2, 3.3},
				APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap5 = dummyVertex.getAP(4);

		dummyVertex.addAP(5, APCONN, APCONN, new double[]{2.2, 2.2, 3.3},
				APClass.make(APCLASS));
		DENOPTIMAttachmentPoint ap6 = dummyVertex.getAP(5);

    	ArrayList<DENOPTIMAttachmentPoint> list =
				dummyVertex.getAttachmentPoints();
    	
    	list.sort(new DENOPTIMAttachmentPointComparator());
    	
    	assertEquals(list.get(0),ap1);
    	assertEquals(list.get(1),ap2);
    	assertEquals(list.get(2),ap3);
    	assertEquals(list.get(3),ap4);
    	assertEquals(list.get(4),ap5);
    	assertEquals(list.get(5),ap6);
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs()
    {
		dummyVertex.addAP(1, 2, 1);
		dummyVertex.addAP(1, 2, 1);
		DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertEquals(-1,apA.compareTo(apB),"Comparison driven by ID.");
    	assertTrue(apA.sameAs(apB),"SameAs ignores ID.");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffSrcAtm()
    {
		dummyVertex.addAP(1, 2, 1);
		dummyVertex.addAP(2, 2, 1);
    	DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
    	DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertFalse(apA.sameAs(apB));
    }

//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_SameAPClass() throws Exception
    {
		APClass apClass = APClass.make("classA:0");
		dummyVertex.addAP(1, 2, 1, apClass);
    	dummyVertex.addAP(1, 2, 1, apClass);
    	DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
    	DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);

    	assertTrue(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSameAs_DiffAPClass() throws Exception
    {
    	dummyVertex.addAP(1, 2, 1, APClass.make("classA:0"));
    	dummyVertex.addAP(1, 2, 1, APClass.make("classB:0"));

		DENOPTIMAttachmentPoint apA = dummyVertex.getAP(0);
		DENOPTIMAttachmentPoint apB = dummyVertex.getAP(1);
		assertFalse(apA.sameAs(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        dummyVertex.addAP(1, 2, 1, APClass.make(APCLASS));
        DENOPTIMAttachmentPoint orig = dummyVertex.getAP(
                dummyVertex.getNumberOfAPs()-1);
        
        DENOPTIMAttachmentPoint clone = orig.clone();

        /* This may not always work as hashing only guarantees that if
        objectA == objectB then objectA.hashCode() == objectB.hashCode(). I.e
        two objects with the same hash code need not be equal.*/
        assertEquals(clone.getAPClass().hashCode(),
                orig.getAPClass().hashCode(),"Hashcode of cloned APClass");
    }
    
//------------------------------------------------------------------------------
    
}

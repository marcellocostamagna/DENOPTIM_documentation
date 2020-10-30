package denoptim.molecule;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.molecule.DENOPTIMEdge.BondType;

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
    public void testConstructorsAndSDFString() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        HashMap<String, BondType> map = new HashMap<String, BondType>();
        map.put(APRULE,BondType.SINGLE);
        FragmentSpace.setBondOrderMap(map);

		DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(dummyVertex);
    	ap.setAPClass(APCLASS);
    	ap.setFreeConnections(APCONN);
    	ap.setAtomPositionNumber(ATMID);
    	ap.setDirectionVector(DIRVEC);
    	
    	String str2 = ap.getSingleAPStringSDF(true);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, str2,
				"SDF");
    	
    	String str3 = (ATMID+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex
				, str3,
				"SDF");
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toString(),ap3.toString());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testConstructorsAndSDFStringNoDirVec() throws Exception
    {
    	DENOPTIMAttachmentPoint ap = new DENOPTIMAttachmentPoint(dummyVertex);
    	ap.setAPClass(APCLASS);
    	ap.setFreeConnections(APCONN);
    	ap.setAtomPositionNumber(ATMID);
    	
    	String str2 = ap.getSingleAPStringSDF(true);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, str2,"SDF");
    	
    	String str3 = (ATMID+1) + DENOPTIMConstants.SEPARATORAPPROPAAP
    			+ ap.getSingleAPStringSDF(false);
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex
				, str3,
				"SDF");
    	
    	assertEquals(ap.getSingleAPStringSDF(true),
    			ap2.getSingleAPStringSDF(true));
    	assertEquals(ap2.toString(),ap3.toString());
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testSortAPs() throws Exception
    {
    	DENOPTIMAttachmentPoint ap1 =
				new DENOPTIMAttachmentPoint(dummyVertex, 0,2,1);
    	DENOPTIMAttachmentPoint ap2 = new DENOPTIMAttachmentPoint(dummyVertex
				, 1,1,1);
    	
    	DENOPTIMAttachmentPoint ap3 = new DENOPTIMAttachmentPoint(dummyVertex);
    	ap3.setAPClass("AA:0");
    	ap3.setAtomPositionNumber(4);
    	ap3.setDirectionVector(DIRVEC);
    	
    	DENOPTIMAttachmentPoint ap4 = new DENOPTIMAttachmentPoint(dummyVertex);
    	ap4.setAPClass("AA:1");
    	ap4.setAtomPositionNumber(4);
    	ap4.setDirectionVector(DIRVEC);
    	
    	DENOPTIMAttachmentPoint ap5 = new DENOPTIMAttachmentPoint(dummyVertex);
    	ap5.setAPClass(APCLASS);
    	ap5.setAtomPositionNumber(5);
    	ap5.setDirectionVector(new double[]{1.1, 2.2, 3.3});

    	DENOPTIMAttachmentPoint ap6 = new DENOPTIMAttachmentPoint(dummyVertex);
    	ap6.setAPClass(APCLASS);
    	ap6.setAtomPositionNumber(5);
    	ap6.setDirectionVector(new double[]{2.2, 2.2, 3.3});
    	
    	ArrayList<DENOPTIMAttachmentPoint> list = 
    			new ArrayList<DENOPTIMAttachmentPoint>();
    	
    	list.add(ap6);
    	list.add(ap5);
    	list.add(ap4);
    	list.add(ap3);
    	list.add(ap2);
    	list.add(ap1);
    	
    	Collections.sort(list, new DENOPTIMAttachmentPointComparator());
    	
    	assertEquals(list.get(0),ap1);
    	assertEquals(list.get(1),ap2);
    	assertEquals(list.get(2),ap3);
    	assertEquals(list.get(3),ap4);
    	assertEquals(list.get(4),ap5);
    	assertEquals(list.get(5),ap6);
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testEquals() throws Exception
    {
    	DENOPTIMAttachmentPoint apA = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	DENOPTIMAttachmentPoint apB = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	
    	assertEquals(-1,apA.compareTo(apB),"Comparison driven by ID.");
    	assertTrue(apA.equals(apB),"Equals ignores ID.");
    	assertEquals(0,apA.comparePropertiesTo(apB),
    	        "Property-based comparison.");
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testEquals_DiffArcAtm() throws Exception
    {
    	DENOPTIMAttachmentPoint apA = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	DENOPTIMAttachmentPoint apB = new DENOPTIMAttachmentPoint(dummyVertex
				, 2, 2, 1);
    	
    	assertFalse(apA.equals(apB));
    }

//-----------------------------------------------------------------------------
    
    @Test
    public void testEquals_SameAPClass() throws Exception
    {
    	DENOPTIMAttachmentPoint apA = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	apA.setAPClass("classA:0");
    	DENOPTIMAttachmentPoint apB = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	apB.setAPClass("classA:0");
    	
    	assertTrue(apA.equals(apB));
    }
    
//-----------------------------------------------------------------------------
    
    @Test
    public void testEquals_DiffAPClass() throws Exception
    {
    	DENOPTIMAttachmentPoint apA = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	apA.setAPClass("classA:0");
    	DENOPTIMAttachmentPoint apB = new DENOPTIMAttachmentPoint(dummyVertex
				, 1, 2, 1);
    	apB.setAPClass("classB:0");
    	
    	assertFalse(apA.equals(apB));
    }
    
//------------------------------------------------------------------------------
}

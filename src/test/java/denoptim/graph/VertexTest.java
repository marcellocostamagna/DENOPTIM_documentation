/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
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

package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.graph.Vertex.BBType;
import denoptim.graph.Vertex.VertexType;
import denoptim.utils.MutationType;

/**
 * Unit test for DENOPTIMVertex
 * 
 * @author Marco Foscato
 */

public class VertexTest
{
	private StringBuilder reason = new StringBuilder();
	
//------------------------------------------------------------------------------

    @Test
    public void testFromToJSON_minimal() throws Exception 
    {   
        EmptyVertex ev = new EmptyVertex();
        assertEquals(VertexType.EmptyVertex,ev.vertexType);
        String evStr = ev.toJson();
        Vertex ev2 = Vertex.fromJson(evStr);
        assertTrue(ev2 instanceof EmptyVertex);
        
        Fragment f = new Fragment();
        assertEquals(VertexType.MolecularFragment,f.vertexType);
        String fStr = f.toJson();
        Vertex f2 = Vertex.fromJson(fStr);
        assertTrue(f2 instanceof Fragment);
        
        Template t = new Template(BBType.FRAGMENT);
        assertEquals(VertexType.Template,t.vertexType);
        String tStr = t.toJson();
        Vertex t2 = Vertex.fromJson(tStr);
        assertTrue(t2 instanceof Template);
    }
	
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_Equal()
    {
        EmptyVertex vA = new EmptyVertex(0);
        EmptyVertex vB = new EmptyVertex(90);
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();

        vB.addAP();
        vB.addAP();
        vB.addAP();
        vB.addAP();
        //NB: vertex ID must be ignores by the sameAs method
    	assertTrue(vA.sameAs(vB, reason));	
    	
    	// ... one can use properties to uniquefy empty vertexes
    	String k = "MyPropKey";
    	vA.setProperty(k, 123);
    	vA.setUniquefyingProperty(k);
        vB.setProperty(k, 123);
        vB.setUniquefyingProperty(k);
        // if the value of the uniquefying property is the same
        assertTrue(vA.sameAs(vB, reason));  
        
        // otherwise
        vB.setProperty(k, 456);
        assertFalse(vA.sameAs(vB, reason)); 
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPNum()
    {
        EmptyVertex vA = new EmptyVertex(0);
        vA.addAP();
        vA.addAP();
        vA.addAP();
        vA.addAP();

        EmptyVertex vB = new EmptyVertex(90);
        vB.addAP();
        vB.addAP();
        vB.addAP();
        //NB: vertex ID must be ignores by the sameAs method

        assertFalse(vA.sameAs(vB, reason));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        EmptyVertex v = new EmptyVertex(0);
        v.addAP();
        v.addAP();
        v.addAP();
        v.setProperty("PROPNAME","PROVALUE");
        
        Vertex c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAPs(), c.getNumberOfAPs(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code"); 
        assertEquals("PROVALUE",c.getProperty("PROPNAME"));
        
        
        Fragment v2 = new Fragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        v2.addAtom(a1);
        v2.addAtom(a2);
        v2.addAtom(a3);
        v2.addBond(new Bond(a1, a2));
        v2.addBond(new Bond(a2, a3));
        String APCLASS = "apc" + DENOPTIMConstants.SEPARATORAPPROPSCL +"0";
        v2.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 2.2, 3.3}));
        v2.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 3.3}));
        v2.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 1.1}));
        v2.addAPOnAtom(a1, APClass.make(APCLASS), new Point3d(
                new double[]{3.0, 0.0, 3.3}));
        
        Vertex c2 = v2.clone();
        
        assertEquals(v2.getVertexId(), c2.getVertexId(), "Vertex ID");
        assertEquals(v2.getNumberOfAPs(), c2.getNumberOfAPs(), "Number of APS");
        assertEquals(v2.getSymmetricAPSets().size(), 
                c2.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v2.isRCV(), c2.isRCV(), "RCV flag");
        assertNotEquals(v2.hashCode(), c2.hashCode(), "Hash code");
        assertEquals(v2.getAllAPClasses(),c2.getAllAPClasses(),"APClass list");
        assertEquals(v2.getAllAPClasses().get(0).hashCode(),
                c2.getAllAPClasses().get(0).hashCode(),"APClass hash code");
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testGetMutationSites() throws Exception
    {
        Vertex v = new EmptyVertex(Vertex.BBType.FRAGMENT);
        assertEquals(1,v.getMutationSites().size(),
                "Fragments return themselves as mutable sites.");
        v = new EmptyVertex(Vertex.BBType.SCAFFOLD);
        assertEquals(0,v.getMutationSites().size(),
                "Scaffolds so not return any mutable site.");
        v = new EmptyVertex(Vertex.BBType.CAP);
        assertEquals(0,v.getMutationSites().size(),
                "Capping groups so not return any mutable site.");
        v = new EmptyVertex(Vertex.BBType.UNDEFINED);
        assertEquals(1,v.getMutationSites().size(),
                "Undefined building block return themselves as mutable sites.");
        v = new EmptyVertex(Vertex.BBType.NONE);
        assertEquals(1,v.getMutationSites().size(),
                "'None' building block return themselves as mutable sites.");
        
        v.setMutationTypes(new ArrayList<>(Arrays.asList(MutationType.EXTEND)));
        assertEquals(1,v.getMutationSites().size(),
                "Consistency with restricted list of mutation types.");
        assertEquals(0,v.getMutationSites(new ArrayList<>(Arrays.asList(
                MutationType.EXTEND))).size(), "Vertex that allows only "
                        + "ignored mutation types is not a mutable site");
    }
    
//------------------------------------------------------------------------------
}

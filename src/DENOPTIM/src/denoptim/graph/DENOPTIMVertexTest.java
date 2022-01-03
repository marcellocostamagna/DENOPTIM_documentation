package denoptim.graph;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import javax.vecmath.Point3d;

import org.junit.jupiter.api.Test;
import org.openscience.cdk.Atom;
import org.openscience.cdk.silent.Bond;

import denoptim.constants.DENOPTIMConstants;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.DENOPTIMVertex.VertexType;
import denoptim.utils.MutationType;

/**
 * Unit test for DENOPTIMVertex
 * 
 * @author Marco Foscato
 */

public class DENOPTIMVertexTest
{
	private StringBuilder reason = new StringBuilder();
	
//------------------------------------------------------------------------------

    @Test
    public void testFromToJSON_minimal() throws Exception 
    {
        // This is because we do not know in which order the unit test run,
        // so, there could be a previous test that defines the fragment space,
        // while here we expect no fragment space to be defined.
        FragmentSpace.clearAll();
        
        EmptyVertex ev = new EmptyVertex();
        assertEquals(VertexType.EmptyVertex,ev.vertexType);
        String evStr = ev.toJson();
        DENOPTIMVertex ev2 = DENOPTIMVertex.fromJson(evStr);
        assertTrue(ev2 instanceof EmptyVertex);
        
        DENOPTIMFragment f = new DENOPTIMFragment();
        assertEquals(VertexType.MolecularFragment,f.vertexType);
        String fStr = f.toJson();
        DENOPTIMVertex f2 = DENOPTIMVertex.fromJson(fStr);
        assertTrue(f2 instanceof DENOPTIMFragment);
        
        DENOPTIMTemplate t = new DENOPTIMTemplate(BBType.FRAGMENT);
        assertEquals(VertexType.Template,t.vertexType);
        String tStr = t.toJson();
        DENOPTIMVertex t2 = DENOPTIMVertex.fromJson(tStr);
        assertTrue(t2 instanceof DENOPTIMTemplate);
    }
	
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_Equal()
    {
        EmptyVertex vA = new EmptyVertex(0);
        EmptyVertex vB = new EmptyVertex(90);
        vA.addAP(0);
        vA.addAP(1);
        vA.addAP(2);
        vA.addAP(3);

        vB.addAP(0);
        vB.addAP(1);
        vB.addAP(2);
        vB.addAP(3);
        //NB: vertex ID must be ignores by the sameAs method

    	assertTrue(vA.sameAs(vB, reason));	
    }
    
//------------------------------------------------------------------------------
	
    @Test
    public void testSameAs_DiffAPNum()
    {
        EmptyVertex vA = new EmptyVertex(0);
        vA.addAP(0);
        vA.addAP(1);
        vA.addAP(2);
        vA.addAP(3);

        EmptyVertex vB = new EmptyVertex(90);
        vB.addAP(0);
        vB.addAP(1);
        vB.addAP(2);
        //NB: vertex ID must be ignores by the sameAs method

        assertFalse(vA.sameAs(vB, reason));
    }
    
//------------------------------------------------------------------------------
    
    @Test
    public void testClone() throws Exception
    {
        // This is just to avoid the warnings about trying to get a bond type
        // when the fragment space in not defined
        String APRULE = "apc";
        HashMap<String, BondType> map = new HashMap<>();
        map.put(APRULE,BondType.DOUBLE);
        FragmentSpace.setBondOrderMap(map);

        EmptyVertex v = new EmptyVertex(0);
        v.addAP(1);
        v.addAP(2);
        v.addAP(3);
        
        DENOPTIMVertex c = v.clone();
        
        assertEquals(v.getVertexId(), c.getVertexId(), "Vertex ID");
        assertEquals(v.getNumberOfAPs(), c.getNumberOfAPs(), "Number of APS");
        assertEquals(v.getSymmetricAPSets().size(), 
                c.getSymmetricAPSets().size(), "Number of SymAPs sets");
        assertEquals(v.isRCV(), c.isRCV(), "RCV flag");
        assertNotEquals(v.hashCode(), c.hashCode(), "Hash code"); 
        
        
        DENOPTIMFragment v2 = new DENOPTIMFragment();
        Atom a1 = new Atom("C", new Point3d(new double[]{0.0, 1.1, 2.2}));
        Atom a2 = new Atom("C", new Point3d(new double[]{1.0, 1.1, 2.2}));
        Atom a3 = new Atom("C", new Point3d(new double[]{2.0, 1.1, 2.2}));
        v2.addAtom(a1);
        v2.addAtom(a2);
        v2.addAtom(a3);
        v2.addBond(new Bond(a1, a2));
        v2.addBond(new Bond(a2, a3));
        String APCLASS = APRULE + DENOPTIMConstants.SEPARATORAPPROPSCL +"0";
        v2.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 2.2, 3.3}));
        v2.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 3.3}));
        v2.addAPOnAtom(a3, APClass.make(APCLASS), new Point3d(
                new double[]{0.0, 0.0, 1.1}));
        v2.addAPOnAtom(a1, APClass.make(APCLASS), new Point3d(
                new double[]{3.0, 0.0, 3.3}));
        
        DENOPTIMVertex c2 = v2.clone();
        
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
        DENOPTIMVertex v = new EmptyVertex(DENOPTIMVertex.BBType.FRAGMENT);
        assertEquals(1,v.getMutationSites().size(),
                "Fragments return themselves as mutable sites.");
        v = new EmptyVertex(DENOPTIMVertex.BBType.SCAFFOLD);
        assertEquals(0,v.getMutationSites().size(),
                "Scaffolds so not return any mutable site.");
        v = new EmptyVertex(DENOPTIMVertex.BBType.CAP);
        assertEquals(0,v.getMutationSites().size(),
                "Capping groups so not return any mutable site.");
        v = new EmptyVertex(DENOPTIMVertex.BBType.UNDEFINED);
        assertEquals(1,v.getMutationSites().size(),
                "Undefined building block return themselves as mutable sites.");
        v = new EmptyVertex(DENOPTIMVertex.BBType.NONE);
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
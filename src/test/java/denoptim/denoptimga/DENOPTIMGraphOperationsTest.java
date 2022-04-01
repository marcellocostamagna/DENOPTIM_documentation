package denoptim.denoptimga;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.vecmath.Point3d;

import org.jgrapht.alg.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openscience.cdk.DefaultChemObjectBuilder;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;

import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.graph.APClass;
import denoptim.graph.DENOPTIMAttachmentPoint;
import denoptim.graph.DENOPTIMFragment;
import denoptim.graph.DENOPTIMGraph;
import denoptim.graph.DENOPTIMRing;
import denoptim.graph.DENOPTIMTemplate;
import denoptim.graph.DENOPTIMTemplate.ContractLevel;
import denoptim.graph.DENOPTIMVertex;
import denoptim.graph.DENOPTIMVertex.BBType;
import denoptim.graph.EmptyVertex;
import denoptim.graph.GraphPattern;
import denoptim.io.DenoptimIO;
import denoptim.graph.DENOPTIMEdge.BondType;
import denoptim.utils.GraphUtils;

/**
 * Unit test
 * 
 * @author Marco Foscato
 */

public class DENOPTIMGraphOperationsTest {

    private static APClass APCA, APCB, APCC, APCD;
    
    IChemObjectBuilder chemBuilder = DefaultChemObjectBuilder.getInstance();
    private final Random rng = new Random();
    private static APClass DEFAULT_APCLASS;

//------------------------------------------------------------------------------

    @BeforeAll
    static void setUpClass() {
        try {
            DEFAULT_APCLASS = APClass.make("norule:0");
        } catch (DENOPTIMException e) {
            e.printStackTrace();
        }
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_singleRingSystem() throws Throwable
    {
        DENOPTIMGraph g = getThreeCycle();

        List<DENOPTIMGraph> subgraphs = g.extractPattern(GraphPattern.RING);

        assertEquals(1, subgraphs.size());
        DENOPTIMGraph actual = subgraphs.get(0);
        DENOPTIMGraph expected = g;

        assertEquals(expected.getVertexCount(), actual.getVertexCount());
        assertEquals(expected.getEdgeCount(), actual.getEdgeCount());
        assertEquals(1, actual.getRingCount());

        assertTrue(DENOPTIMGraph.compareGraphNodes(expected.getSourceVertex(),
                expected, actual.getSourceVertex(), actual));
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_returnsEmptyListIfNoRings() 
            throws Throwable
    {
        DENOPTIMGraph g = getThreeCycle();
        g.removeRing(g.getRings().get(0));
        List<DENOPTIMGraph> subgraphs = g.extractPattern(GraphPattern.RING);

        assertEquals(0, subgraphs.size());
    }

//------------------------------------------------------------------------------

    @Test
    public void testExtractPattern_fusedRings() throws Throwable 
    {
        ExtractPatternCase testCase = getFusedRings();

        List<DENOPTIMGraph> subgraphs = testCase.g.extractPattern(GraphPattern.RING);

        assertTrue(testCase.matchesExpected(subgraphs));
    }

//------------------------------------------------------------------------------

    /**
     * Returns a molecule consisting of two pairs of fused rings connected
     * by an oxygen atom. The dots represents chords. The molecule looks as
     * follows:
     *     Cl
     *     |   ↑
     * O - C - C →       ← N - |
     * .   |   .       ↑   .   |
     * .   N - C - O - C - C - C →
     * .   |   .       |   .   ↓
     * . . N . .       O . .
     *
     * The atoms are labelled in order of the leftmost then topmost.
     *     2
     *     |   ↑
     * 0 - 1 - 4 →        ← 12 - |
     * .   |   .       ↑    .    |
     * .   3 - 6 - 7 - 8 - 10 - 11 →
     * .   |   .       |   .     ↓
     * . . 5 . .       9 . .
     */
    private ExtractPatternCase getFusedRings() throws Throwable
    {
        BiFunction<String, Boolean, DENOPTIMVertex> vertexSupplier =
                (s, isRCV) -> {
            int apCount = 0;
            switch (s) {
                case "Cl":
                    apCount = 1;
                    break;
                case "O":
                    apCount = 2;
                    break;
                case "N":
                    apCount = 3;
                    break;
                case "C":
                    apCount = 4;
                    break;
            }
            return buildFragment(s, apCount, isRCV);
        };

        /* We label the vertices in order of top left to bottom right. */
        List<DENOPTIMVertex> vertices = Stream.of(
                new Pair<>("O", true), new Pair<>("C", false),
                new Pair<>("Cl", false), new Pair<>("N",false),
                new Pair<>("C", true), new Pair<>("N", true),
                new Pair<>("C", true), new Pair<>("O", false),
                new Pair<>("C", false), new Pair<>("O", true),
                new Pair<>("C", true), new Pair<>("C", false),
                new Pair<>("N", true)
        ).map(p -> vertexSupplier.apply(p.getFirst(), p.getSecond()))
                .collect(Collectors.toList());

        /* Here we specify the connections between atoms. Previously
        connected vertices are not connected twice. Chords are not connected. */
        List<List<Integer>> edges = Arrays.asList(
                Arrays.asList(1),
                Arrays.asList(2, 3, 4),
                Arrays.asList(),
                Arrays.asList(5, 6),
                Arrays.asList(),
                Arrays.asList(),
                Arrays.asList(7),
                Arrays.asList(8),
                Arrays.asList(9, 10),
                Arrays.asList(),
                Arrays.asList(11),
                Arrays.asList(12)
        );

        DENOPTIMGraph g = null;
        try
        {
            g = buildGraph(vertices, edges);
        } catch (DENOPTIMException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        g.renumberGraphVertices();
        DENOPTIMGraph.setScaffold(vertices.get(0));
        addRings(vertices, g);
        Set<DENOPTIMGraph> expectedSubgraphs = getExpectedSubgraphs(g);
        return new ExtractPatternCase(g, 2, expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private DENOPTIMGraph buildGraph(List<DENOPTIMVertex> vertices,
                                     List<List<Integer>> edges) 
                                             throws DENOPTIMException {
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(vertices.get(0));
        for (int i = 0; i < edges.size(); i++) {
            DENOPTIMVertex srcVertex = vertices.get(i);
            for (Integer adj : edges.get(i)) {
                DENOPTIMVertex trgVertex = vertices.get(adj);

                DENOPTIMAttachmentPoint srcAP = srcVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() == null)
                        .findFirst()
                        .get();
                DENOPTIMAttachmentPoint trgAP = trgVertex
                        .getAttachmentPoints()
                        .stream()
                        .filter(ap -> ap.getEdgeUser() == null)
                        .findFirst()
                        .get();

                try {
                    g.appendVertexOnAP(srcAP, trgAP);
                } catch (DENOPTIMException e) {
                    // Should not happen
                    e.printStackTrace();
                }
            }
        }
        return g;
    }

//------------------------------------------------------------------------------

    private void addRings(List<DENOPTIMVertex> vertices, DENOPTIMGraph g) {
        List<List<DENOPTIMVertex>> ringVertices = Stream.of(
                Arrays.asList(0, 1, 3, 5),
                Arrays.asList(4, 1, 3, 6),
                Arrays.asList(6, 3, 5),
                Arrays.asList(9, 8, 10),
                Arrays.asList(10, 11, 12))
                .map(indices -> indices
                        .stream()
                        .map(vertices::get)
                        .collect(Collectors.toList())
                )
                .collect(Collectors.toList());

        for (List<DENOPTIMVertex> vs : ringVertices) {
            DENOPTIMRing r = new DENOPTIMRing();
            for (DENOPTIMVertex v : vs) {
                r.addVertex(v);
            }
            g.addRing(r);
        }
    }

//------------------------------------------------------------------------------

    private Set<DENOPTIMGraph> getExpectedSubgraphs(DENOPTIMGraph graph) {
        List<Set<Integer>> keepVertices = Stream.of(
                Stream.of(0, 1, 3, 4, 5, 6),
                Stream.of(8, 9, 10, 11, 12))
                .map(indices -> indices.collect(Collectors.toSet()))
                .collect(Collectors.toList());

        List<DENOPTIMGraph> expectedSubgraphs = new ArrayList<>(2);
        for (Set<Integer> keepVertex : keepVertices) {
            DENOPTIMGraph expSubgraph = graph.clone();
            List<DENOPTIMVertex> vertices = expSubgraph.getVertexList();
            Set<DENOPTIMVertex> removeVertices = IntStream
                    .range(0, vertices.size())
                    .filter(i -> !keepVertex.contains(i))
                    .mapToObj(vertices::get)
                    .collect(Collectors.toSet());

            for (DENOPTIMVertex removeVertex : removeVertices) {
                expSubgraph.removeVertex(removeVertex);
            }
            expectedSubgraphs.add(expSubgraph);
        }

        return new HashSet<>(expectedSubgraphs);
    }

//------------------------------------------------------------------------------

    private DENOPTIMVertex buildFragment(String elementSymbol, int apCount,
            boolean isRCV)
    {
        try
        {
            IAtomContainer atomContainer = chemBuilder.newAtomContainer();
            IAtom oxygen = chemBuilder.newAtom();
            oxygen.setSymbol(elementSymbol);
            atomContainer.addAtom(oxygen);
    
            DENOPTIMFragment v = new DENOPTIMFragment(
                    GraphUtils.getUniqueVertexIndex(), atomContainer,
                    BBType.FRAGMENT, isRCV);
            for (int i = 0; i < apCount; i++) 
            {
                v.addAP(0, DEFAULT_APCLASS, getRandomVector());
            }
            return v;
        } catch (Throwable t)
        {
            return null;
        }
   }

//------------------------------------------------------------------------------

    private Point3d getRandomVector() 
    {
        double precision = 10*10*10*10;
        return new Point3d(
                (double) (Math.round(rng.nextDouble() * precision)) / precision,
                (double) (Math.round(rng.nextDouble() * precision)) / precision,
                (double) (Math.round(rng.nextDouble() * precision)) / precision
        );
    }

//------------------------------------------------------------------------------

    /**
     * Returns a 3-cycle. The S marks the scaffold vertex:
     *    /--- S ---\
     *   /           \
     * RCV -(chord)- RCV
     */
    private DENOPTIMGraph getThreeCycle() throws DENOPTIMException 
    {
        EmptyVertex v1 = new EmptyVertex(0);
        EmptyVertex rcv1 = new EmptyVertex(1, new ArrayList<>(),
                new ArrayList<>(), true);
        EmptyVertex rcv2 = new EmptyVertex(2, new ArrayList<>(),
                new ArrayList<>(), true);

        List<EmptyVertex> vertices = Arrays.asList(v1, rcv1, rcv2);
        for (EmptyVertex v : vertices) {
            v.setBuildingBlockType(BBType.FRAGMENT);
            v.addAP();
        }
        // Need an additional AP on v1
        v1.addAP();

        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(v1);
        g.appendVertexOnAP(v1.getAP(0), rcv1.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), rcv2.getAP(0));

        DENOPTIMRing r = new DENOPTIMRing(new ArrayList<>(
                Arrays.asList(rcv1, v1, rcv2)));
        g.addRing(r);

        g.renumberGraphVertices();
        return g;
    }

//------------------------------------------------------------------------------

    private static final class ExtractPatternCase 
    {
        final DENOPTIMGraph g;
        final int expectedSize;
        final Set<DENOPTIMGraph> expectedGraphs;
        final Comparator<DENOPTIMGraph> graphComparator = (gA, gB) ->
                gA.sameAs(gB, new StringBuilder()) ? 0 : -1;

        private ExtractPatternCase(DENOPTIMGraph g, int expectedSize,
                                         Set<DENOPTIMGraph> expectedGraphs) {
            this.g = g;
            this.expectedSize = expectedSize;
            this.expectedGraphs = expectedGraphs;
        }

        private boolean matchesExpected(Collection<DENOPTIMGraph> actuals) {
            if (actuals.size() != expectedSize) {
                return false;
            }
                
            Set<DENOPTIMGraph> unmatchedGraphs = new HashSet<>(expectedGraphs);
            for (DENOPTIMGraph g : actuals) 
            {
                boolean hasMatch = expectedGraphs
                        .stream()
                        .anyMatch(exp -> graphComparator.compare(g, exp) == 0);
                if (hasMatch) {
                    unmatchedGraphs = unmatchedGraphs
                            .stream()
                            .filter(exp -> graphComparator.compare(g, exp) != 0)
                            .collect(Collectors.toSet());
                } else {
                    return false;
                }
            }

            // Check that no graphs are missing from actual
            return unmatchedGraphs.size() == 0;
        }
    }
    
//------------------------------------------------------------------------------

    @Test
    public void testLocateCompatibleXOverPoints() throws Exception
    {
        DENOPTIMGraph[] pair = getPairOfTestGraphs();
        DENOPTIMGraph graphA = pair[0];
        DENOPTIMGraph graphB = pair[1];
        
        DENOPTIMTemplate t1 = (DENOPTIMTemplate) graphA.getVertexAtPosition(1);
        DENOPTIMTemplate t2 = (DENOPTIMTemplate) graphB.getVertexAtPosition(1);
        
        // Making some empty vertexes unique to enable swapping (otherwise they
        // are seen as the same node and excluded from xover sites list)
        DENOPTIMVertex v5A = t1.getInnerGraph().getVertexAtPosition(5);
        String k = "Uniquefier";
        v5A.setUniquefyingProperty(k);
        v5A.setProperty(k, "123");
        DENOPTIMVertex v3B = t2.getInnerGraph().getVertexAtPosition(3);
        v3B.setUniquefyingProperty(k);
        v3B.setProperty(k, "789");
        
        List<XoverSite> xoverSites = 
                DENOPTIMGraphOperations.locateCompatibleXOverPoints(graphA, graphB);
        
        assertEquals(17, xoverSites.size());

        // NB: we exploit the fact that every vertex has a unique label as a 
        // property and the combination of sites generates an invariant.
        // To generate this labels programmatically the following code is used
        // but only after having checked manually.
        boolean writeCode = false;
        if (writeCode)
        {
            for (XoverSite x : xoverSites)
            {
                String s = "\"\"";
                for (DENOPTIMVertex v : x.getA())
                {
                    String g = "";
                    if (v.getGraphOwner()==graphA)
                        g = "graphA";
                    else if (v.getGraphOwner()==graphB)
                        g = "graphB";
                    else if (v.getGraphOwner()==t1.getInnerGraph())
                        g = "t1.getInnerGraph()";
                    else if (v.getGraphOwner()==t2.getInnerGraph())
                        g = "t2.getInnerGraph()";
                    else
                        g = "noGraph";
                    
                    s = s + "+GraphUtils.getLabel("+g+","+v.getGraphOwner().indexOf(v)+")+\"_\"";
                }
                s = s + "+\"@@@_\"";
                for (DENOPTIMVertex v : x.getB())
                {
                    String g = "";
                    if (v.getGraphOwner()==graphA)
                        g = "graphA";
                    else if (v.getGraphOwner()==graphB)
                        g = "graphB";
                    else if (v.getGraphOwner()==t1.getInnerGraph())
                        g = "t1.getInnerGraph()";
                    else if (v.getGraphOwner()==t2.getInnerGraph())
                        g = "t2.getInnerGraph()";
                    else
                        g = "noGraph";
                    
                    s = s + "+GraphUtils.getLabel("+g+","+v.getGraphOwner().indexOf(v)+")+\"_\"";
                }
                System.out.println("expected.add("+s+");");
            }
        }
        
        Set<String> expected = new HashSet<String>();
        expected.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,1)+"_"+GraphUtils.getLabel(graphA,4)+"_"+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,2)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,2)+"_"+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,3)+"_"+"@@@_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,1)+"_"+GraphUtils.getLabel(graphB,5)+"_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(graphA,5)+"_"+"@@@_"+GraphUtils.getLabel(graphB,2)+"_"+GraphUtils.getLabel(graphB,3)+"_"+GraphUtils.getLabel(graphB,4)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),1)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),4)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),5)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),1)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),1)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),2)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),2)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),3)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),4)+"_"+GraphUtils.getLabel(t1.getInnerGraph(),5)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        expected.add(""+GraphUtils.getLabel(t1.getInnerGraph(),5)+"_"+"@@@_"+GraphUtils.getLabel(t2.getInnerGraph(),3)+"_");
        
        for (XoverSite site : xoverSites)
        {
            String label = "";
            for (DENOPTIMVertex v : site.getA())
            {
                label = label + GraphUtils.getLabel(v.getGraphOwner(),
                        v.getGraphOwner().indexOf(v)) + "_";
            }
            label = label + "@@@_";
            for (DENOPTIMVertex v : site.getB())
            {
                label = label + GraphUtils.getLabel(v.getGraphOwner(),
                        v.getGraphOwner().indexOf(v)) + "_";
            }
            assertTrue(expected.contains(label), "Missing label: "+label);
        }
    }
    
//------------------------------------------------------------------------------
    
    //TODO-gg @Test
    public void testLocateCompatibleXOverPointsB() throws Exception
    {
        PopulationTest.prepare();

        DENOPTIMGraph[] pair = PopulationTest.getPairOfTestGraphsB();
        DENOPTIMGraph gA = pair[0];
        DENOPTIMGraph gB = pair[1];
        
        List<XoverSite> xoverSites = 
                DENOPTIMGraphOperations.locateCompatibleXOverPoints(gA, gB);
        
        assertEquals(17, xoverSites.size()); //TODO-gg update
        
        assertTrue(false);//TODO-gg update
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Generates a pair of graphs that include templates with free content. 
     * The first graph is
     * <pre>
     *                (A)--(A)-m5
     *               /
     *  m1-(A)--(A)-T1-(A)--(A)-m2-(B)--(B)-m3
     *               \
     *                (C)--(C)-m4
     * </pre>
     * where template 'T1' is:
     * <pre> 
     *     (A)         (C)
     *    /           /
     *  tv0-(A)--(A)-tv1-(B)--(C)-tv2-(A)--(A)-tv3-(A)-
     *                \
     *                 (A)--(A)-tv4-(A)--(A)-tv5-(A)-
     * </pre>
     * 
     * And the second graph is
     * <pre>
     *  f1-(A)--(A)-T1-(A)--(A)-f2-(B)--(B)-f3-(C)--(C)-f4
     *               \
     *                (C)--(C)-f5
     * </pre>
     * where template 'T2' is:
     * <pre> 
     *         (C)
     *        /
     *  -(A)-tw1-(B)--(C)-tw2-(B)--(B)-tw3-(A)-(A)-tw4-(A)
     * </pre>
     */
    private DENOPTIMGraph[] getPairOfTestGraphs() throws Exception
    {
        prepareAPClassCompatibility();
        
        // Prepare special building block: template T1
        EmptyVertex v0 = new EmptyVertex(0);
        v0.addAP(APCA);
        v0.addAP(APCA);
        v0.setProperty("Label", "tv0");
        
        EmptyVertex v1 = new EmptyVertex(1);
        v1.addAP(APCA);
        v1.addAP(APCA);
        v1.addAP(APCB);
        v1.addAP(APCC);
        v1.setProperty("Label", "tv1");
        
        EmptyVertex v2 = new EmptyVertex(2);
        v2.addAP(APCA);
        v2.addAP(APCC);
        v2.setProperty("Label", "tv2");
        
        EmptyVertex v3 = new EmptyVertex(3);
        v3.addAP(APCA);
        v3.addAP(APCA);
        v3.setProperty("Label", "tv3");

        EmptyVertex v4 = new EmptyVertex(4);
        v4.addAP(APCA);
        v4.addAP(APCA);
        v4.setProperty("Label", "tv4");
        
        EmptyVertex v5 = new EmptyVertex(5);
        v5.addAP(APCA);
        v5.addAP(APCA);
        v5.setProperty("Label", "tv5");
        
        DENOPTIMGraph g = new DENOPTIMGraph();
        g.addVertex(v0);
        g.setGraphId(-1);
        g.appendVertexOnAP(v0.getAP(0), v1.getAP(0));
        g.appendVertexOnAP(v1.getAP(2), v2.getAP(1));
        g.appendVertexOnAP(v2.getAP(0), v3.getAP(0));
        g.appendVertexOnAP(v1.getAP(1), v4.getAP(1));
        g.appendVertexOnAP(v4.getAP(0), v5.getAP(1));
        
        DENOPTIMTemplate t1 = new DENOPTIMTemplate(BBType.NONE);
        t1.setInnerGraph(g);
        t1.setProperty("Label", "t1");
        t1.setContractLevel(ContractLevel.FREE);
        
        // Assemble the first graph: graphA
        
        EmptyVertex m1 = new EmptyVertex(101);
        m1.addAP(APCA);
        m1.setProperty("Label", "m101");
        
        EmptyVertex m2 = new EmptyVertex(102);
        m2.addAP(APCA);
        m2.addAP(APCB);
        m2.setProperty("Label", "m102");
        
        EmptyVertex m3 = new EmptyVertex(103);
        m3.addAP(APCB);
        m3.setProperty("Label", "m103");

        EmptyVertex m4 = new EmptyVertex(104);
        m4.addAP(APCC);
        m4.setProperty("Label", "m104");

        EmptyVertex m5 = new EmptyVertex(105);
        m5.addAP(APCA);
        m5.setProperty("Label", "m105");
        
        DENOPTIMGraph graphA = new DENOPTIMGraph();
        graphA.addVertex(m1);
        graphA.appendVertexOnAP(m1.getAP(0), t1.getAP(0));
        graphA.appendVertexOnAP(t1.getAP(2), m2.getAP(0));
        graphA.appendVertexOnAP(m2.getAP(1), m3.getAP(0));
        graphA.appendVertexOnAP(t1.getAP(1), m4.getAP(0));
        graphA.appendVertexOnAP(t1.getAP(3), m5.getAP(0));

        graphA.setGraphId(11111);
        
        //Prepare special building block: template T2
        EmptyVertex w1 = new EmptyVertex(11);
        w1.addAP(APCA);
        w1.addAP(APCB);
        w1.addAP(APCC);
        w1.setProperty("Label", "tw11");
        
        EmptyVertex w2 = new EmptyVertex(12);
        w2.addAP(APCB);
        w2.addAP(APCC);
        w2.setProperty("Label", "tw12");
        
        EmptyVertex w3 = new EmptyVertex(13);
        w3.addAP(APCA);
        w3.addAP(APCB);
        w3.setProperty("Label", "tw13");
        
        EmptyVertex w4 = new EmptyVertex(14);
        w4.addAP(APCA);
        w4.addAP(APCA);
        w4.setProperty("Label", "tw14");
        
        DENOPTIMGraph g2 = new DENOPTIMGraph();
        g2.addVertex(w1);
        g2.appendVertexOnAP(w1.getAP(1), w2.getAP(1));
        g2.appendVertexOnAP(w2.getAP(0), w3.getAP(1));
        g2.appendVertexOnAP(w3.getAP(0), w4.getAP(0));
        g2.setGraphId(-2);
        
        DENOPTIMTemplate t2 = new DENOPTIMTemplate(BBType.NONE);
        t2.setInnerGraph(g2);
        t2.setProperty("Label", "t2");
        t2.setContractLevel(ContractLevel.FREE);
        
        // Assemble the second graph: graphB
        
        EmptyVertex f1 = new EmptyVertex(1001);
        f1.addAP(APCA);
        f1.setProperty("Label", "f1001");
        
        EmptyVertex f2 = new EmptyVertex(1002);
        f2.addAP(APCA);
        f2.addAP(APCB);
        f2.setProperty("Label", "f1002");
        
        EmptyVertex f3 = new EmptyVertex(1003);
        f3.addAP(APCB);
        f3.addAP(APCC);
        f3.setProperty("Label", "f1003");

        EmptyVertex f4 = new EmptyVertex(1004);
        f4.addAP(APCC);
        f4.setProperty("Label", "f1004");

        EmptyVertex f5 = new EmptyVertex(1005);
        f5.addAP(APCC);
        f5.setProperty("Label", "f1005");
        
        DENOPTIMGraph graphB = new DENOPTIMGraph();
        graphB.addVertex(f1);
        graphB.appendVertexOnAP(f1.getAP(0), t2.getAP(0));
        graphB.appendVertexOnAP(t2.getAP(2), f2.getAP(0));
        graphB.appendVertexOnAP(f2.getAP(1), f3.getAP(0));
        graphB.appendVertexOnAP(f3.getAP(1), f4.getAP(0));
        graphB.appendVertexOnAP(t2.getAP(1), f5.getAP(0));
        graphB.setGraphId(22222);
        
        DENOPTIMGraph[] pair = new DENOPTIMGraph[2];
        pair[0] = graphA;
        pair[1] = graphB;
        
        return pair;
    }
    
//------------------------------------------------------------------------------

    /**
     * Sets the compatibility matrix (src -> trg);
     * 
     * <pre>
     *      |  A  |  B  |  C  |  D  |
     *    ---------------------------
     *    A |  T  |     |     |     |
     *    ---------------------------
     *    B |     |  T  |  T  |     |
     *    ---------------------------
     *    C |     |     |  T   |     |
     *    ---------------------------
     *    D |     |     |     |  T  |
     * </pre>
     */
    private void prepareAPClassCompatibility() throws Exception
    {
        // Prepare APClass compatibility rules
        APCA = APClass.make("A", 0);
        APCB = APClass.make("B", 0);
        APCC = APClass.make("C", 0);
        APCD = APClass.make("D", 0);
        
        HashMap<APClass,ArrayList<APClass>> cpMap = 
                new HashMap<APClass,ArrayList<APClass>>();
        ArrayList<APClass> lstA = new ArrayList<APClass>();
        lstA.add(APCA);
        cpMap.put(APCA, lstA);
        ArrayList<APClass> lstB = new ArrayList<APClass>();
        lstB.add(APCB);
        lstB.add(APCC);
        cpMap.put(APCB, lstB);
        ArrayList<APClass> lstC = new ArrayList<APClass>();
        lstC.add(APCC);
        cpMap.put(APCC, lstC);
        ArrayList<APClass> lstD = new ArrayList<APClass>();
        lstD.add(APCD);
        cpMap.put(APCD, lstD);
        
        HashMap<APClass,APClass> capMap = new HashMap<APClass,APClass>();
        HashSet<APClass> forbEnds = new HashSet<APClass>();
        
        FragmentSpace.setCompatibilityMatrix(cpMap);
        FragmentSpace.setCappingMap(capMap);
        FragmentSpace.setForbiddenEndList(forbEnds);
        FragmentSpace.setAPclassBasedApproach(true);
        
        FragmentSpace.setScaffoldLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setFragmentLibrary(new ArrayList<DENOPTIMVertex>());
        FragmentSpace.setCappingLibrary(new ArrayList<DENOPTIMVertex>());
    }

//------------------------------------------------------------------------------
}
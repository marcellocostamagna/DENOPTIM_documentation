/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no>
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

import java.lang.reflect.Type;

import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.interfaces.IBond.Order;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import denoptim.utils.GeneralUtils;

/**
 * This class represents the edge between two vertices.
 */

public class Edge
{
    /**
     * Attachment point at source end
     */
    private AttachmentPoint srcAP;

    /**
     * Attachment point at target end
     */
    private AttachmentPoint trgAP;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = BondType.UNDEFINED;


//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge that connects two APs. The number of 
     * connections available in the APs is reduced upon creation of the edge 
     * and according the the bond type.
     * @param srcAP attachment point at source end
     * @param trgAP attachment point at target end
     * @param bondType defines what kind of bond type this edge should be 
     * converted to when converting a graph into a chemical representation.
     */
    
    public Edge(AttachmentPoint srcAP,
                          AttachmentPoint trgAP, BondType bondType) {
        this.srcAP = srcAP;
        this.trgAP = trgAP;
        this.bondType = bondType;
        this.srcAP.setUser(this);
        this.trgAP.setUser(this);
    }
      
//------------------------------------------------------------------------------
      
    /**
     * Constructor for an edge that connects two APs. We assume a single bond.
     * The number of 
     * connections available in the APs is reduced upon creation of the edge 
     * and according the the bond type.
     * @param srcAP attachment point at source end
     * @param trgAP attachment point at target end
     */
    
    public Edge(AttachmentPoint srcAP,
                          AttachmentPoint trgAP) {
        this(srcAP, trgAP, BondType.UNDEFINED);
    }
     
//------------------------------------------------------------------------------

    public AttachmentPoint getSrcAP()
    {
        return srcAP;
    }
    
//------------------------------------------------------------------------------

    public AttachmentPoint getSrcAPThroughout()
    {
        return srcAP.getEmbeddedAP();
    }
    
//------------------------------------------------------------------------------

    public AttachmentPoint getTrgAPThroughout()
    {
        return trgAP.getEmbeddedAP();
    }
    
//------------------------------------------------------------------------------

    public AttachmentPoint getTrgAP()
    {
        return trgAP;
    }
    
//------------------------------------------------------------------------------

    public int getSrcVertex()
    {
        return srcAP.getOwner().getVertexId();
    }
    
//------------------------------------------------------------------------------

    public int getSrcAPID()
    {
        return srcAP.getOwner().getIndexOfAP(srcAP);
    }
    
//------------------------------------------------------------------------------

    public int getTrgAPID()
    {
        return trgAP.getOwner().getIndexOfAP(trgAP);
    }        

//------------------------------------------------------------------------------

    public int getTrgVertex()
    {
        return trgAP.getOwner().getVertexId();
    }

//------------------------------------------------------------------------------
    
    public APClass getSrcAPClass()
    {
        return srcAP.getAPClass();
    }
    
//------------------------------------------------------------------------------
    
    public APClass getTrgAPClass()
    {
        return trgAP.getAPClass();
    }

//------------------------------------------------------------------------------

    public BondType getBondType()
    {
        return bondType;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another edge ignoring edge and vertex IDs
     * @param other edge to compare against
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two edges represent the same connection
     * even if the vertex IDs are different.
     */
    
    public boolean sameAs(Edge other, StringBuilder reason)
    {
        if (!this.getSrcAP().sameAs(other.getSrcAP(), reason))
        {
    		reason.append("Different source AP.");
    		return false;
    	}
        if (!this.getTrgAP().sameAs(other.getTrgAP(), reason))
        {
            reason.append("Different target AP.");
            return false;
        }
        if (this.getBondType() != (other.getBondType()))
    	{
    		reason.append("Different bond type ("+this.getBondType()+":"
					+other.getBondType()+");");
    		return false;
    	}
    	return true;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another edge ignoring the directionality of both, i.e.,
     * as if both edges were undirected. Ranking and comparison is based on an
     * invariant lexicographic string that combines, for each side of the edge, 
     * the following information:
     * <ol>
     * <li>type of the building block reached,</li>
     * <li>the ID of the building block,</li>
     * <li>the index of the attachment point.</li>
     * </ol>
     * Only for edges that link equivalent building blocks via the corresponding
     * APs (i.e., edges belonging to the same invariant class), the bond type
     * is considered as the final comparison criterion.
     * 
     * @param other edge to compare with this.
     * @return a negative integer, zero, or a positive integer as this object is
     * less than, equal to, or greater than the specified object.
     */
    public int compareAsUndirected(Edge other)
    {
        Vertex tvA = srcAP.getOwner();
        Vertex tvB = trgAP.getOwner();
        Vertex ovA = other.srcAP.getOwner();
        Vertex ovB = other.trgAP.getOwner();
        
        String invariantTA = tvA.getBuildingBlockType().toOldInt() +
                GeneralUtils.getPaddedString(6,tvA.getBuildingBlockId()) +
                GeneralUtils.getPaddedString(4,srcAP.getIndexInOwner());
        
        String invariantTB = tvB.getBuildingBlockType().toOldInt() +
                GeneralUtils.getPaddedString(6,tvB.getBuildingBlockId()) +
                GeneralUtils.getPaddedString(4,trgAP.getIndexInOwner());
        
        String invariantOA = ovA.getBuildingBlockType().toOldInt() +
                GeneralUtils.getPaddedString(6,ovA.getBuildingBlockId()) +
                GeneralUtils.getPaddedString(4,other.srcAP.getIndexInOwner());
        
        String invariantOB = ovB.getBuildingBlockType().toOldInt() +
                GeneralUtils.getPaddedString(6,ovB.getBuildingBlockId()) +
                GeneralUtils.getPaddedString(4,other.trgAP.getIndexInOwner());
                
        String invariantThis = invariantTA + invariantTB;
        if (invariantTA.compareTo(invariantTB) > 0)
            invariantThis = invariantTB + invariantTA;
        
        String invariantOther = invariantOA + invariantOB;
        if (invariantOA.compareTo(invariantOB) > 0)
            invariantOther = invariantOB + invariantOA;
        
        int resultIgnoringBondType = invariantThis.compareTo(invariantOther);
        
        if (resultIgnoringBondType == 0)
        {
            return this.getBondType().compareTo(other.getBondType());
        } else {
            return resultIgnoringBondType;
        }
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        Vertex srcVertex = srcAP.getOwner();
        int srcAPID = this.getSrcAPID();
        Vertex trgVertex = trgAP.getOwner();
        int trgAPID = this.getTrgAPID();
        
        StringBuilder sb = new StringBuilder(64);
        sb.append(srcVertex.getVertexId()).append("_")
                .append(srcAPID).append("_").
                append(trgVertex.getVertexId()).append("_")
                .append(trgAPID).append("_").
                append(bondType.toString());
        if (srcAP.getAPClass()!=null && trgAP.getAPClass()!=null)
        {
            sb.append("_").append(srcAP.getAPClass()).append("_").append(
                    trgAP.getAPClass());
        }
        
        return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Exchanges source and target vertices and respective APs of this edge.
     */
    public void flipEdge() {
        AttachmentPoint newTrgAP = getSrcAP();
        srcAP = getTrgAP();
        trgAP = newTrgAP;
    }

//------------------------------------------------------------------------------

    /**
     * Possible chemical bond types an edge can represent.
     */
    public enum BondType {

        NONE, UNDEFINED, ANY, SINGLE, DOUBLE, TRIPLE, QUADRUPLE;

        private int valenceUsed = 0;

        private IBond.Order bo = null;

        static {
            ANY.bo = IBond.Order.SINGLE;
            SINGLE.bo = IBond.Order.SINGLE;
            DOUBLE.bo = IBond.Order.DOUBLE;
            TRIPLE.bo = IBond.Order.TRIPLE;
            QUADRUPLE.bo = IBond.Order.QUADRUPLE;

            SINGLE.valenceUsed = 1;
            DOUBLE.valenceUsed = 2;
            TRIPLE.valenceUsed = 3;
            QUADRUPLE.valenceUsed = 4;
        }

        /**
         * Checks if it is possible to convert this edge type into a CDK bond.
         * @return <code>true</code> if this can be converted to a CDK bond.
         */
        public boolean hasCDKAnalogue() {
            return (bo != null);
        }

        /**
         * @return the CDK {@link IBond.Order} represented by this edge type.
         */
        public Order getCDKOrder() {
            return bo;
        }

        /**
         * @param i int to be parsed
         * @return the corresponding bond type, if known, or UNDEFINED.
         */
        public static BondType parseInt(int i)
        {
            switch (i)
            {
                case 0:
                    return NONE;
                case 1:
                    return SINGLE;
                case 2:
                    return DOUBLE;
                case 3:
                    return TRIPLE;
                case 4:
                    return QUADRUPLE;
                case 8:
                    return ANY;
                default:
                    return UNDEFINED;
            }
        }

        /**
         * @param string to be parsed.
         * @return the corresponding bond type, if the string corresponds to a
         * known value, or UNDEFINED.
         */
        public static BondType parseStr(String string)
        {
            for (BondType bt : BondType.values())
            {
                if (bt.name().equals(string.trim().toUpperCase()))
                    return bt;
            }
            switch (string.trim())
            {
                case "1":
                    return SINGLE;
                case "2":
                    return DOUBLE;
                case "3":
                    return TRIPLE;
                case "4":
                    return QUADRUPLE;
                case "8":
                    return ANY;
                default:
                    return UNDEFINED;
            }
        }

        /**
         * @return the number of valences occupied by the bond analogue
         */
        public int getValence()
        {
            return valenceUsed;
        }
    }
    
//------------------------------------------------------------------------------

    public static class DENOPTIMEdgeSerializer 
    implements JsonSerializer<Edge>
    {
        @Override
        public JsonElement serialize(Edge edge, Type typeOfSrc,
              JsonSerializationContext context)
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("srcAPID", edge.getSrcAP().getID());
            jsonObject.addProperty("trgAPID", edge.getTrgAP().getID());
            jsonObject.add("bondType", context.serialize(edge.getBondType()));
            return jsonObject;
        }
    }

//------------------------------------------------------------------------------

}

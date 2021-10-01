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

package denoptim.molecule;

import denoptim.molecule.DENOPTIMEdge.BondType;

/**
 * A query for edges: a list of properties that target edges should possess in 
 * order to match this query.
 */
public class EdgeQuery
{

    /**
     * The vertex id of the source fragment
     */
    private int srcVertex = -1; 
    
    /**
     * the vertex id of the destination fragment
     */
    private int trgVertex = -1; 
    
    /**
     * the index of the attachment point in the list of DAPs associated
     * with the source fragment
     */
    private int srcDAP = -1;
    
    /**
     * the index of the attachment point in the list of DAPs associated
     * with the target fragment
     */
    private int trgDAP = -1;

    /**
     * The bond type associated with the connection between the fragments
     */
    private BondType bondType = BondType.UNDEFINED;
    
    /**
     * The class associated with the source AP
     */
    private APClass srcAPC = null;
    
    /**
     * The class associated with the target AP
     */
    private APClass trgAPC = null;

//------------------------------------------------------------------------------
    
    /**
     * Constructor for a query that contains only wildcards.
     */
    public EdgeQuery()
    {}
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor for an edge from all parameters
     * @param m_src verted ID of the source vertex
     * @param m_trg vertex ID of the target vertex
     * @param m_srcDAP index of the AP on the source vertex
     * @param m_trgDAP index of the AP on the target vertex
     * @param m_btype the bond type
     */
    public EdgeQuery(int m_src, int m_trg, int m_srcDAP, int m_trgDAP, 
            BondType bt)
    {
        srcVertex = m_src;
        trgVertex = m_trg;
        srcDAP = m_srcDAP;
        trgDAP = m_trgDAP;
        bondType = bt;
    }
    
//------------------------------------------------------------------------------

    public void setSourceVertex(int m_src)
    {
        srcVertex = m_src;
    }
    
//------------------------------------------------------------------------------

    public void setSourceAPIdx(int m_srcDAP)
    {
        srcDAP = m_srcDAP;
    }
    
//------------------------------------------------------------------------------

    public void setTargetAPIdx(int m_trgDAP)
    {
        trgDAP = m_trgDAP;
    }        

//------------------------------------------------------------------------------

    public void setTargetVertex(int m_trg)
    {
        trgVertex = m_trg;
    }

//------------------------------------------------------------------------------

    public void setBondType(BondType bt)
    {
        bondType = bt; 
    }
    
//------------------------------------------------------------------------------

    public int getSourceVertex()
    {
        return srcVertex;
    }
    
//------------------------------------------------------------------------------

    public int getSourceAPIdx()
    {
        return srcDAP;
    }
    
//------------------------------------------------------------------------------

    public int getTargetAPIdx()
    {
        return trgDAP;
    }        

//------------------------------------------------------------------------------

    public int getTargetVertex()
    {
        return trgVertex;
    }
    
//------------------------------------------------------------------------------
    
    public APClass getSourceAPClass()
    {
        return srcAPC;
    }
    
//------------------------------------------------------------------------------
    
    public APClass getTargetAPClass()
    {
        return trgAPC;
    }    
    
//------------------------------------------------------------------------------
    
    public void setSourceAPClass(APClass apc)
    {
        srcAPC = apc;
    }
    
//------------------------------------------------------------------------------
    
    public void setTargetAPClass(APClass apc)
    {
        trgAPC = apc;
    }    
    
//------------------------------------------------------------------------------

    public BondType getBondType()
    {
        return bondType;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compares this and another edge ignoring edge and vertex IDs
     * @param other
     * @param reason string builder used to build the message clarifying the 
     * reason for returning <code>false</code>.
     * @return <code>true</code> if the two edges represent the same connection
     * even if the vertex IDs are different.
     */
    public boolean sameAs(EdgeQuery other, StringBuilder reason)
    {
    	if (this.getSourceAPIdx() != other.getSourceAPIdx())
    	{
    		reason.append("Different source atom ("+this.getSourceAPIdx()+":"
    						+other.getSourceAPIdx()+"); ");
    		return false;
    	}
    	if (this.getTargetAPIdx() != other.getTargetAPIdx())
    	{
    		reason.append("Different target atom ("+this.getTargetAPIdx()+":"
					+other.getTargetAPIdx()+"); ");
    		return false;
    	}
    	if (!this.getSourceAPClass().equals(other.getSourceAPClass()))
    	{
    		reason.append("Different source APClass ("
    				+this.getSourceAPClass()+":"
					+other.getSourceAPClass()+"); ");
    		return false;
    	}
    	if (!this.getTargetAPClass().equals(other.getTargetAPClass()))
    	{
    		reason.append("Different target APClass ("
    				+this.getTargetAPClass()+":"
					+other.getTargetAPClass()+"); ");
    		return false;
    	}
    	if (this.getBondType() != (other.getBondType()))
    	{
    		reason.append("Different bond type ("+this.getBondType()+":"
					+other.getBondType()+"); ");
    		return false;
    	}
    	return true;
    }

//------------------------------------------------------------------------------

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(64);
        sb.append(srcVertex).append("_").append(srcDAP).append("_").
                append(trgVertex).append("_").append(trgDAP).append("_").
                append(bondType);
        if (srcAPC != null && trgAPC != null)
            sb.append("_").append(srcAPC).append("_").append(trgAPC);

        return sb.toString();
    }

//------------------------------------------------------------------------------    
}
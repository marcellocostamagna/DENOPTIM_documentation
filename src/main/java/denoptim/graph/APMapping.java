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

import java.util.LinkedHashMap;
import java.util.List;

import denoptim.exception.DENOPTIMException;

/**
 * Class representing a mapping between attachment points (APs). The relations 
 * have a given order, which is that with which they are put in this map
 * (i.e., this class extends {@link LinkedHashMap}). 
 * The mapping can contain any number of one-to-one relations between APs,
 * and each such relation is sorted: all APs present as keys are assumed to 
 * be related by the same context (e.g., they belong to the same vertex), 
 * and all APs in the value position are related by the same context
 * (e.g., they belong to the same vertex).
 * 
 * @author Marco Foscato
 *
 */

public class APMapping extends LinkedHashMap<AttachmentPoint, AttachmentPoint>
    implements Cloneable
{
    /**
     * Version UID
     */
    private static final long serialVersionUID = 1L;

//------------------------------------------------------------------------------

    /**
     * Creates a mapping that links no pair of APs.
     */
    public APMapping()
    {
        super();
    }
    
//------------------------------------------------------------------------------

    /**
     * Produces an index-based version of this mapping where each index
     * represents the attachment point as its index in the owner vertex
     * (i.e., the integer is the result of 
     * {@link Vertex#getIndexInOwner()}. 
     * For the indexes to work properly, all the 1st/2nd APs in the mapping 
     * pairs, must consistently belong to the same vertex. 
     * @return an index-based analog of this mapping.
     * @throws DENOPTIMException if APs belong to different owners so the
     * int-based mapping cannot be produced.
     */
    public LinkedHashMap<Integer, Integer> toIntMappig() throws DENOPTIMException
    {
        LinkedHashMap<Integer, Integer> apMap = new LinkedHashMap<Integer, Integer>();
     
        if (this.isEmpty())
            return apMap;
        
        Vertex ownerKey = null;
        Vertex ownerVal = null;
        
        for (AttachmentPoint key : this.keySet())
        {
            if (ownerKey != null && key.getOwner() != ownerKey)
            {
                throw new DENOPTIMException("Owner of AP " 
                        + key.getID() + " is not vertex " 
                        + ownerKey.getVertexId() + ". APMapping cannot be "
                        + "converted to int-mapping.");
            }
            AttachmentPoint val = this.get(key);
            if (ownerVal != null && val.getOwner() != ownerVal)
            {
                throw new IllegalStateException("Owner of AP " 
                        + val.getID() + " is not vertex " 
                        + ownerKey.getVertexId() + ". APMapping cannot be "
                        + "converted to int-mapping.");
            }
            apMap.put(key.getIndexInOwner(),val.getIndexInOwner());
            
            ownerKey = key.getOwner();
            ownerVal = val.getOwner();
        }
        return apMap;
    }

//------------------------------------------------------------------------------
    
    /**
     * Check if this mapping contains all the given attachment points (APs)
     * in the 1st positions of the AP pairs.
     * @param keys the list of key APs.
     * @return <code>true</code> if this mapping contains all the APs
     * and they are all in the 1st position of their pair.
     */
    public boolean containsAllKeys(List<AttachmentPoint> keys)
    {
        return this.keySet().containsAll(keys);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Check if this mapping contains all the given attachment points (APs)
     * in the 2nd positions of the AP pairs.
     * @param keys the list of APs.
     * @return <code>true</code> if this mapping contains all the APs
     * and they are all in the 2nd position of their pair.
     */
    public boolean containsAllValues(List<AttachmentPoint> keys)
    {
        return this.values().containsAll(keys);
    }
    
//------------------------------------------------------------------------------

    /**
     * Shallow cloning.
     */
    @Override
    public APMapping clone()
    {
        APMapping c = new APMapping();
        for (AttachmentPoint key : this.keySet())
        {
            c.put(key, this.get(key));
        }
        return c;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Produces a human readable string based on the AP IDs.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        for (AttachmentPoint key : this.keySet())
        {
            sb.append(key.getID()+"-"+this.get(key).getID()+" ");
        }
        return sb.toString();
    }
    
//------------------------------------------------------------------------------

}
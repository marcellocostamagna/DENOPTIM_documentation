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

package denoptim.molecule;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;

/**
 * Each attachment point is annotated by the number (position) of the atom
 * in the molecule, the number of bonds it is associated with, the current
 * number of bonds it is still allowed to form. Where applicable further
 * information in the form of the set of reaction classes is also added.
 * 
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


public class DENOPTIMAttachmentPoint implements Serializable
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = -3680248393705286640L;

	/**
	 * The index of the source atom in the atom list of the fragment (0-based)
	 */
    private int atomPostionNumber;
    
    /**
     * the original number of connections of this atom
     */
    private int atomConnections; 

    /**
     * the current free connections
     */
    private int apConnections; 
    
    /**
     * The cutting rule that generated this AP (the main APClass)
     */
    private String apRule;
    
    /**
     * The direction index of the cutting rule that generated this AP 
     * (the subClass)
     */
    private int apSubClass;
    
    /**
     * The class associated with the AP
     */
    private String apClass;
    
    /**
     * The direction vector representing the bond direction
     */
    private double[] dirVec; 


//------------------------------------------------------------------------------

    /**
     * Constructor for undefined DENOPTIMAttachmentPoint
     */
    public DENOPTIMAttachmentPoint()
    {
        atomPostionNumber = 0;
        atomConnections = 0;
        apConnections = 0;
        apClass = "";
    }

//------------------------------------------------------------------------------

    /**
     * Constructor
     * @param m_AtomPosNum the index of the source atom (0-based)
     * @param m_atomConnections the total number of connections
     * @param m_apConnections the number of free connections
     */
    public DENOPTIMAttachmentPoint(int m_AtomPosNum, int m_atomConnections,
                                                            int m_apConnections)
    {
        atomPostionNumber = m_AtomPosNum;
        atomConnections = m_atomConnections;
        apConnections = m_apConnections;
        apClass = "";
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Constructor
     * @param m_AtomPosNum the index of the source atom (0-based)
     * @param m_atomConnections the total number of connections
     * @param m_apConnections the number of free connections
     * @param m_dirVec the AP direction vector end (the beginning ate the 
     * coords of the source atom). This must array have 3 entries.
     */
    public DENOPTIMAttachmentPoint(int m_AtomPosNum, int m_atomConnections,
                                        int m_apConnections, double[] m_dirVec)
    {
        atomPostionNumber = m_AtomPosNum;
        atomConnections = m_atomConnections;
        apConnections = m_apConnections;
        apClass = "";
        
        dirVec = new double[3];
        System.arraycopy(m_dirVec, 0, dirVec, 0, m_dirVec.length);        
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Construct an attachment point based on the formatted string 
     * representation. The format is the one used in SDF files.
     * @param str the formatted string.
     * @param format is the format of the string. Acceptable values are 
     * <code>SDF</code> for strings coming from SDF file molecular properties,
     * and <code>MAP</code> for strings generated by the <code>toString()</code>
     * method of DENOPTIMAttachmentPoint.
     * @throws DENOPTIMException
     */
    public DENOPTIMAttachmentPoint(String str, String format) 
    		throws DENOPTIMException
    {
    	switch (format)
    	{
    		case "SDF":
    			processSdfString(str);
    			break;
    			
    		case "MAP":
    			processMapString(str);
    			break;
    			
    		default:
    			throw new DENOPTIMException("Unknown format for string "
    					+ "representation of DENOPTIMAttachmentPoint");
    	}
    }
//-----------------------------------------------------------------------------
	
    private void processMapString(String str) throws DENOPTIMException
    {
    	if (str.contains("{") || str.contains("}")
    			|| str.contains("[") || str.contains("]"))
    	{
    		throw new DENOPTIMException("Unable to parse APString '" + str 
    				+ "'");
    	}
    	
    	String[] params = str.split(",");
    	for (int i=0; i<params.length; i++)
    	{
    		String pvPair = params[i];
    		String[] parts = pvPair.split("=");
    		if (parts.length != 2)
    		{
    			throw new DENOPTIMException("Unable to parse APString '" + str 
    					+ "'");
    		}
    		switch (parts[0])
    		{
    			case "atomPostionNumber":
    				this.atomPostionNumber = Integer.parseInt(parts[1]);
    				break;
    				
    			case "atomConnections":
					this.atomConnections = Integer.parseInt(parts[1]);
					break;
				
    			case "apConnections":
    				this.apConnections = Integer.parseInt(parts[1]);
					break;
				
    			case "apRule":
    				this.apRule = parts[1];
					break;
				
    			case "apSubClass":
    				this.apSubClass = Integer.parseInt(parts[1]);
					break;
				
    			case "apClass":
    				this.apClass = parts[1];
					break;
				
    			case "dirVec.x":
    				this.dirVec[0] = Double.parseDouble(parts[1]);
					break;
				
    			case "dirVec.y":
    				this.dirVec[1] = Double.parseDouble(parts[1]);
					break;
					
    			case "dirVec.z":
    				this.dirVec[2] = Double.parseDouble(parts[1]);
					break;
				
				default:
					throw new DENOPTIMException("Unable to parse APString '" 
							+ str + "'");    				
    		}
    	}
    }
    
//-----------------------------------------------------------------------------
    	
    private void processSdfString(String str) throws DENOPTIMException
    {
        try 
        {
            String[] parts = str.split(
            		Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPAAP));
            
            //WARNING here we convert from 1-based to 0-based index
            this.atomPostionNumber = Integer.parseInt(parts[0])-1;

            String[] details = parts[1].split(
            		Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPSCL));
            switch (details.length)
            {
	            case 2:
	            	//APClass:subclass but no direction vector
	            	break;
	            	
	            case 3:
	            	//APClass:subclass:direction_vector
	            	break;
	            	
	            default:
	            	throw new DENOPTIMException("Unable to split APClass, "
	            			+ "subclass, and coordinates");
            }
            
            this.apRule = details[0];
            this.apSubClass = Integer.parseInt(details[1]);
            this.apClass = this.apRule + DENOPTIMConstants.SEPARATORAPPROPSCL 
            		+ Integer.toString(this.apSubClass);
            
            if (details.length == 3)
            {
	            String[] coord = details[2].split(
	            		Pattern.quote(DENOPTIMConstants.SEPARATORAPPROPXYZ)); 
	            
	            if (coord.length == 3)
	            {
	            	this.dirVec = new double[3];
	            	this.dirVec[0] = Double.parseDouble(coord[0]);
	            	this.dirVec[1] = Double.parseDouble(coord[1]);
	            	this.dirVec[2] = Double.parseDouble(coord[2]);
	            }
            }
	    } catch (Throwable t) {
			throw new DENOPTIMException("Cannot construct AP from string '" 
						+ str + "'");
	    }
    }

//------------------------------------------------------------------------------

    /**
     * Return the total number of connections that can be generated by this AP.
     * This practically corresponds to the max valence this AP can occupy.
     * This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @return the max number of connections
     */
    public int getAtmConnections()
    {
    	return atomConnections;
    }    

//------------------------------------------------------------------------------

    /**
     * Return the number of unoccupied connections. This practically corresponds
     * to the number of unsaturated valences that can still be used on this 
     * attachment point. This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @return the current free connections associated with the atom
     */
    public int getAPConnections()
    {
        return apConnections;
    }

//------------------------------------------------------------------------------

    /**
     * The index of the source atom in the atom list of the fragment. 
     * The index is reported considering 0-based enumeration.
     * @return the index of the source atom in the atom list of the fragment
     */
    public int getAtomPositionNumber()
    {
        return atomPostionNumber;
    }

//------------------------------------------------------------------------------

    /**
     * Set the index of the source atom in the list of atoms of the fragment.
     * The index should reflect 0-based enumeration.
     * @param m_atomPostionNumber the index
     */
    public void setAtomPositionNumber(int m_atomPostionNumber)
    {
        atomPostionNumber = m_atomPostionNumber;
    }

//------------------------------------------------------------------------------

    /**
     * Set the number of unoccupied connections. This practically corresponds
     * to the number of unsaturated valences that can still be used on this 
     * attachment point. This is useful for attachment points that do not follow
     * the APClass-based formalism.
     * @param m_apConnections
     */
    public void setAPConnections(int m_apConnections)
    {
        apConnections = m_apConnections;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Sets the end of the 3D vector defining the direction of the AP in 3D.
     * Note that the source of this vector is the source atom, and that the 
     * entries of <code>m_dirVec</code> are referred to the same origin as
     * the coordinates of the source atom.
     * @param m_dirVec the coordinates of the 3D point defining the end of the 
     * direction vector
     */
    public void setDirectionVector(double[] m_dirVec)
    {
        dirVec = new double[3];
        System.arraycopy(m_dirVec, 0, dirVec, 0, m_dirVec.length);  
    }

//------------------------------------------------------------------------------

    /**
     * Set the Attachment Point class. The APClass is the combination of a main
     * class (or "rule") and a subclass (or direction).  
     * @param m_class the new APClass.
     */
    public void setAPClass(String m_class) throws DENOPTIMException
    {
    	if (!m_class.contains(DENOPTIMConstants.SEPARATORAPPROPSCL))
    	{
    		throw new DENOPTIMException("Attempt to use APClass that does not" 
    					+ "contain main:subclass pair");
    	}
        apClass = m_class;
    }

//------------------------------------------------------------------------------

    /**
     * Returns the Attachment Point class.
     * @return the APClass
     */
    public String getAPClass()
    {
        return apClass;
    }
//------------------------------------------------------------------------------

    /**
     * Returns the end of the direction vector. The coordinates are referred to
     * the same reference system of the fragment holding this AP.
     * @return the direction vector.
     */
    public double[] getDirectionVector()
    {
        return dirVec;
    }    

//------------------------------------------------------------------------------

    /**
     * Resets the connections of this AP. Makes all connections free.
     */

    public void resetAPConnections()
    {
        setAPConnections(atomConnections);
    }

//------------------------------------------------------------------------------

    /**
     * Change the number of free connections by the given delta.
     * @param delta the amount of change.
     */
    public void updateAPConnections(int delta)
    {
        apConnections = apConnections + delta;
    }

//------------------------------------------------------------------------------

    /**
     * Check availability of free connections
     * @return <code>true</code> if the attachment point has free connections
     */

    public boolean isAvailable()
    {
        return apConnections > 0;
    }

//------------------------------------------------------------------------------

    /**
     * Checks if this attachment point respects the APClass-based approach.
     * @return <code>true</code> if an APClass is defined.
     */
    public boolean isClassEnabled()
    {
        return apClass.length() > 0;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Compare this and given attachment point. This method defines how
     * DENOPTIMAttachmentPoint are sorted.
     * @param other
     * @return an integer that can be used by a comparator.
     */
    public int compareTo(DENOPTIMAttachmentPoint other)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (this == other)
            return EQUAL;

        //Compare CLASS name
        String cl = this.getAPClass();
        String othercl = other.getAPClass();
        int res = cl.compareTo(othercl);
        if (res < 0)
            return BEFORE;
        else if (res > 0)
            return AFTER;
        
        int tBO = FragmentSpace.getBondOrderForAPClass(this.getAPClass());
        int oBO = FragmentSpace.getBondOrderForAPClass(other.getAPClass());
        if (tBO != oBO)
        {
        	System.out.println("WARNING: Unexpected difference in Bond Order "
        			+ "while CLASS is equal!");
        }
        
        //Compare Direction Vector if AtomID is equal
        if (this.getDirectionVector() != null 
        		&& other.getDirectionVector() != null)
        {
	        double[] thisVec = this.getDirectionVector();
	        double[] otherVec = other.getDirectionVector();
	
	        if (thisVec[0] < otherVec[0])
	            return BEFORE;
	        else if (thisVec[0] > otherVec[0])
	            return AFTER;
	
	        if (thisVec[1] < otherVec[1])
	            return BEFORE;
	        else if (thisVec[1] > otherVec[1])
	            return AFTER;
	        
	        if (thisVec[2] < otherVec[2])
	            return BEFORE;
	        else if (thisVec[2] > otherVec[2])
	            return AFTER;
        }
        else
        {
        	if (this.getDirectionVector() != null)
        	{
        		return AFTER;
        	}
        	else
        	{
        		return BEFORE;
        	}
        }
        
        // Make sure we are consistent with equals method
        assert this.equals(other) : 
        	"DENOPTIMAttachmentPoint.compareTo inconsistent with equals.";
        
        return EQUAL;
    }

//------------------------------------------------------------------------------

    /**
     * Prepare a string for writing this AP in a fragment SDF file.
     * Only DENOPTIM's format is currently supported and we assume three 
     * coordinates are used to define the direction vector.
     * @param isFirst set <code>true</code> is this is the first AP among those
     * on the same source atom. When you give <code>false</code> the atom ID and
     * the first separator are omitted.
     * @returns a string with APClass and coordinated of the AP direction vector
     **/

    public String getSingleAPStringSDF(boolean isFirst)
    {
		StringBuilder sb = new StringBuilder();
		if (isFirst)
		{
			//WARNING! In the mol.property we use 1-to-n+1 instead of 0-to-n
			sb.append(atomPostionNumber+1);
			sb.append(DENOPTIMConstants.SEPARATORAPPROPAAP);
		}
		sb.append(apClass);
		if (dirVec != null)
		{
	        DecimalFormat digits = new DecimalFormat("###.####");
	        digits.setMinimumFractionDigits(4);
			sb.append(DENOPTIMConstants.SEPARATORAPPROPSCL);
			sb.append(digits.format(dirVec[0]));
			sb.append(DENOPTIMConstants.SEPARATORAPPROPXYZ);
			sb.append(digits.format(dirVec[1]));
			sb.append(DENOPTIMConstants.SEPARATORAPPROPXYZ);
			sb.append(digits.format(dirVec[2]));
		}
		return sb.toString();
    }

//------------------------------------------------------------------------------

    /**
     * Produces a string with the information included in this object.
     * @return the string
     */
    
    @Override
    public String toString()
    {
    	Map<String,Object> pars = new HashMap<String,Object>();
    	pars.put("atomPostionNumber",atomPostionNumber);
    	pars.put("atomConnections",atomConnections);
    	pars.put("apConnections",apConnections);
    	if (apClass != null)
    	{
        	pars.put("apRule",apRule);
        	pars.put("apSubClass",apSubClass);
        	pars.put("apClass",apClass);
    	}
    	if (dirVec != null)
    	{
	    	pars.put("dirVec.x",dirVec[0]);
	    	pars.put("dirVec.y",dirVec[1]);
	    	pars.put("dirVec.z",dirVec[2]);
    	}

		return pars.toString();
    }
    
//------------------------------------------------------------------------------
}

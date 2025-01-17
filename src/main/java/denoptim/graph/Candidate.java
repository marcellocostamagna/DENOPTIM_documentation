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

import java.io.File;

import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.io.DenoptimIO;
import denoptim.utils.MoleculeUtils;


/**
 * A candidate is the combination of a denoptim graph with molecular 
 * representation and may include also fitness/error, and possibly other stuff.
 */

public class Candidate implements Comparable<Candidate>, Cloneable
{
    /**
     * Unique identifier of this candidate
     */
    private String uid;
    
	/**
     * Graph representation
     */
    private DGraph graph;
    
    /**
     * Chemical object representation
     */
    private IAtomContainer iac;
    
    /**
     * SMILES representation
     */
    private String smiles;
    
    /**
     * fitness value
     */
    private double fitness;
    
    /**
     * Pathname to SDF file
     */
    private String sdfFile;

    /**
     * Pathname to an image (i.e., PNG file)
     */
    private String imgFile;
    
    /**
     * Any comments
     */
    private String comment;
    
    /**
     * Error that prevented calculation of the fitness
     */
    private String error;
    
    /**
     * Flag signaling the presence of a fitness value associated
     */
    private boolean hasFitness;
    
    /**
     * ID of the generation at which this candidate was generated. This may not
     * be the generation at which this candidate entered the population!
     */
    private int generationId = -1;
    
    /**
     * Name of this candidate (not guaranteed to be unique)
     */
    private String name;
    
    /**
     * Level that generated this graph in fragment space exploration
     */
    private int level;

    
//------------------------------------------------------------------------------

    public Candidate()
    {
        name = "noname";
        uid = "UNDEFINED";
        smiles = "UNDEFINED";
        hasFitness = false;
    }

//------------------------------------------------------------------------------
    
    public Candidate(DGraph graph)
    {
        this();
        this.graph = graph;
        graph.setCandidateOwner(this);
        uid = "UNDEFINED";
        smiles = "UNDEFINED";
        hasFitness = false;
   }
    
//------------------------------------------------------------------------------
    
    public Candidate(String name, DGraph graph)
    {
        this();
        this.name = name;
        this.graph = graph;
        graph.setCandidateOwner(this);
        uid = "UNDEFINED";
        smiles = "UNDEFINED";
        hasFitness = false;
   }
    
//------------------------------------------------------------------------------
    
    public Candidate(String name, DGraph graph, double fitness,
            String uid, String smiles)
    {
        this();
        this.name = name;
        this.graph = graph;
        graph.setCandidateOwner(this);
        this.uid = uid;
        this.smiles = smiles;
        this.fitness = fitness;
        hasFitness = true;
   }

//------------------------------------------------------------------------------

    private Candidate(String name, DGraph graph,
            String uid, String smiles, String molFile, String imgFile,
            String comment, int generationId, int level)
    {
        this();
        this.name = name;
        this.graph = graph;
        graph.setCandidateOwner(this);
        this.uid = uid;
        this.smiles = smiles;
        this.sdfFile = molFile;
        this.imgFile = imgFile;
        this.comment = comment;
        this.generationId = generationId;
        this.level = level; 
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Builds a candidate from the SDF representation of an atom container. This 
     * method does interpret the properties of the atom container object and
     * used them to define the various field values that define this candidate.
     * @param iac the container to read and interpret.
     * @param useFragSpace set <code>true</code> to enable usage of the building 
     * block space when reading in the string-encoder graph representation of 
     * this object. Use <code>false</code> when a building block space is 
     * undefined and the graph representation can only be built assuming that
     * there are as many APs as used in this graph, 
     * while other information on the graph 
     * building blocks cannot be inferred.
     * @throws DENOPTIMException
     */
    public Candidate(IAtomContainer iac, boolean useFragSpace) 
    		throws DENOPTIMException
    {
    	this(iac, useFragSpace, false);
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Builds a candidate from the SDF representation of an atom container. This 
     * method does interpret the properties of the atom container object and
     * used them to define the various field values that define this candidate.
     * @param iac the container to read and interpret.
     * @param useFragSpace set <code>true</code> to enable usage of the building 
     * block space when reading in the string-encoder graph representation of 
     * this object. Use <code>false</code> when a building block space is 
     * undefined and the graph representation can only be built assuming that
     * there are as many APs as used in this graph, 
     * while other information on the graph 
     * building blocks cannot be inferred.
     * @param allowNoUID use <code>true</code> to allow creation on a candidate 
     * that has no unique identifier.
     * @throws DENOPTIMException
     */
    public Candidate(IAtomContainer iac, boolean useFragSpace, 
    		boolean allowNoUID) throws DENOPTIMException
    {
        this.uid = "UNDEFINED";
        this.smiles = "UNDEFINED";
        this.hasFitness = false;
        
        this.iac = MoleculeUtils.makeSameAs(iac);
		
		if (iac.getProperty(CDKConstants.TITLE) != null)
		{
			this.name = iac.getProperty(CDKConstants.TITLE).toString();
		}
		
        if (iac.getProperty(DENOPTIMConstants.MOLERRORTAG) != null)
        {
        	this.error = iac.getProperty(
        			DENOPTIMConstants.MOLERRORTAG).toString();
        }

        if (iac.getProperty(DENOPTIMConstants.FITNESSTAG) != null)
        {
            String fitprp = iac.getProperty(
            		DENOPTIMConstants.FITNESSTAG).toString();
            double fitVal = Double.parseDouble(fitprp);
            if (Double.isNaN(fitVal))
            {
                String msg = "Cannot build Candidate from "
                		+ "IAtomContainer: Fitness value is NaN!";
                throw new DENOPTIMException(msg);
            }
            this.fitness = fitVal;
            this.hasFitness = true;
        }
        
        if (iac.getProperty(DENOPTIMConstants.GRAPHLEVELTAG) != null)
        {
        	this.level = Integer.parseInt(iac.getProperty(
        			DENOPTIMConstants.GRAPHLEVELTAG).toString());
        }
        
        if (iac.getProperty(DENOPTIMConstants.SMILESTAG) != null)
        {
        	this.smiles = iac.getProperty(
        			DENOPTIMConstants.SMILESTAG).toString();
        }

        try
        {
            this.uid = iac.getProperty(
            		DENOPTIMConstants.UNIQUEIDTAG).toString();
        } catch (Exception e) {
        	if (allowNoUID)
        	{
        		this.uid = "noUID";
        	} else {
        		throw new DENOPTIMException("Could not read UID to make "
        				+ "Candidate.", e);
        	}
        }
        
        if (iac.getProperty(DENOPTIMConstants.GMSGTAG) != null)
        {
            this.comment = iac.getProperty(
                    DENOPTIMConstants.GMSGTAG).toString();
        }
        
        this.graph = DenoptimIO.readGraphFromSDFileIAC(iac);
        this.graph.setCandidateOwner(this);
    }

//------------------------------------------------------------------------------
    
    /**
     * Wraps an atom container as a candidate even if a graph is not 
     * available. Essentially, this method produces a candidate that has as much
     * info as available in the atom container, but ignores the lack of a valid 
     * graph definition.
     * @param iac the atom container to be wrapped.
     * @param allowNoUID use <code>true</code> to allow creation on a candidate 
     * that has no unique identifier.
     * @return a new candidate with no graph but with a molecular representation.
     * @throws DENOPTIMException if the UID is required but not found.
     */
    public static Candidate fromAtomContainerNoGraph(IAtomContainer iac, 
            boolean allowNoUID) throws DENOPTIMException
    {
        Candidate cand = new Candidate();
        
        cand.iac = MoleculeUtils.makeSameAs(iac);
        
        if (iac.getProperty(CDKConstants.TITLE) != null)
        {
            cand.name = iac.getProperty(CDKConstants.TITLE).toString();
        }
        
        if (iac.getProperty(DENOPTIMConstants.MOLERRORTAG) != null)
        {
            cand.error = iac.getProperty(
                    DENOPTIMConstants.MOLERRORTAG).toString();
        }

        if (iac.getProperty(DENOPTIMConstants.FITNESSTAG) != null)
        {
            String fitprp = iac.getProperty(
                    DENOPTIMConstants.FITNESSTAG).toString();
            double fitVal = Double.parseDouble(fitprp);
            if (Double.isNaN(fitVal))
            {
                String msg = "Cannot build Candidate from "
                        + "IAtomContainer: Fitness value is NaN!";
                throw new DENOPTIMException(msg);
            }
            cand.fitness = fitVal;
            cand.hasFitness = true;
        }
        
        if (iac.getProperty(DENOPTIMConstants.GRAPHLEVELTAG) != null)
        {
            cand.level = Integer.parseInt(iac.getProperty(
                    DENOPTIMConstants.GRAPHLEVELTAG).toString());
        }
        
        if (iac.getProperty(DENOPTIMConstants.SMILESTAG) != null)
        {
            cand.smiles = iac.getProperty(
                    DENOPTIMConstants.SMILESTAG).toString();
        }

        try {
            cand.uid = iac.getProperty(DENOPTIMConstants.UNIQUEIDTAG).toString();
        } catch (Exception e) {
            if (allowNoUID)
            {
                cand.uid = "noUID";
            } else {
                throw new DENOPTIMException("Could not read UID to make "
                        + "Candidate. No property '" 
                        + DENOPTIMConstants.UNIQUEIDTAG 
                        + "' in the atom container.", e);
            }
        }
        
        if (iac.getProperty(DENOPTIMConstants.GMSGTAG) != null)
        {
            cand.comment = iac.getProperty(
                    DENOPTIMConstants.GMSGTAG).toString();
        }
        
        return cand;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Just place the argument in the IAtomContainer field of this object. We do 
     * not read and interpret the input argument as done 
     * in {@link #Candidate(IAtomContainer)} or
     * {@link #Candidate(IAtomContainer, boolean)}.
     * @param iac new value of IAtomContainer field
     */
    public void setChemicalRepresentation(IAtomContainer iac)
    {
        this.iac = iac;
    }
    
//------------------------------------------------------------------------------

    /**
     * Returns the atom container representing this candidate
     * @return the atom container.
     */
    public IAtomContainer getChemicalRepresentation()
    {
        return iac;
    } 
    
//------------------------------------------------------------------------------

    /**
     * Returns an atom container representing this item in a way that is 
     * compatible for writing this item to an SDF file.
     * @return a decorated atom container.
     * @throws DENOPTIMException when conversion of graph to json fails.
     */
    public IAtomContainer getFitnessProviderOutputRepresentation() throws DENOPTIMException
    {
        IAtomContainer iacForFitFile = iac;
        if (iacForFitFile==null)
        {
            IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
            iacForFitFile = builder.newAtomContainer();
        }
        iacForFitFile.setProperty(CDKConstants.TITLE, name);
        iacForFitFile.setProperty(DENOPTIMConstants.GCODETAG, graph.getGraphId());
        iacForFitFile.setProperty(DENOPTIMConstants.GRAPHTAG, graph.toString());
        iacForFitFile.setProperty(DENOPTIMConstants.GRAPHJSONTAG, graph.toJson());
        if (graph.getLocalMsg() != null && !graph.getLocalMsg().equals(""))
        {
            iacForFitFile.setProperty(DENOPTIMConstants.GMSGTAG, graph.getLocalMsg());
        }
        iacForFitFile.setProperty(DENOPTIMConstants.SMILESTAG, smiles);
        iacForFitFile.setProperty(DENOPTIMConstants.UNIQUEIDTAG, uid);
        if (hasFitness)
        {
            iacForFitFile.setProperty(DENOPTIMConstants.FITNESSTAG, fitness);
        } else {
            iacForFitFile.setProperty(DENOPTIMConstants.MOLERRORTAG, error);
        }
        return iacForFitFile;
    } 
    
//------------------------------------------------------------------------------
    
    public void setComments(String str)
    {
        this.comment = str;
    }
    
//------------------------------------------------------------------------------
    
    public String getComments()
    {
        return comment;
    }    
	
//------------------------------------------------------------------------------

    public void setSDFFile(String molFile)
    {
        this.sdfFile = molFile;
    }	
    
//------------------------------------------------------------------------------

    public void setImageFile(String imgFile)
    {
        this.imgFile = imgFile;
    }

//------------------------------------------------------------------------------

    public void setGraph(DGraph graph)
    {
        this.graph = graph;
    }

//------------------------------------------------------------------------------

    public void setUID(String uid)
    {
        this.uid = uid;
    }

//------------------------------------------------------------------------------

    public void setSmiles(String smiles)
    {
        this.smiles = smiles;
    }
   
//------------------------------------------------------------------------------

    public void setFitness(double fitness)
    {
        this.fitness = fitness;
        this.hasFitness = true;
    }
    
//------------------------------------------------------------------------------

    public void setError(String error)
    {
        this.error = error;
    }
    
//------------------------------------------------------------------------------

    public void setName(String name)
    {
        this.name = name;
    }
    
//------------------------------------------------------------------------------

    public String getName()
    {
        return name;
    }
    
//------------------------------------------------------------------------------

    public String getError()
    {
        return error;
    }
    
//------------------------------------------------------------------------------

    public String getUID()
    {
        return uid;
    }

//------------------------------------------------------------------------------

    public String getSmiles()
    {
        return smiles;
    }
    
//------------------------------------------------------------------------------

    public String getSDFFile()
    {
        return sdfFile;
    }
    
//------------------------------------------------------------------------------

    public String getImageFile()
    {
        return imgFile;
    }

//------------------------------------------------------------------------------

    public DGraph getGraph()
    {
        return graph;
    }

//------------------------------------------------------------------------------

    public double getFitness()
    {
        return fitness;
    }
    
//------------------------------------------------------------------------------
    
    public boolean hasFitness()
    {
    	return hasFitness;
    }

//------------------------------------------------------------------------------
    
    public void setGeneration(int genId)
    {
	    generationId = genId;
	}
    
//------------------------------------------------------------------------------
    
    /**
     * The generation this candidate belong to is that in which it was generated.
     * In asynchronous experiments, the generation at which a candidate is 
     * generated and that when it enters the population may be different.
     * @return the generation at which a candidate is generated.
     */
    public int getGeneration()
    {
	    return generationId;
	}
    
//------------------------------------------------------------------------------
    
    /**
     * Sets level that generated this graph in a fragment space 
     * exploration experiment.
     * @param lev the level index
     */
    public void setLevel(int lev)
    {
    	level = lev;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns the level that generated this graph in a fragment space 
     * exploration experiment.
     * @return the level that generated this graph in a fragment space 
     * exploration experiment.
     */
	public int getLevel() 
	{
		return level;
	}

//------------------------------------------------------------------------------

    /**
     * @returns a negative integer, 0, or a positive integer depending 
     * on whether this object is less than, equal to, or greater than 
     * the other object given as argument.
     */

	//TODO: this comparator is meant for specific uses like sorting the 
	// population according to fitness, and it should thus be a
	// specific comparator, not here determining the natural ordering.
	
    @Override
    public int compareTo(Candidate other)
    {
        if (!hasFitness && !other.hasFitness)
        {
            return 0;
        } else if (!hasFitness && other.hasFitness) {
            return -1;
        } else if (hasFitness && !other.hasFitness) {
            return 1;
        }
        
        if (this.fitness > other.fitness)
            return 1;
        else if (this.fitness < other.fitness)
            return -1;
        return 0;
    }

//------------------------------------------------------------------------------

    /* TODO: change this method! Now it writes a string that is formatted 
     * only for logging needs not really a good toString string.
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(16);
        if (sdfFile != null && name == null)
            name = new File(sdfFile).getName();
        if (name != null)
            sb.append(String.format("%-20s", name));
        
        sb.append(String.format("%-20s", this.graph.getGraphId()));
        sb.append(String.format("%-30s", uid));
        sb.append(String.format("%12.3f", fitness));

        return sb.toString();
    }

//------------------------------------------------------------------------------    
    
    /**
     * Clear the graph describing this candidate
     */
    public void cleanup()
    {
        if (graph != null)
            graph.cleanup();
    }

//------------------------------------------------------------------------------        

    public Candidate clone()
    {
        Candidate c = new Candidate(name, graph.clone(), uid,
                smiles, sdfFile, imgFile, comment, generationId, level);
        
        if (hasFitness)
        {
            c.setFitness(fitness);
        }
        
        if (error!=null)
        {
            c.setError(error);
        }
        
        if (this.iac != null)
        {
            try
            {
                c.setChemicalRepresentation(MoleculeUtils.makeSameAs(
                        this.iac));
            } catch (DENOPTIMException e)
            {
                e.printStackTrace();
                c.setChemicalRepresentation(null);
            }
        }
        return c;
    }

//------------------------------------------------------------------------------        
    
}

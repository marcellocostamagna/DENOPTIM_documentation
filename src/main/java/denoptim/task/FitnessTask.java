/*
 *   DENOPTIM
 *   Copyright (C) 2019 Vishwesh Venkatraman <vishwesh.venkatraman@ntnu.no> and
 *   Marco Foscato <marco.foscato@uib.no>
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

package denoptim.task;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.openscience.cdk.Atom;
import org.openscience.cdk.AtomContainer;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fitness.FitnessProvider;
import denoptim.graph.Candidate;
import denoptim.graph.DENOPTIMGraph;
import denoptim.io.DenoptimIO;
import denoptim.logging.DENOPTIMLogger;
import denoptim.threedim.ThreeDimTreeBuilder;
import denoptim.utils.DENOPTIMMoleculeUtils;
import denoptim.utils.TaskUtils;

/**
 * Task that assesses the fitness of a given graph.
 */

public abstract class FitnessTask extends Task
{
	/**
	 * The graph representation of the entity to evaluate.
	 */
    protected final DENOPTIMGraph dGraph;
    
	/**
	 * The chemical representation of the entity to evaluate. We do not check 
	 * for consistency between this member and the graph representation.
	 * This data structure holds also lost of attributes that are not used
	 * in all subclasses extending the FitnessTask. Moreover, it will be updated
	 * once a presumably refined molecular representation is produced by
	 * a fitness provider.
	 */
    protected IAtomContainer fitProvMol = null;
    
    /**
     * The data structure holding the results of this task
     */
    protected Candidate result;
    
    /**
     * The file where we store the input to the fitness provider.
     */
    protected String fitProvInputFile = "noName"
            + DENOPTIMConstants.FITFILENAMEEXTIN;
    
    /**
     * The file where we store the final output from the fitness provider.
     */
    protected String fitProvOutFile = "noName" 
            + DENOPTIMConstants.FITFILENAMEEXTOUT;
    
    /**
     * The file where we store the a graphical representation of the candidate 
     * (i.e., a picture).
     */
    protected String fitProvPNGFile = "noName"
            + DENOPTIMConstants.CANDIDATE2DEXTENSION;
    
    /**
     * The file where we store the list of unique identifiers or previously 
     * evaluated candidates.
     */
    protected String fitProvUIDFile = null;
    
    /**
     * Flag specifying if a valid fitness value is required to consider the
     * task successfully complete.
     */
    protected boolean fitnessIsRequired = false;

//------------------------------------------------------------------------------
    
    public FitnessTask(Candidate c)
    {
    	super(TaskUtils.getUniqueTaskIndex());
    	this.result = c;
        this.dGraph = c.getGraph();
    }

//------------------------------------------------------------------------------

    /**
     * This method runs the actual evaluation of the fitness, whether that is 
     * run internally (i.e., within this instance of the JAVA VM), or 
     * delegated to an external child process.
     * @return the object with data obtained from the fitness provider.
     * @throws DENOPTIMException
     */
    protected Candidate runFitnessProvider() throws DENOPTIMException
    {
    	// Ensure these two variables have been set
        result.setSDFFile(fitProvOutFile);
        if (fitProvMol == null)
    	{
            ThreeDimTreeBuilder t3d = new ThreeDimTreeBuilder();
            fitProvMol = t3d.convertGraphTo3DAtomContainer(dGraph,true);
    	}
        
        if (fitProvMol.getProperty(DENOPTIMConstants.GMSGTAG) == null ||
        		fitProvMol.getProperty(
        		        DENOPTIMConstants.GMSGTAG).toString().equals(""))
        {
        	fitProvMol.removeProperty(DENOPTIMConstants.GMSGTAG);
        }
        
        // Run fitness provider
        boolean status = false;
        if (FitnessParameters.useExternalFitness()) {
            // Write file with input data to fitness provider
            DenoptimIO.writeSDFFile(fitProvInputFile, fitProvMol, false);

            // NB: inside this call we change fitProvMol for a reordered copy: 
            //     reference will not work!
            status = runExternalFitness();
        } else {
        	// NB: the internal fitness provider removes dummy atoms before 
            // calculating CDK descriptors, so the 'fitProvMol' changes
            status = runInternalFitness();
        }
        
        // Write the FIT file
        result.setChemicalRepresentation(fitProvMol);
        DenoptimIO.writeCandidateToFile(new File(fitProvOutFile), result, false);
        
        // Optional image creation
        if (status && FitnessParameters.makePictures())
        {
            try
            {
                DENOPTIMMoleculeUtils.moleculeToPNG(fitProvMol,fitProvPNGFile);
                result.setImageFile(fitProvPNGFile);
            }
            catch (Exception ex)
            {
                result.setImageFile(null);
                DENOPTIMLogger.appLogger.log(Level.WARNING, 
                    "Unable to create image. {0}", ex.getMessage());
            }
        }
        
        return result;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * @return <code>true</code> if it is all good, <code>false</code> in case 
     * of any reason for premature returning of the results (error generated in
     * from the external tool, rejection on the candidate).
     * @throws DENOPTIMException 
     * @throws Exception 
     */

	private boolean runExternalFitness() throws DENOPTIMException
	{
		StringBuilder sb = new StringBuilder();
        sb.append(FitnessParameters.getExternalFitnessProviderInterpreter());
        sb.append(" ").append(
        		FitnessParameters.getExternalFitnessProvider())
              .append(" ").append(fitProvInputFile)
              .append(" ").append(fitProvOutFile)
              .append(" ").append(workDir)
              .append(" ").append(id);
        if (fitProvUIDFile != null)
        {
            sb.append(" ").append(fitProvUIDFile);
        }
        
        String msg = "Calling external fitness provider: => " + sb + NL;
        DENOPTIMLogger.appLogger.log(Level.INFO, msg);

        // run the process
        processHandler = new ProcessHandler(sb.toString(), 
        		Integer.toString(id));

        processHandler.runProcess();
        if (processHandler.getExitCode() != 0)
        {
            msg = "Failed to execute fitness provider " 
                + FitnessParameters.getExternalFitnessProviderInterpreter()
                    .toString()
		        + " command '" + FitnessParameters.getExternalFitnessProvider()
		        + "' on " + fitProvInputFile;
            DENOPTIMLogger.appLogger.severe(msg);
            DENOPTIMLogger.appLogger.severe(
            		processHandler.getErrorOutput());
            throw new DENOPTIMException(msg);
        }
        processHandler = null;
        
        // Read results from fitness provider
        IChemObjectBuilder builder = SilentChemObjectBuilder.getInstance();
        IAtomContainer processedMol = builder.newAtomContainer();
        boolean unreadable = false;
        try
        {
            processedMol = DenoptimIO.getFirstMolInSDFFile(fitProvOutFile);
            if (processedMol.isEmpty())
            {
                unreadable=true;
            }
            Object o = processedMol.getProperty(DENOPTIMConstants.ATMPROPVERTEXID);
            if (o != null)
            {
                String[] parts = o.toString().trim().split("\\s+");
                if (processedMol.getAtomCount() != parts.length)
                {
                    throw new DENOPTIMException("Inconsistent number of vertex "
                            + "IDs (" + parts.length + ") and atoms (" 
                            + processedMol.getAtomCount() + ") in candidate "
                            + "processed by external fitness provider.");
                }
                for (int i=0; i<processedMol.getAtomCount(); i++)
                {
                    processedMol.getAtom(i).setProperty(
                            DENOPTIMConstants.ATMPROPVERTEXID, 
                            Integer.parseInt(parts[i]));
                }
            }
        }
        catch (Throwable t)
        {
            unreadable=true;
        }
        
        if (unreadable)
        {
        	// If file is not properly readable, we keep track of the 
        	// unreadable file, and we label the candidate to signal the 
        	// error, and we replace the unreadable one with a file that 
        	// is readable.
        	
            msg = "Unreadable file from fitness provider run (Task " + id 
            		+ "). Check " + result.getName() + ".";
            DENOPTIMLogger.appLogger.log(Level.WARNING, msg);
            
            String fileBkp = fitProvOutFile 
                    + DENOPTIMConstants.UNREADABLEFILEPOSTFIX;
            try {
				FileUtils.copyFile(new File(fitProvOutFile), new File(fileBkp));
			} catch (IOException e) {
				// At this point the file must be there!
				throw new DENOPTIMException("File '"+ fitProvOutFile + "' has "
						+ "disappeared (it was there, but not anymore!)");
			}
            FileUtils.deleteQuietly(new File(fitProvOutFile));
            
            String err = "#FTask: Unable to retrive data. See " + fileBkp;
            processedMol = new AtomContainer();
            processedMol.addAtom(new Atom("H"));
            
            result.setChemicalRepresentation(processedMol);
            result.setError(err);
            return false;
        }
        
        // Unique identifier might be updated by the fitness provider, so
        // we need to update in the the returned value
        if (processedMol.getProperty(DENOPTIMConstants.UNIQUEIDTAG) != null)
        {
            result.setUID(processedMol.getProperty(
            		DENOPTIMConstants.UNIQUEIDTAG).toString());
        }
        
        if (processedMol.getProperty(DENOPTIMConstants.MOLERRORTAG) != null)
        {
        	String err = processedMol.getProperty(
        			DENOPTIMConstants.MOLERRORTAG).toString();
            msg = result.getName() + " has an error ("+err+")";
            DENOPTIMLogger.appLogger.info(msg);

            result.setChemicalRepresentation(processedMol);
            result.setError(err);
            return false;
        }
        
        if (processedMol.getProperty(DENOPTIMConstants.FITNESSTAG) != null)
        {
            String fitprp = processedMol.getProperty(
            		DENOPTIMConstants.FITNESSTAG).toString();
            double fitVal = 0.0;
            try
            {
                fitVal = Double.parseDouble(fitprp);
            }
            catch (Throwable t)
            {
                synchronized (lock)
                {
                    hasException = true;
                    msg = "Fitness value '" + fitprp + "' of " 
                            + result.getName() + " could not be converted "
                            + "to double.";
                    errMsg = msg;
                    thrownExc = t;
                }
                DENOPTIMLogger.appLogger.severe(msg);
                dGraph.cleanup();
                throw new DENOPTIMException(msg);
            }

            if (Double.isNaN(fitVal))
            {
                synchronized (lock)
                {
                    hasException = true;
                    msg = "Fitness value is NaN for " + result.getName();
                    errMsg = msg;
                }
                DENOPTIMLogger.appLogger.severe(msg);
                dGraph.cleanup();
                throw new DENOPTIMException(msg);
            }
            
            //TODO: consider this...
            // We want to retain as much as possible of the info we had on the
            // initial, pre-processing molecular representation. However, the
            // external task may have altered the molecular representation
            // to the point we cannot recover. Still, since the graph may be
            // conceptual, i.e., it intentionally does not translate in a valid
            // molecular representation within DENOPTIM, but it does so only
            // within the external fitness provider, we might still prefer
            // to collect as final molecular representation that generated by
            // the external tasks. This could be something to be made optional.
            
            // Replace initial molecular representation of this object with 
            // that coming from the external fitness provider.
            fitProvMol = processedMol;
            result.setChemicalRepresentation(processedMol);
            result.setFitness(fitVal);
        } else {
            if (fitnessIsRequired)
            {
                synchronized (lock)
                {
                    hasException = true;
                    msg = "Could not find '" + DENOPTIMConstants.FITNESSTAG 
                    		+ "' tag in: " + fitProvOutFile;
                    errMsg = msg;
                }
                DENOPTIMLogger.appLogger.severe(msg);
                throw new DENOPTIMException(msg);
        	}
        }
        return true;
	}
	
//------------------------------------------------------------------------------

    /**
     * @return <code>true</code> if it is all good, <code>false</code> in case 
     * of any reason for premature returning of the results.
     * @throws DENOPTIMException 
     * @throws Exception 
     */
	
	private boolean runInternalFitness() throws DENOPTIMException 
	{
		String msg = "Calling internal fitness provider. "+ NL;
	    DENOPTIMLogger.appLogger.log(Level.INFO, msg);

	    double fitVal = Double.NaN;
		try {
			FitnessProvider fp = new FitnessProvider(
					FitnessParameters.getDescriptors(),
					FitnessParameters.getFitnessExpression());
			// NB: here we remove dummy atoms!
			fitVal = fp.getFitness(fitProvMol);
		} catch (Exception e) {
			throw new DENOPTIMException("Failed to calculate fitness.", e);
		}

        if (Double.isNaN(fitVal))
        {
            
            //synchronized (lock)
            //{
            //    hasException = true;
                msg = "Fitness value is NaN for " + result.getName();
                errMsg = msg;
            //}
            DENOPTIMLogger.appLogger.severe(msg);
            
            fitProvMol.removeProperty(DENOPTIMConstants.FITNESSTAG);
            fitProvMol.setProperty(DENOPTIMConstants.MOLERRORTAG, 
                    "#InternalFitness: NaN value");
            result.setError(msg);
            
            //TODO-V3 make ignoring of NaN optional
            /*
            dGraph.cleanup();
            throw new DENOPTIMException(msg);
            */
        } else {
            result.setFitness(fitVal);
        }
        
		return true;
	}

//------------------------------------------------------------------------------

}

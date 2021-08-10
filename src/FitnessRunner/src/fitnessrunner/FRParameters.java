/*
 *   DENOPTIM
 *   Copyright (C) 2019 Marco Foscato <marco.foscato@uib.no>
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

package fitnessrunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Date;

import denoptim.exception.DENOPTIMException;
import denoptim.fitness.FitnessParameters;
import denoptim.fragspace.FragmentSpaceParameters;
import denoptim.io.DenoptimIO;
import denoptim.io.FileFormat;
import denoptim.logging.DENOPTIMLogger;
import denoptim.rings.RingClosureParameters;


/**
 * Parameters controlling execution of FitnessRunner.
 * 
 * @author Marco Foscato
 */

public class FRParameters
{
    /**
     * Flag indicating that at least one parameter has been defined
     */
    private static boolean frParamsInUse = false;

    /**
     * Working directory
     */
    protected static String workDir = System.getProperty("user.dir");

    /**
     * File with input for fitness provider
     */
    private static File inpFile = null;
    
    /**
     * File where the results of the fitness evaluation will be printed
     */
    private static File outFile = null;
    
    /**
     * Verbosity level
     */
    private static int verbosity = 0;
    
    
//-----------------------------------------------------------------------------

    /**
     * Restores the default values of the most important parameters. 
     * Given that this is a static collection of parameters, running subsequent
     * experiments from the GUI ends up reusing parameters from the previous
     * run. This method allows to clean-up old values.
     */
    public static void resetParameters()
    {
        frParamsInUse = false;
        workDir = System.getProperty("user.dir");
        inpFile = null;
        outFile = null;
        verbosity = 0;
        
        FitnessParameters.resetParameters();        
    }

//-----------------------------------------------------------------------------

    public static boolean frParamsInUse()
    {
        return frParamsInUse;
    }

//-----------------------------------------------------------------------------

    public static String getWorkDirectory()
    {
        return workDir;
    }
   
//-----------------------------------------------------------------------------

    public static File getInputFile()
    {
        return inpFile;
    }
    
//-----------------------------------------------------------------------------

    public static File getOutputFile()
    {
        return outFile;
    }

//-----------------------------------------------------------------------------

    public static int getVerbosity()
    {
	    return verbosity;
    }

//-----------------------------------------------------------------------------

    /**
     * Read the parameter TXT file line by line and interpret its content.
     * @param infile
     * @throws DENOPTIMException
     */

    public static void readParameterFile(String infile) throws DENOPTIMException
    {
        String option, line;
        BufferedReader br = null;
        try
        {
            br = new BufferedReader(new FileReader(infile));
            while ((line = br.readLine()) != null)
            {
                if ((line.trim()).length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

                if (line.toUpperCase().startsWith("FR-"))
                {
                    interpretKeyword(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("FS-"))
                {
                    FragmentSpaceParameters.interpretKeyword(line);
                    continue;
                }

                if (line.toUpperCase().startsWith("RC-"))
                {
                    RingClosureParameters.interpretKeyword(line);
                    continue;
                }
               
                if (line.toUpperCase().startsWith("FP-"))
                {
                    FitnessParameters.interpretKeyword(line);
                    continue;
                }
            }
        }
        catch (NumberFormatException | IOException nfe)
        {
            throw new DENOPTIMException(nfe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                    br = null;
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        option = null;
        line = null;
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a string looking for keyword and a possibly associated value.
     * @param line the string to parse
     * @throws DENOPTIMException
     */

    public static void interpretKeyword(String line) throws DENOPTIMException
    {
        String key = line.trim();
        String value = "";
        if (line.contains("="))
        {
            key = line.substring(0,line.indexOf("=") + 1).trim();
            value = line.substring(line.indexOf("=") + 1).trim();
        }
        try
        {
            interpretKeyword(key,value);
        }
        catch (DENOPTIMException e)
        {
            throw new DENOPTIMException(e.getMessage()+" Check line "+line);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Processes a keyword/value pair and assign the related parameters.
     * @param key the keyword as string
     * @param value the value as a string
     * @throws DENOPTIMException
     */

    public static void interpretKeyword(String key, String value)
                                                      throws DENOPTIMException
    {
        frParamsInUse = true;
        String msg = "";
        switch (key.toUpperCase())
        {
        case "FR-WORKDIR=":
            workDir = value;
            break;
		case "FR-INPUT=":
		    inpFile = new File(value);
		    break;
        case "FR-OUTPUT=":
            outFile = new File(value);
            break;
        case "FR-VERBOSITY=":
            try
            {
                verbosity = Integer.parseInt(value);
            }
            catch (Throwable t)
            {
                msg = "Unable to understand value " + key + "'" + value + "'";
                throw new DENOPTIMException(msg);
            }
            break;
        default:
             msg = "Keyword " + key + " is not a known FragmentSpaceExplorer-" 
            		 + "related keyword. Check input files.";
            throw new DENOPTIMException(msg);
        }
    }

//-----------------------------------------------------------------------------

    /**
     * Evaluate consistency of input parameters.
     * @throws DENOPTIMException
     */

    public static void checkParameters() throws DENOPTIMException
    {
        String msg = "";
        if (!frParamsInUse)
        {
            return;
        }
    
    	if (!workDir.equals(".") && !DenoptimIO.checkExists(workDir))
    	{
    	   msg = "Directory '" + workDir + "' not found. Please specify an "
    		 + "existing directory.";
    	   throw new DENOPTIMException(msg);
    	}

    	if (inpFile != null && !inpFile.exists())
    	{
    	    msg = "File with input data not found. Check " + inpFile;
                throw new DENOPTIMException(msg);
    	}
    	
        if (outFile != null && outFile.exists())
        {
            msg = "File meant for output results exists and we do not overwrite.";
            throw new DENOPTIMException(msg);
        }

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.checkParameters();
        }

        if (RingClosureParameters.rcParamsInUse())
        {
            RingClosureParameters.checkParameters();
        }
    }

//----------------------------------------------------------------------------

    /**
     * Processes all parameters and initialize related objects.
     * @throws DENOPTIMException
     */

    public static void processParameters() throws DENOPTIMException
    {
        DenoptimIO.addToRecentFiles(outFile, FileFormat.GRAPHSDF);

        if (FragmentSpaceParameters.fsParamsInUse())
        {
            FragmentSpaceParameters.processParameters();
        }

        if (RingClosureParameters.allowRingClosures())
        {
            RingClosureParameters.processParameters();
        }
        
        if (FitnessParameters.fitParamsInUse())
        {
            FitnessParameters.processParameters();
        }
        
        System.err.println("Output files associated with the current run are " +
                                "located in " + workDir);
    }

//----------------------------------------------------------------------------

    /**
     * Print all parameters. 
     */

    public static void printParameters()
    {
		if (!frParamsInUse)
		{
		    return;
		}
        String eol = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(1024);
        sb.append(" frParameters ").append(eol);
        for (Field f : FRParameters.class.getDeclaredFields()) 
        {
            try
            {
                sb.append(f.getName()).append(" = ").append(
                            f.get(FRParameters.class)).append(eol);
            }
            catch (Throwable t)
            {
                sb.append("ERROR! Unable to print FRParameters. Cause: " + t);
                break;
            }
        }
        DENOPTIMLogger.appLogger.info(sb.toString());
        sb.setLength(0);

        FragmentSpaceParameters.printParameters();
        RingClosureParameters.printParameters();
        FitnessParameters.printParameters();
    }

//----------------------------------------------------------------------------

}
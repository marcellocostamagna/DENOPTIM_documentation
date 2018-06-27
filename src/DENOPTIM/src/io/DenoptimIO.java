package io;

/**
 * Utility methods for input/output
 *
 * @author Vishwesh Venkatraman
 * @author Marco Foscato
 */


import java.util.ArrayList;
import java.util.BitSet;
import java.io.FileNotFoundException;
import java.io.InvalidClassException;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.File;
import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.FileOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.vecmath.Point3d;
import java.util.Properties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import javax.imageio.ImageIO;
import java.awt.geom.Rectangle2D;
import java.awt.RenderingHints;
import java.text.SimpleDateFormat;
import java.util.Map;

import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.io.Mol2Writer;
import org.openscience.cdk.io.XYZWriter;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.AtomContainerSet;
import org.openscience.cdk.interfaces.IAtomContainerSet;
import org.openscience.cdk.tools.FormatStringBuffer;
import org.openscience.cdk.io.MDLV2000Writer;
import org.openscience.cdk.io.listener.PropertiesListener;
import org.openscience.cdk.geometry.GeometryTools;
import org.openscience.cdk.renderer.AtomContainerRenderer;
import org.openscience.cdk.renderer.font.AWTFontManager;
import org.openscience.cdk.renderer.generators.BasicAtomGenerator;
import org.openscience.cdk.renderer.generators.BasicBondGenerator;
import org.openscience.cdk.renderer.generators.BasicSceneGenerator;
import org.openscience.cdk.renderer.generators.IGenerator;
import org.openscience.cdk.renderer.visitor.AWTDrawVisitor;

import exception.DENOPTIMException;
import org.openscience.cdk.CDKConstants;
import org.openscience.cdk.interfaces.IAtom;
import org.openscience.cdk.interfaces.IBond;
import org.openscience.cdk.renderer.RendererModel;
import org.openscience.cdk.smiles.InvPair;
import molecule.DENOPTIMGraph;
import utils.DENOPTIMGraphEdit;
import utils.GenUtils;
import utils.GraphConversionTool;
import utils.DENOPTIMMoleculeUtils;
import rings.ClosableChain;
import logging.DENOPTIMLogger;
import java.util.logging.Level;


public class DenoptimIO
{
    private static final String lsep = System.getProperty("line.separator");

    // A list of properties used by CDK algorithms which must never be
    // serialized into the SD file format.

    private static final ArrayList<String> cdkInternalProperties
            = new ArrayList<>(Arrays.asList(new String[]
                {InvPair.CANONICAL_LABEL, InvPair.INVARIANCE_PAIR}));

//------------------------------------------------------------------------------

    /**
     * Reads a text file containing links to multiple molecules mol/sdf format
     *
     * @param filename the file containing the list of molecules
     * @return IAtomContainer[] an array of molecules
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readTxtFile(String filename)
            throws DENOPTIMException
    {
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();
        String sCurrentLine;

        BufferedReader br = null;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((sCurrentLine = br.readLine()) != null)
            {
                sCurrentLine = sCurrentLine.trim();
                if (sCurrentLine.length() == 0)
                {
                    continue;
                }
                if (GenUtils.getFileExtension(sCurrentLine).
                        compareToIgnoreCase(".smi") == 0)
                {
                    throw new DENOPTIMException("Fragment files in SMILES format not supported.");
                }

                ArrayList<IAtomContainer> mols = readSDFFile(sCurrentLine);
                lstContainers.addAll(mols);
            }
        }
        catch (FileNotFoundException fnfe)
        {
            throw new DENOPTIMException(fnfe);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        catch (DENOPTIMException de)
        {
            throw de;
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        return lstContainers;

    }

//------------------------------------------------------------------------------

    /**
     * Reads a file containing multiple molecules (multiple SD format))
     *
     * @param filename the file containing the molecules
     * @return IAtomContainer[] an array of molecules
     * @throws DENOPTIMException
     */
    public static ArrayList<IAtomContainer> readSDFFile(String filename)
            throws DENOPTIMException
    {
        MDLV2000Reader mdlreader = null;
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();

        try
        {
            mdlreader = new MDLV2000Reader(new FileReader(new File(filename)));
            ChemFile chemFile = (ChemFile) mdlreader.read((ChemObject) new ChemFile());
            lstContainers.addAll(
                    ChemFileManipulator.getAllAtomContainers(chemFile));
        }
        catch (CDKException | IOException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        finally
        {
            try
            {
                if (mdlreader != null)
                {
                    mdlreader.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (lstContainers.isEmpty())
        {
            throw new DENOPTIMException("No data found in " + filename);
        }

        return lstContainers;
        //return lstContainers.toArray(new IAtomContainer[lstContainers.size()]);
    }

//------------------------------------------------------------------------------
    
    /**
     * Reads a file containing multiple molecules (multiple SD format))
     *
     * @param filename the file containing the molecules
     * @return IAtomContainer[] an array of molecules
     * @throws DENOPTIMException
     */
    public static IAtomContainer readSingleSDFFile(String filename)
            throws DENOPTIMException
    {
        MDLV2000Reader mdlreader = null;
        ArrayList<IAtomContainer> lstContainers = new ArrayList<>();

        try
        {
            mdlreader = new MDLV2000Reader(new FileReader(new File(filename)));
            ChemFile chemFile = (ChemFile) mdlreader.read((ChemObject) new ChemFile());
            lstContainers.addAll(
                    ChemFileManipulator.getAllAtomContainers(chemFile));
        }
        catch (CDKException | IOException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        finally
        {
            try
            {
                if (mdlreader != null)
                {
                    mdlreader.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (lstContainers.isEmpty())
        {
            throw new DENOPTIMException("No data found in " + filename);
        }

        return lstContainers.get(0);
        //return lstContainers.toArray(new IAtomContainer[lstContainers.size()]);
    }

//------------------------------------------------------------------------------

    /**
     * Writes the 2D/3D representation of the molecule to multi-SD file
     *
     * @param filename The file to be written to
     * @param mols The molecules to be written
     * @throws DENOPTIMException
     */
    public static void writeMoleculeSet(String filename,
            ArrayList<IAtomContainer> mols)
            throws DENOPTIMException
    {
        SDFWriter sdfWriter = null;
        try
        {
            IAtomContainerSet molSet = new AtomContainerSet();
            for (int idx = 0; idx < mols.size(); idx++)
            {
                molSet.addAtomContainer(mols.get(idx));
            }
            sdfWriter = new SDFWriter(new FileWriter(new File(filename)));
            sdfWriter.write(molSet);
        }
        catch (CDKException | IOException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        finally
        {
            try
            {
                if (sdfWriter != null)
                {
                    sdfWriter.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a single molecule to the specified file
     *
     * @param filename The file to be written to
     * @param mol The molecule to be written
     * @param append
     * @throws DENOPTIMException
     */
    public static void writeMolecule(String filename, IAtomContainer mol,
            boolean append) throws DENOPTIMException
    {
        SDFWriter sdfWriter = null;
        try
        {
            sdfWriter = new SDFWriter(new FileWriter(new File(filename), append));
            sdfWriter.write(mol);
        }
        catch (CDKException | IOException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        finally
        {
            try
            {
                if (sdfWriter != null)
                {
                    sdfWriter.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    public static void writeMol2File(String filename, IAtomContainer mol,
            boolean append) throws DENOPTIMException
    {
        Mol2Writer mol2Writer = null;
        try
        {
            mol2Writer = new Mol2Writer(new FileWriter(new File(filename), append));
            mol2Writer.write(mol);
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (mol2Writer != null)
                {
                    mol2Writer.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    public static void writeXYZFile(String filename, IAtomContainer mol,
            boolean append) throws DENOPTIMException
    {
        XYZWriter xyzWriter = null;
        try
        {
            xyzWriter = new XYZWriter(new FileWriter(new File(filename), append));
            xyzWriter.write(mol);
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (xyzWriter != null)
                {
                    xyzWriter.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes multiple smiles string array to the specified file
     *
     * @param filename The file to be written to
     * @param smiles array of smiles strings to be written
     * @param append if
     * <code>true</code> append to the file
     * @throws DENOPTIMException
     */
    public static void writeSmilesSet(String filename, String[] smiles,
            boolean append) throws DENOPTIMException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(new File(filename), append);
            for (int i = 0; i < smiles.length; i++)
            {
                fw.write(smiles[i] + lsep);
                fw.flush();
            }
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (fw != null)
                {
                    fw.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Writes a single smiles string to the specified file
     *
     * @param filename The file to be written to
     * @param smiles
     * @param append if
     * <code>true</code> append to the file
     * @throws DENOPTIMException
     */
    public static void writeSmiles(String filename, String smiles,
            boolean append) throws DENOPTIMException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(new File(filename), append);
            fw.write(smiles + lsep);
            fw.flush();
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (fw != null)
                {
                    fw.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write a data file
     *
     * @param filename
     * @param data
     * @param append
     * @throws DENOPTIMException
     */
    public static void writeData(String filename, String data, boolean append)
            throws DENOPTIMException
    {
        FileWriter fw = null;
        try
        {
            fw = new FileWriter(new File(filename), append);
            fw.write(data + lsep);
            fw.flush();
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (fw != null)
                {
                    fw.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Serialize an object into a given file
     *
     * @param filename
     * @param obj
     * @param append
     * @throws DENOPTIMException
     */
    public static void serializeToFile(String filename, Object obj, 
								 boolean append)
							throws DENOPTIMException
    {
	FileOutputStream fos = null;
	ObjectOutputStream oos = null;
        try
        {
            fos = new FileOutputStream(filename, append);
	    oos = new ObjectOutputStream(fos);
            oos.writeObject(obj);
            oos.close();
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException("Cannot serialize object.", t);
        }
        finally
        {
	    try
	    {
	        fos.flush();
                fos.close();
                fos = null;
	    } 
	    catch (Throwable t)
	    {
		throw new DENOPTIMException("cannot close FileOutputStream",t);
	    }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Deserialize a <code>DENOPTIMGraph</code> from a given file
     * @param file the given file
     * @return the graph
     * @throws DENOPTIMException if anything goes wrong
     */

    public static DENOPTIMGraph deserializeDENOPTIMGraph(File file)
                                                        throws DENOPTIMException
    {
        DENOPTIMGraph graph = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        try
        {
            fis = new FileInputStream(file);
            ois = new ObjectInputStream(fis);
            graph = (DENOPTIMGraph) ois.readObject();
            ois.close();
        }
        catch (InvalidClassException ice)
        {
	    String msg = "Attempt to deserialized old graph generated by an "
                        + "older version of DENOPTIM. A serialized graph "
			+ "can only be read by the version of DENOPTIM that "
			+ "has generate the serialized file.";
            throw new DENOPTIMException(msg);
        }
        catch (Throwable t)
        {
            throw new DENOPTIMException(t);
        }
        finally
        {
            try
            {
                fis.close();
            }
            catch (Throwable t)
            {
                throw new DENOPTIMException(t);
            }
        }

        return graph;
    }

//------------------------------------------------------------------------------

    /**
     * Creates a zip file
     *
     * @param zipOutputFileName
     * @param filesToZip
     * @throws Exception
     */
    public static void createZipFile(String zipOutputFileName,
            String[] filesToZip) throws Exception
    {
        FileOutputStream fos = new FileOutputStream(zipOutputFileName);
        ZipOutputStream zos = new ZipOutputStream(fos);
        int bytesRead;
        byte[] buffer = new byte[1024];
        CRC32 crc = new CRC32();
        for (int i = 0, n = filesToZip.length; i < n; i++)
        {
            String fname = filesToZip[i];
            File cFile = new File(fname);
            if (!cFile.exists())
            {
                //System.err.println("Skipping: " + fname);
                continue;
            }

            BufferedInputStream bis = new BufferedInputStream(
                    new FileInputStream(cFile));
            crc.reset();
            while ((bytesRead = bis.read(buffer)) != -1)
            {
                crc.update(buffer, 0, bytesRead);
            }
            bis.close();
            // Reset to beginning of input stream
            bis = new BufferedInputStream(new FileInputStream(cFile));
            ZipEntry ze = new ZipEntry(fname);
            // DEFLATED setting for a compressed version
            ze.setMethod(ZipEntry.DEFLATED);
            ze.setCompressedSize(cFile.length());
            ze.setSize(cFile.length());
            ze.setCrc(crc.getValue());
            zos.putNextEntry(ze);
            while ((bytesRead = bis.read(buffer)) != -1)
            {
                zos.write(buffer, 0, bytesRead);
            }
            bis.close();
        }
        zos.close();
    }

//------------------------------------------------------------------------------

    /**
     * Delete the file
     *
     * @param fileName
     * @throws DENOPTIMException
     */
    public static void deleteFile(String fileName) throws DENOPTIMException
    {
        File f = new File(fileName);
        // Make sure the file or directory exists and isn't write protected
        if (!f.exists())
        {
            //System.err.println("Delete: no such file or directory: " + fileName);
            return;
        }

        if (!f.canWrite())
        {
            //System.err.println("Delete: write protected: " + fileName);
            return;
        }

        // If it is a directory, make sure it is empty
        if (f.isDirectory())
        {
            //System.err.println("Delete operation on directory not supported");
            return;
        }

        // Attempt to delete it
        boolean success = f.delete();

        if (!success)
        {
            throw new DENOPTIMException("Deletion of " + fileName + " failed.");
        }
    }

//------------------------------------------------------------------------------

    /**
     * Delete all files with pathname containing a given string
     *
     * @param path
     * @param pattern 
     * @throws DENOPTIMException
     */
    public static void deleteFilesContaining(String path, String pattern) 
							throws DENOPTIMException
    {
        File folder = new File(path);
	File[] listOfFiles = folder.listFiles();
	for (int i=0; i<listOfFiles.length; i++)
	{
	    if (listOfFiles[i].isFile())
	    {
		String name = listOfFiles[i].getName();
		if (name.contains(pattern))
		{
		    deleteFile(listOfFiles[i].getAbsolutePath());
		}
	    }
        }
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param filename
     * @return
     * <code>true</code> if directory is successfully created
     */
    public static boolean createDirectory(String filename)
    {
        return (new File(filename)).mkdir();
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param filename
     * @return
     * <code>true</code> if file exists
     */
    public static boolean checkExists(String filename)
    {
        if (filename.length() > 0)
        {
            return (new File(filename)).exists();
        }
        return false;
    }

//------------------------------------------------------------------------------

    /**
     * Count the number of lines in the file
     *
     * @param filename
     * @return number of lines in the file
     * @throws DENOPTIMException
     */
    public static int countLinesInFile(String filename) throws DENOPTIMException
    {
        BufferedInputStream bis = null;
        try
        {
            bis = new BufferedInputStream(new FileInputStream(filename));
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            while ((readChars = bis.read(c)) != -1)
            {
                for (int i = 0; i < readChars; ++i)
                {
                    if (c[i] == '\n')
                    {
                        ++count;
                    }
                }
            }
            return count;
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (bis != null)
                {
                    bis.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     *
     * @param filename
     * @return list of fingerprints in bit representation
     * @throws DENOPTIMException
     */
    public static ArrayList<BitSet> readFingerprintData(String filename)
            throws DENOPTIMException
    {
        ArrayList<BitSet> fps = new ArrayList<>();

        BufferedReader br = null;
        String sCurrentLine;

        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((sCurrentLine = br.readLine()) != null)
            {
                if (sCurrentLine.trim().length() == 0)
                {
                    continue;
                }
                String[] str = sCurrentLine.split(", ");
                int n = str.length - 1;
                BitSet bs = new BitSet(n);
                for (int i = 0; i < n; i++)
                {
                    bs.set(i, Boolean.parseBoolean(str[i + 1]));
                }
                fps.add(bs);
            }
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (fps.isEmpty())
        {
            throw new DENOPTIMException("No data found in file: " + filename);
        }

        return fps;
    }

//------------------------------------------------------------------------------

    /**
     * Perform a deep copy of the object
     *
     * @param oldObj
     * @return a deep copy of an object
     * @throws DENOPTIMException
     */
    public static Object deepCopy(Object oldObj) throws DENOPTIMException
    {
        ObjectOutputStream oos = null;
        ObjectInputStream ois = null;
        try
        {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            // serialize and pass the object
            oos.writeObject(oldObj);
            oos.flush();
            ByteArrayInputStream bin =
                    new ByteArrayInputStream(bos.toByteArray());
            ois = new ObjectInputStream(bin);
            // return the new object
            return ois.readObject();
        }
        catch (IOException | ClassNotFoundException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (oos != null)
                {
                    oos.close();
                }
                if (ois != null)
                {
                    ois.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Read list of data
     *
     * @param filename
     * @return list of data
     * @throws DENOPTIMException
     */
    public static ArrayList<String> readList(String filename) throws DENOPTIMException
    {
        ArrayList<String> lst = new ArrayList<>();
        BufferedReader br = null;
        String line = null;
        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }
                lst.add(line.trim());
            }
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (lst.isEmpty())
        {
            throw new DENOPTIMException("No data found in file: " + filename);
        }

        return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Write the coordinates in XYZ format
     *
     * @param filename
     * @param atom_symbols
     * @param atom_coords
     * @throws DENOPTIMException
     */
    public static void writeXYZFile(String filename, ArrayList<String> atom_symbols,
            ArrayList<Point3d> atom_coords) throws DENOPTIMException
    {
        FileWriter fw = null;
        FormatStringBuffer fsb = new FormatStringBuffer("%-8.6f");
        try
        {
            String molname = filename.substring(0, filename.length() - 4);
            fw = new FileWriter(new File(filename));
            int numatoms = atom_symbols.size();
            fw.write("" + numatoms + lsep);
            fw.flush();
            fw.write(molname + lsep);

            String line = "", st = "";

            for (int i = 0; i < atom_symbols.size(); i++)
            {
                st = atom_symbols.get(i);
                Point3d p3 = atom_coords.get(i);

                line = st + "\t" + (p3.x < 0 ? "" : " ") + fsb.format(p3.x) + "\t"
                        + (p3.y < 0 ? "" : " ") + fsb.format(p3.y) + "\t"
                        + (p3.z < 0 ? "" : " ") + fsb.format(p3.z);
                fw.write(line + lsep);
                fw.flush();
            }
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        finally
        {
            try
            {
                if (fw != null)
                {
                    fw.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * Generate the ChemDoodle representation of the molecule
     *
     * @param mol
     * @return molecule as a formatted string
     * @throws DENOPTIMException
     */
    public static String getChemDoodleString(IAtomContainer mol)
            throws DENOPTIMException
    {
        StringWriter stringWriter = new StringWriter();
        MDLV2000Writer mw = null;
        try
        {
            mw = new MDLV2000Writer(stringWriter);
            mw.write(mol);
        }
        catch (CDKException cdke)
        {
            throw new DENOPTIMException(cdke);
        }
        finally
        {
            try
            {
                if (mw != null)
                {
                    mw.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        String MoleculeString = stringWriter.toString();

        //System.out.print(stringWriter.toString());
        //now split MoleculeString into multiple lines to enable explicit printout of \n
        String Moleculelines[] = MoleculeString.split("\\r?\\n");

        StringBuilder sb = new StringBuilder(1024);
        sb.append("var molFile = '");
        for (int i = 0; i < Moleculelines.length; i++)
        {
            sb.append(Moleculelines[i]);
            sb.append("\\n");
        }
        sb.append("';");
        return sb.toString();
    }

//------------------------------------------------------------------------------

    public static void writeMolecule2D(String filename, IAtomContainer mol)
                                                        throws DENOPTIMException
    {
        MDLV2000Writer writer = null;

        try
        {
            writer = new MDLV2000Writer(new FileWriter(new File(filename)));
            Properties customSettings = new Properties();
            customSettings.setProperty("ForceWriteAs2DCoordinates", "true");
            PropertiesListener listener = new PropertiesListener(customSettings);
            writer.addChemObjectIOListener(listener);
            writer.writeMolecule(mol);
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
        catch (Exception ex)
        {
            throw new DENOPTIMException(ex);
        }
        finally
        {
            try
            {
                if (writer != null)
                {
                    writer.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
    }

//------------------------------------------------------------------------------

    /**
     * The class compatibility matrix
     *
     * @param filename
     * @param compReacMap
     * @param reacBonds
     * @param reacCap
     * @param forbEnd
     * @throws DENOPTIMException
     */
    public static void readCompatibilityMatrix(String filename,
            HashMap<String, ArrayList<String>> compReacMap,
            HashMap<String, Integer> reacBonds, HashMap<String, String> reacCap,
            ArrayList<String> forbEnd)
            throws DENOPTIMException
    {

        BufferedReader br = null;
        String line = null;
        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

                if (line.startsWith("RCN"))
                {
                    String str[] = line.split("\\s+");
                    if (str.length < 3)
                    {
                        String err = "Incomplete reaction compatibility data.";
                        throw new DENOPTIMException(err + " " + filename);
                    }

                    // to account for multiple compatibilities
                    String strRcn[] = str[2].split(",");
                    for (int i=0; i<strRcn.length; i++)
                        strRcn[i] = strRcn[i].trim();

                    compReacMap.put(str[1],
                            new ArrayList<>(Arrays.asList(strRcn)));

                }
                else
                {
                    if (line.startsWith("RBO"))
                    {
                        String str[] = line.split("\\s+");
                        if (str.length != 3)
                        {
                            String err = "Incomplete reaction bondorder data.";
                            throw new DENOPTIMException(err + " " + filename);
                        }
                        reacBonds.put(str[1], new Integer(str[2]));
                    }
                    else
                    {
                        if (line.startsWith("CAP"))
                        {
                            String str[] = line.split("\\s+");
                            if (str.length != 3)
                            {
                                String err = "Incomplete capping reaction data.";
                                throw new DENOPTIMException(err + " " + filename);
                            }
                            reacCap.put(str[1], str[2]);
                        }
                        else
			{
			    if (line.startsWith("DEL"))
			    {
				String str[] = line.split("\\s+");
				if (str.length != 2)
				{
				    for (int is=1; is<str.length; is++)
				    {
					forbEnd.add(str[is]);
				    }
				}
				else
				{
				    forbEnd.add(str[1]);
				}
			    }
			}
                    }
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
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (compReacMap.isEmpty())
        {
            String err = "No reaction compatibility data found in file: ";
            throw new DENOPTIMException(err + " " + filename);
        }

        if (reacBonds.isEmpty())
        {
            String err = "No bond data found in file: ";
            throw new DENOPTIMException(err + " " + filename);
        }

//        System.err.println("RCN");
//        System.err.println(compReacMap.toString());
//
//        System.err.println("RBO");
//        System.err.println(reacBonds.toString());

    }

//------------------------------------------------------------------------------

    /**
     * Reads the APclass compatibility matrix for ring-closing connections 
     * (the RC-CPMap).
     * Note that RC-CPMap is by definition symmetric. Though, <code>true</code>
     * entries can be defined either from X:Y or Y:X, and we make sure
     * such entries are stored in the map. This method assumes
     * that the APclasses reported in the RC-CPMap are defined, w.r.t bond
     * order, in the regular compatibility matrix as we wont
     * check it this condition is satisfied.
     *
     * @param filename 
     * @param rcCompMap
     * @throws DENOPTIMException
     */
    public static void readRCCompatibilityMatrix(String filename,
            HashMap<String, ArrayList<String>> rcCompMap)
            throws DENOPTIMException
    {
        BufferedReader br = null;
        String line = null;
        try
        {
            br = new BufferedReader(new FileReader(filename));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

                if (line.startsWith("RCN"))
                {
                    String str[] = line.split("\\s+");
                    if (str.length < 3)
                    {
                        String err = "Incomplete reaction compatibility data.";
                        throw new DENOPTIMException(err + " " + filename);
                    }

                    // to account for multiple compatibilities
                    String strRcn[] = str[2].split(",");
                    for (int i=0; i<strRcn.length; i++)
		    {
                        strRcn[i] = strRcn[i].trim();

		        if (rcCompMap.containsKey(str[1]))
		        {
			    rcCompMap.get(str[1]).add(strRcn[i]);
		        }
		        else
		        {
			    ArrayList<String> rccomp = new ArrayList<String>();
			    rccomp.add(strRcn[i]);
                            rcCompMap.put(str[1],rccomp);
		        }

                        if (rcCompMap.containsKey(strRcn[i]))
                        {
                            rcCompMap.get(strRcn[i]).add(str[1]);
                        }
                        else
                        {
                            ArrayList<String> rccomp = new ArrayList<String>();
                            rccomp.add(str[1]);
                            rcCompMap.put(strRcn[i],rccomp);
                        }
		    }
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
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }

        if (rcCompMap.isEmpty())
        {
            String err = "No reaction compatibility data found in file: ";
            throw new DENOPTIMException(err + " " + filename);
        }
    }

//------------------------------------------------------------------------------

    public static ArrayList<IAtomContainer> readMoleculeData(String filename)
                                                        throws DENOPTIMException
    {
        ArrayList<IAtomContainer> mols;
        // check file extension
        if (GenUtils.getFileExtension(filename).
                                    compareToIgnoreCase(".smi") == 0)
        {
            throw new DENOPTIMException("Fragment files in SMILES format not supported.");
        }
        else if (GenUtils.getFileExtension(filename).
                                    compareToIgnoreCase(".sdf") == 0)
        {
            mols = DenoptimIO.readSDFFile(filename);
        }
        // process everything else as a text file with links to individual molecules
        else
        {
            mols = DenoptimIO.readTxtFile(filename);
        }
        return mols;
    }

//------------------------------------------------------------------------------

    /**
     * Writes a PNG representation of the molecule
     * @param mol the molecule
     * @param filename output file
     * @throws DENOPTIMException
     */

    public static void moleculeToPNG(IAtomContainer mol, String filename)
                                                        throws DENOPTIMException
    {
        IAtomContainer iac = null;
        if (!GeometryTools.has2DCoordinates(mol))
        {
            iac = DENOPTIMMoleculeUtils.generate2DCoordinates(mol);
        }
        else
        {
            iac = mol;
        }

        if (iac == null)
        {
            throw new DENOPTIMException("Failed to generate 2D coordinates.");
        }

        try
        {
            int WIDTH = 500;
            int HEIGHT = 500;
            // the draw area and the image should be the same size
            Rectangle drawArea = new Rectangle(WIDTH, HEIGHT);
            Image image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);

            // generators make the image elements
            ArrayList<IGenerator<IAtomContainer>> generators = new ArrayList<>();
            generators.add(new BasicSceneGenerator());
            generators.add(new BasicBondGenerator());
            generators.add(new BasicAtomGenerator());


            GeometryTools.translateAllPositive(iac);

            // the renderer needs to have a toolkit-specific font manager
            AtomContainerRenderer renderer =
                    new AtomContainerRenderer(generators, new AWTFontManager());

            RendererModel model = renderer.getRenderer2DModel();
            model.set(BasicSceneGenerator.UseAntiAliasing.class, true);
            //model.set(BasicAtomGenerator.KekuleStructure.class, true);
            model.set(BasicBondGenerator.BondWidth.class, 2.0);
            model.set(BasicAtomGenerator.ColorByType.class, true);
            model.set(BasicAtomGenerator.ShowExplicitHydrogens.class, false);
            model.getParameter(BasicSceneGenerator.FitToScreen.class).setValue(Boolean.TRUE);


            // the call to 'setup' only needs to be done on the first paint
            renderer.setup(iac, drawArea);

            // paint the background
            Graphics2D g2 = (Graphics2D)image.getGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
		                     RenderingHints.VALUE_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillRect(0, 0, WIDTH, HEIGHT);


            // the paint method also needs a toolkit-specific renderer
            renderer.paint(iac, new AWTDrawVisitor(g2),
                    new Rectangle2D.Double(0, 0, WIDTH, HEIGHT), true);

            ImageIO.write((RenderedImage)image, "PNG", new File(filename));
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Write the molecule in V3000 format.
     * @param outfile
     * @param mol
     * @throws Exception
     */

    @SuppressWarnings("ConvertToTryWithResources")
    private static void writeV3000File(String outfile, IAtomContainer mol)
                                                        throws DENOPTIMException
    {
        StringBuilder sb = new StringBuilder(1024);

        String title = (String) mol.getProperty(CDKConstants.TITLE);
        if (title == null)
            title = "";
        if(title.length() > 80)
            title=title.substring(0, 80);
        sb.append(title).append("\n");

    	sb.append("  CDK     ").append(new SimpleDateFormat("MMddyyHHmm").
                                        format(System.currentTimeMillis()));
        sb.append("\n\n");

        sb.append("  0  0  0     0  0            999 V3000\n");

        sb.append("M  V30 BEGIN CTAB\n");
        sb.append("M  V30 COUNTS ").append(mol.getAtomCount()).append(" ").
                append(mol.getBondCount()).append(" 0 0 0\n");
        sb.append("M  V30 BEGIN ATOM\n");
        for (int f = 0; f < mol.getAtomCount(); f++)
        {
            IAtom atom = mol.getAtom(f);
            sb.append("M  V30 ").append((f+1)).append(" ").append(atom.getSymbol()).
                    append(" ").append(atom.getPoint3d().x).append(" ").
                    append(atom.getPoint3d().y).append(" ").
                    append(atom.getPoint3d().z).append(" ").append("0");
            sb.append("\n");
        }
        sb.append("M  V30 END ATOM\n");
        sb.append("M  V30 BEGIN BOND\n");

        Iterator<IBond> bonds = mol.bonds().iterator();
        int f = 0;
        while (bonds.hasNext())
        {
            IBond bond = bonds.next();
            int bondType = bond.getOrder().numeric();
            String bndAtoms = "";
            if (bond.getStereo() == IBond.Stereo.UP_INVERTED ||
                        bond.getStereo() == IBond.Stereo.DOWN_INVERTED ||
                        bond.getStereo() == IBond.Stereo.UP_OR_DOWN_INVERTED)
            {
                // turn around atom coding to correct for inv stereo
                bndAtoms = mol.getAtomNumber(bond.getAtom(1)) + 1 + " ";
                bndAtoms += mol.getAtomNumber(bond.getAtom(0)) + 1;
            }
            else
            {
                bndAtoms = mol.getAtomNumber(bond.getAtom(0)) + 1 + " ";
                bndAtoms += mol.getAtomNumber(bond.getAtom(1)) + 1;
            }

//            String stereo = "";
//            switch(bond.getStereo())
//            {
//                case UP:
//                    stereo += "1";
//                    break;
//       		case UP_INVERTED:
//                    stereo += "1";
//                    break;
//                case DOWN:
//                    stereo += "6";
//                    break;
//                case DOWN_INVERTED:
//                    stereo += "6";
//                    break;
//                case UP_OR_DOWN:
//                    stereo += "4";
//                    break;
//                case UP_OR_DOWN_INVERTED:
//                    stereo += "4";
//                    break;
//                case E_OR_Z:
//                    stereo += "3";
//                    break;
//                default:
//                    stereo += "0";
//            }

            sb.append("M  V30 ").append((f+1)).append(" ").append(bondType).
                    append(" ").append(bndAtoms).append("\n");
            f = f + 1;
        }

        sb.append("M  V30 END BOND\n");
        sb.append("M  V30 END CTAB\n");
        sb.append("M  END\n\n");

        Map<Object,Object> sdFields = mol.getProperties();
        if(sdFields != null)
        {
            for (Object propKey : sdFields.keySet())
            {
                if (!cdkInternalProperties.contains((String) propKey))
                {
                    sb.append("> <").append(propKey).append(">");
                    sb.append("\n");
                    sb.append("").append(sdFields.get(propKey));
                    sb.append("\n\n");
                }
            }
        }


        sb.append("$$$$\n");

        //System.err.println(sb.toString());

        try
        {

            FileWriter fw = new FileWriter(outfile);
            fw.write(sb.toString());
            fw.close();
        }
        catch (IOException ioe)
        {
            throw new DENOPTIMException(ioe);
        }
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of graph editing tasks from a text file
     * @param fileName the pathname of the file to read
     * @return the list of editing tasks
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraphEdit> readDENOPTIMGraphEditFromFile(
                                                                String fileName)
                                                        throws DENOPTIMException
    {
        ArrayList<DENOPTIMGraphEdit> lst = new ArrayList<DENOPTIMGraphEdit>();
        BufferedReader br = null;
        String line = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

                DENOPTIMGraphEdit graphEdit;
                try
                {
		    graphEdit = new DENOPTIMGraphEdit(line.trim());
                }
                catch (Throwable t)
                {
                    String msg = "Cannot convert string to DENOPTIMGraphEdit. "
                                 + "Check line '" + line.trim() + "'";
                    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg,t);
                }
                lst.add(graphEdit);
            }
        }
        catch (IOException ioe)
        {
            String msg = "Cannot read file " + fileName;
            DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg,ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
	return lst;
    }

//------------------------------------------------------------------------------

    /**
     * Reads a list of <code>DENOPTIMGraph</code>s from a text file
     * @param fileName the pathname of the file to read
     * @return the list of graphs
     * @throws DENOPTIMException
     */
    public static ArrayList<DENOPTIMGraph> readDENOPTIMGraphsFromFile(
								String fileName)
							throws DENOPTIMException
    {
	ArrayList<DENOPTIMGraph> lstGraphs = new ArrayList<DENOPTIMGraph>();
        BufferedReader br = null;
        String line = null;
        try
        {
            br = new BufferedReader(new FileReader(fileName));
            while ((line = br.readLine()) != null)
            {
                if (line.trim().length() == 0)
                {
                    continue;
                }

                if (line.startsWith("#"))
                {
                    continue;
                }

		DENOPTIMGraph graph;
		try
		{
		    graph = GraphConversionTool.getGraphFromString(line.trim());
		}
		catch (Throwable t)
		{
		    String msg = "Cannot convert string to DENOPTIMGraph. "
			         + "Check line '" + line.trim() + "'";
		    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
                    throw new DENOPTIMException(msg,t);
		}
		lstGraphs.add(graph);
	    }
	}
        catch (IOException ioe)
        {
	    String msg = "Cannot read file " + fileName;
	    DENOPTIMLogger.appLogger.log(Level.SEVERE, msg);
            throw new DENOPTIMException(msg,ioe);
        }
        finally
        {
            try
            {
                if (br != null)
                {
                    br.close();
                }
            }
            catch (IOException ioe)
            {
                throw new DENOPTIMException(ioe);
            }
        }
	return lstGraphs;
    }

//------------------------------------------------------------------------------

}

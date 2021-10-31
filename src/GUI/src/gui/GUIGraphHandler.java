/*
 *   DENOPTIM
 *   Copyright (C) 2020 Marco Foscato <marco.foscato@uib.no>
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

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JSpinner.DefaultEditor;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.io.FilenameUtils;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObjectBuilder;
import org.openscience.cdk.silent.SilentChemObjectBuilder;

import denoptim.constants.DENOPTIMConstants;
import denoptim.exception.DENOPTIMException;
import denoptim.fragspace.FragmentSpace;
import denoptim.fragspace.IdFragmentAndAP;
import denoptim.io.DenoptimIO;
import denoptim.io.FileAndFormat;
import denoptim.io.FileFormat;
import denoptim.io.UndetectedFileFormatException;
import denoptim.molecule.APClass;
import denoptim.molecule.DENOPTIMAttachmentPoint;
import denoptim.molecule.DENOPTIMEdge;
import denoptim.molecule.DENOPTIMEdge.BondType;
import denoptim.molecule.DENOPTIMGraph;
import denoptim.molecule.DENOPTIMRing;
import denoptim.molecule.DENOPTIMVertex;
import denoptim.molecule.EmptyVertex;
import denoptim.molecule.DENOPTIMVertex.BBType;
import denoptim.utils.DENOPTIMMoleculeUtils;
import edu.uci.ics.jung.visualization.control.ModalGraphMouse;
import gui.GraphViewerPanel2.LabelType;


/**
 * A panel that understands DENOPTIM graphs and allows to create and edit
 * them.
 * 
 * @author Marco Foscato
 */

public class GUIGraphHandler extends GUICardPanel implements ILoadFragSpace
{
	/**
	 * Version UID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Unique identified for instances of this handler
	 */
	public static AtomicInteger graphHandlerTabUID = 
			new AtomicInteger(1);
	
	/**
	 * The currently loaded list of graphs
	 */
	protected ArrayList<DENOPTIMGraph> dnGraphLibrary =
			new ArrayList<DENOPTIMGraph>();
	
	/**
	 * The currently loaded list of molecular representations 
	 * of the graphs
	 */
	private ArrayList<IAtomContainer> molLibrary =
			new ArrayList<IAtomContainer>();
	
	/**
	 * The unsaved version of the currently loaded graph
	 */
	private DENOPTIMGraph dnGraph;
	
	/**
	 * Unique identified for graphs built here
	 */
	public static AtomicInteger graphUID = new AtomicInteger(1);
	
	/**
	 * The index of the currently loaded dnGraph [0–(n-1)}
	 */
	private int currGrphIdx = 0;
	
	/**
	 * Flag signaling that loaded data has changes since last save
	 */
	private boolean unsavedChanges = false;
	
	/**
	 * Flag signaling that there is a fully defined fragment space
	 */
	private boolean hasFragSpace = FragmentSpace.isDefined();
	
	// The panel that hosts graph, vertex, and molecular viewers
	private GraphVertexMolViewerPanel visualPanel;
	
	// The panel hosting buttons for manipulation of graphs
	private JPanel graphCtrlPane;
	
	// The panel hosting buttons for navigation in the list of graphs
	private JPanel graphNavigPane;
	
	private JPanel pnlMouseMode;
	private JButton btnPickMode;
	private JButton btnMoveMode;
    
	private JButton btnAddGraph;
	private JButton btnGraphDel;
	
	private JButton btnOpenGraphs;
	
	private JSpinner graphNavigSpinner;
	private JLabel totalGraphsLabel;
	private final GraphSpinnerChangeEvent graphSpinnerListener = 
												new GraphSpinnerChangeEvent();
	
	private JPanel pnlEditVrtxBtns;
	private JButton btnAddLibVrtx;
    private JButton btnAddEmptyVrtx;
	private JButton btnDelSel;
	private JButton btnAddChord; 
	
	private JPanel pnlShowLabels;
	private JButton btnLabAPC;
    private JButton btnLabBT;
    private JButton btnLabBB;
	
	private JPanel pnlSaveEdits;
	private JButton btnSaveEdits;
	
	/**
	 * Subset of vertices for compatible building block selecting GUI.
	 * These vertices are clones of those in the loaded library,
	 * and are annotate with fragmentID and AP pointers meant to 
	 * facilitate a quick selection of compatible connections.
	 */
	private ArrayList<DENOPTIMVertex> compatVrtxs;
	
	/**
	 * Map converting fragIDs in fragment library to fragIDs in subset
	 * of compatible fragments
	 */
	private Map<Integer,Integer> genToLocIDMap;
	
	private boolean updateMolViewer = false;
	
	private static final  IChemObjectBuilder builder = 
            SilentChemObjectBuilder.getInstance();
	
	private boolean painted;

//-----------------------------------------------------------------------------
	
	@Override
	public void paint(Graphics g) {
	    super.paint(g);

	    if (!painted) {
	        painted = true;
	        visualPanel.setDefaultDividerLocation();
	    }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Constructor
	 */
	public GUIGraphHandler(GUIMainPanel mainPanel)
	{
		super(mainPanel, "Graph Handler #" 
					+ graphHandlerTabUID.getAndIncrement());
		super.setLayout(new BorderLayout());
		initialize();
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Initialize the panel and add buttons.
	 */
	private void initialize() 
	{	
		// BorderLayout is needed to allow dynamic resizing!
		this.setLayout(new BorderLayout()); 
		
		// This card structure includes center, east and south panels:
		// - (Center) where graphs/vertices/molecules are visualised
		// - (East) graph controls
		// - (South) general controls (load, save, close)
		
		visualPanel = new GraphVertexMolViewerPanel();
		this.add(visualPanel,BorderLayout.CENTER);
       
		// General panel on the right: it containing all controls
        graphCtrlPane = new JPanel();
        graphCtrlPane.setVisible(true);
        graphCtrlPane.setLayout(new BoxLayout(graphCtrlPane, 
        		SwingConstants.VERTICAL));

        // Controls to navigate the list of dnGraphs
        graphNavigPane = new JPanel();
        JLabel navigationLabel1 = new JLabel("Graph # ");
        JLabel navigationLabel2 = new JLabel("Current library size: ");
        totalGraphsLabel = new JLabel("0");
        
		graphNavigSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
		graphNavigSpinner.setToolTipText("Move to graph number # in the "
				+ "currently loaded library.");
		graphNavigSpinner.setMaximumSize(new Dimension(75,20));
		graphNavigSpinner.addChangeListener(graphSpinnerListener);
        
		btnAddGraph = new JButton("Add");
		btnAddGraph.setToolTipText("Append a graph to the currently loaded "
				+ "list of graphs.");
		btnAddGraph.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] options = new String[]{"Build", "File", "Cancel"};
				int res = JOptionPane.showOptionDialog(null,
		                "<html>Please choose wherther to start creations "
		                + "of a new graph (Build), "
		                + "or import graph from file.</html>",
		                "Specify source of new graph",
		                JOptionPane.DEFAULT_OPTION,
		                JOptionPane.QUESTION_MESSAGE,
		                UIManager.getIcon("OptionPane.warningIcon"),
		                options,
		                options[2]);
				switch (res)
				{
					case 0:
						if (!hasFragSpace)
						{
							JOptionPane.showMessageDialog(null,
					                "<html>No fragment space is currently loaded!<br>"
					                + "You must load a fragment space to build graphs that<br>"
					                + "contain molecular frgments. <br>"
					                + "However, without a fragment space, you can still build<br>"
					                + "graphs made of empty vertexes (i.e., vertexes contain<br>"
					                + "no atoms, but only attachment points).</html>",
					                "WARNING",
					                JOptionPane.WARNING_MESSAGE,
					                UIManager.getIcon("OptionPane.warningIcon"));
						}
						try
                        {
                            startGraphFromFragSpaceOrCreationOfEmptyVertex();
                        } catch (DENOPTIMException e1)
                        {
                            e1.printStackTrace();
                            JOptionPane.showMessageDialog(null,
                                    "<html>Could not create graph!<br>"
                                    + "Exception thrown when starting the "
                                    + "construction<br>"
                                    + "of a new graph. Please, report this to "
                                    + "the authors.</html>",
                                    "Error",
                                    JOptionPane.ERROR_MESSAGE,
                                    UIManager.getIcon("OptionPane.errorIcon"));
                            return;
                        }
						break;
					
					case 1:
						File inFile = GUIFileOpener.pickFileWithGraph(
						        btnAddGraph);
						if (inFile == null 
								|| inFile.getAbsolutePath().equals(""))
						{
							return;
						}
						appendGraphsFromFile(inFile);
						break;
				}
			}
		});
		btnGraphDel = new JButton("Remove");
		btnGraphDel.setToolTipText("<html>Remove the present graph from the "
				+ "library.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!</html>");
		btnGraphDel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					removeCurrentDnGraph();
				} catch (DENOPTIMException e1) {
					System.out.println("Exception while removing the current "
							+ "graph:");
					e1.printStackTrace();
				}
			}
		});
		
        GroupLayout lyoAddDetGraphs = new GroupLayout(graphNavigPane);
        graphNavigPane.setLayout(lyoAddDetGraphs);
        lyoAddDetGraphs.setAutoCreateGaps(true);
        lyoAddDetGraphs.setAutoCreateContainerGaps(true);
        lyoAddDetGraphs.setHorizontalGroup(lyoAddDetGraphs.createParallelGroup(
                        		GroupLayout.Alignment.CENTER)
                        .addGroup(lyoAddDetGraphs.createSequentialGroup()
                                        .addComponent(navigationLabel1)
                                        .addComponent(graphNavigSpinner))
                        .addGroup(lyoAddDetGraphs.createSequentialGroup()
                                        .addComponent(navigationLabel2)
                                        .addComponent(totalGraphsLabel))
                        .addGroup(lyoAddDetGraphs.createSequentialGroup()
                                        .addComponent(btnAddGraph)
                                        .addComponent(btnGraphDel)));
        lyoAddDetGraphs.setVerticalGroup(lyoAddDetGraphs.createSequentialGroup()
                        .addGroup(lyoAddDetGraphs.createParallelGroup(
                        		GroupLayout.Alignment.CENTER)
                                        .addComponent(navigationLabel1)
                                        .addComponent(graphNavigSpinner))
                        .addGroup(lyoAddDetGraphs.createParallelGroup()
                                        .addComponent(navigationLabel2)
                                        .addComponent(totalGraphsLabel))
                        .addGroup(lyoAddDetGraphs.createParallelGroup()
                                        .addComponent(btnAddGraph)
                                        .addComponent(btnGraphDel)));
		graphCtrlPane.add(graphNavigPane);
		
		
		graphCtrlPane.add(new JSeparator());
		
		
		JLabel mouseModeLab = new JLabel("Mouse mode:");
		btnPickMode = new JButton("Pick");
		btnPickMode.setToolTipText("Make the mouse select vertex on click.");
		btnPickMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                visualPanel.setMouseMode(ModalGraphMouse.Mode.PICKING);
            }
        });
        btnMoveMode = new JButton("Move");
        btnMoveMode.setToolTipText("Makes mouse move transform the graph view.");
        btnMoveMode.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                visualPanel.setMouseMode(ModalGraphMouse.Mode.TRANSFORMING);
            }
        });
        pnlMouseMode = new JPanel();
        GroupLayout lyoMouseModeLayout = new GroupLayout(pnlMouseMode);
        pnlMouseMode.setLayout(lyoMouseModeLayout);
        lyoMouseModeLayout.setAutoCreateGaps(true);
        lyoMouseModeLayout.setAutoCreateContainerGaps(true);
        lyoMouseModeLayout.setHorizontalGroup(lyoMouseModeLayout.createParallelGroup(
                GroupLayout.Alignment.CENTER)
                .addComponent(mouseModeLab)
                .addGroup(lyoMouseModeLayout.createSequentialGroup()
                        .addComponent(btnMoveMode)
                        .addComponent(btnPickMode)));
        lyoMouseModeLayout.setVerticalGroup(lyoMouseModeLayout.createSequentialGroup()
                .addComponent(mouseModeLab)
                .addGroup(lyoMouseModeLayout.createParallelGroup()
                        .addComponent(btnMoveMode)
                        .addComponent(btnPickMode)));
        graphCtrlPane.add(pnlMouseMode);
        
		
		graphCtrlPane.add(new JSeparator());
		
		
		// Controls to alter the presently loaded graph (if any)
		pnlEditVrtxBtns = new JPanel();
		JLabel edtVertxsLab = new JLabel("Edit Graph:");
		btnAddLibVrtx = new JButton("Add Vertex from Library");
		btnAddLibVrtx.setToolTipText("<html>Choose a vertex from the "
		        + "loaded fragment space and<br>"
		        + "append it to the "
				+ "attachment point/s selected in the current graph.<html>");
		btnAddLibVrtx.setEnabled(false);
		btnAddLibVrtx.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {	
				if (!hasFragSpace)
				{
					JOptionPane.showMessageDialog(null,
			                "<html>No fragment space is currently "
			                + "loaded!<br>"
			                + "You must first load a fragment space before "
			                + "trying to build graphs.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				ArrayList<DENOPTIMAttachmentPoint> selAps = 
				        visualPanel.getAPsSelectedInViewer();				
				if (selAps.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
			                "<html>No attachment point selected!<br>"
			                + "Drag the mouse to select APs.<br> "
			                + "Click again to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				extendGraphFromFragSpace(selAps);
				
				// Update viewer
				visualPanel.loadDnGraphToViewer(dnGraph,true,hasFragSpace);
				
				// Protect edited system
		        unsavedChanges = true;
		        protectEditedSystem();

				// The molecular representation is updated when we save changes
		        visualPanel.renderMolVieverToNeedUpdate();
		        updateMolViewer = true;
			}
		});
		
		
	    btnAddEmptyVrtx = new JButton("Add Empty Vertex");
        btnAddEmptyVrtx.setToolTipText("<html>Creates an empty vertex "
                + "(i.e., a vertex with attachment points<br>"
                + "and properties, but that contains no atoms) and appends it "
                + "to<br>"
                + "the attachment points selected in the current graph."
                + "<html>");
        btnAddEmptyVrtx.setEnabled(true);
        btnAddEmptyVrtx.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                ArrayList<DENOPTIMAttachmentPoint> selAps = 
                        new ArrayList<DENOPTIMAttachmentPoint> ();
                if (dnGraph != null)
                {
                    selAps = visualPanel.getAPsSelectedInViewer(); 
                    if (selAps.size() == 0)
                    {
                        //This would overwrite the current graph, so no-go!
                        JOptionPane.showMessageDialog(null,
                                "<html>No attachment point selected!<br>"
                                + "Drag the mouse to select APs.<br> "
                                + "Click again to unselect.</html>",
                                "Error",
                                JOptionPane.ERROR_MESSAGE,
                                UIManager.getIcon("OptionPane.errorIcon"));
                        return;
                    }
                }
                startGraphFromCreationOfEmptyVertex(selAps);
            }
        });
        
		
		btnDelSel = new JButton("Remove Vertex");
		btnDelSel.setToolTipText("<html>Removes the selected vertices from "
				+ "the system.<br><br><b>WARNING:</b> this action cannot be "
				+ "undone!</html>");
		btnDelSel.setEnabled(false);
		btnDelSel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ArrayList<DENOPTIMVertex> selVrtx = 
				        visualPanel.getSelectedNodesInViewer();
				if (selVrtx.size() == 0)
				{
					JOptionPane.showMessageDialog(null,
							"<html>No vertex selected! Drag the "
			                + "mouse to select vertices."
					        + "<br>Click on background to unselect.</html>",
			                "Error",
			                JOptionPane.ERROR_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				
				// Prevent removal of the scaffold
				for (DENOPTIMVertex v : selVrtx)
				{
					if (v.getLevel() == -1)
					{
						JOptionPane.showMessageDialog(null,
								"<html>The scaffold cannot be removed."
						        + "</html>",
				                "Error",
				                JOptionPane.ERROR_MESSAGE,
				                UIManager.getIcon("OptionPane.errorIcon"));
						return;
					}
				}
				
				for (DENOPTIMVertex v : selVrtx)
				{
					dnGraph.removeVertex(v);
				}
				
				// Update viewer
                visualPanel.loadDnGraphToViewer(dnGraph,true,hasFragSpace);
				
		        // Protect the temporary "dnGraph" obj
		        unsavedChanges = true;
		        protectEditedSystem();
			
		        // The molecular representation is updated when we save changes
                visualPanel.renderMolVieverToNeedUpdate();
                updateMolViewer = true;
			}
		});
		
	    // Controls to add chord (ring closing edge)
        btnAddChord = new JButton("Add Chord");
        btnAddChord.setToolTipText("<html>Add a ring-closing edge between two "
                + "selected vertices.<html>");
        btnAddChord.setEnabled(false);
        btnAddChord.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {    
                ArrayList<DENOPTIMVertex> selVrtxs = 
                        visualPanel.getSelectedNodesInViewer();               
                if (selVrtxs.size() != 2)
                {
                    JOptionPane.showMessageDialog(null,
                            "<html>Number of selected vertices: "
                            + selVrtxs.size() + " <br>"
                            + "Please, drag the mouse and "
                            + "select only two vertices!<br> "
                            + "Click again to unselect.</html>",
                            "Error",
                            JOptionPane.ERROR_MESSAGE,
                            UIManager.getIcon("OptionPane.errorIcon"));
                    return;
                }
                addChordOnGraph(selVrtxs);
                
                // Update viewer
                visualPanel.loadDnGraphToViewer(dnGraph,true,hasFragSpace);
                
                // Protect edited system
                unsavedChanges = true;
                protectEditedSystem();

                // The molecular representation is updated when we save changes
                visualPanel.renderMolVieverToNeedUpdate();
                updateMolViewer = true;
            }
        });
        
        
		GroupLayout lyoEditVertxs = new GroupLayout(pnlEditVrtxBtns);
		pnlEditVrtxBtns.setLayout(lyoEditVertxs);
		lyoEditVertxs.setAutoCreateGaps(true);
		lyoEditVertxs.setAutoCreateContainerGaps(true);
		lyoEditVertxs.setHorizontalGroup(lyoEditVertxs.createParallelGroup(
				GroupLayout.Alignment.CENTER)
				.addComponent(edtVertxsLab)
				.addComponent(btnAddLibVrtx)
                .addComponent(btnAddEmptyVrtx)
				.addComponent(btnDelSel)
				.addComponent(btnAddChord));
		lyoEditVertxs.setVerticalGroup(lyoEditVertxs.createSequentialGroup()
				.addComponent(edtVertxsLab)
				.addComponent(btnAddLibVrtx)
                .addComponent(btnAddEmptyVrtx)
				.addComponent(btnDelSel)
				.addComponent(btnAddChord));
		graphCtrlPane.add(pnlEditVrtxBtns);
		
		graphCtrlPane.add(new JSeparator());
		
		// Controls of displayed attributes
		pnlShowLabels = new JPanel();
		JLabel lblShowHideLabels = new JLabel("Show/Hide labels:");
		
		btnLabAPC = new JButton("APClass");
		btnLabAPC.addActionListener(new showHideLabelsListener(LabelType.APC));
		btnLabAPC.setEnabled(false);
        btnLabAPC.setToolTipText("Show/Hide attachment point class labels.");
        
        btnLabBT = new JButton("Bnd Typ");
        btnLabBT.addActionListener(new showHideLabelsListener(LabelType.BT));
        btnLabBT.setEnabled(false);
        btnLabBT.setToolTipText("Show/Hide bond type ID labels.");
        
        btnLabBB = new JButton("BB ID");
        btnLabBB.addActionListener(new showHideLabelsListener(LabelType.BBID));
        btnLabBB.setEnabled(false);
        btnLabBB.setToolTipText("Show/Hide building block ID labels.");
		
        GroupLayout lyoShowAttr = new GroupLayout(pnlShowLabels);
        pnlShowLabels.setLayout(lyoShowAttr);
        lyoShowAttr.setAutoCreateGaps(true);
        lyoShowAttr.setAutoCreateContainerGaps(true);
        lyoShowAttr.setHorizontalGroup(lyoShowAttr.createParallelGroup(
                        GroupLayout.Alignment.CENTER)
                        .addComponent(lblShowHideLabels)
                        .addComponent(btnLabAPC)
                        .addComponent(btnLabBT)
                        .addComponent(btnLabBB));
        lyoShowAttr.setVerticalGroup(lyoShowAttr.createSequentialGroup()
		                .addComponent(lblShowHideLabels)
                        .addComponent(btnLabAPC)
                        .addComponent(btnLabBT)
                        .addComponent(btnLabBB));
        graphCtrlPane.add(pnlShowLabels);
        
        graphCtrlPane.add(new JSeparator());
		
		// Control for unsaved changes
        pnlSaveEdits = new JPanel();
        btnSaveEdits = new JButton("Save Changes");
        btnSaveEdits.setForeground(Color.RED);
        btnSaveEdits.setEnabled(false);
        btnSaveEdits.setToolTipText("<html>Save the current graph replacing"
        	+ " <br>the original one in the currently loaded library.</html>");
        btnSaveEdits.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                	saveUnsavedChanges();
                }
        });
        pnlSaveEdits.add(btnSaveEdits);
        graphCtrlPane.add(pnlSaveEdits);
		this.add(graphCtrlPane,BorderLayout.EAST);
		
		// Panel with buttons to the bottom of the frame
		ButtonsBar commandsPane = new ButtonsBar();
		super.add(commandsPane, BorderLayout.SOUTH);
		
		btnOpenGraphs = new JButton("Load Library of Graphs");
		btnOpenGraphs.setToolTipText("Reads graphs or structures from file.");
		btnOpenGraphs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File inFile = GUIFileOpener.pickFile(btnOpenGraphs);
				if (inFile == null || inFile.getAbsolutePath().equals(""))
				{
					return;
				}
				importGraphsFromFile(inFile);
			}
		});
		commandsPane.add(btnOpenGraphs);
		
		JButton btnSaveFrags = new JButton("Save Library of Graphs");
		btnSaveFrags.setToolTipText("Write all graphs to a file.");
		btnSaveFrags.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				FileAndFormat fileAndFormat = 
				        GUIFileSaver.pickFileForSavingGraphs(btnSaveFrags);
				if (fileAndFormat == null)
				{
					return;
				}
				File outFile = fileAndFormat.file;
				try
				{
					outFile = DenoptimIO.writeGraphsToFile(outFile,
					        fileAndFormat.format, dnGraphLibrary);
				}
				catch (Exception ex)
				{
					JOptionPane.showMessageDialog(btnSaveFrags,
			                "Could not write to '" + outFile + "'!.",
			                "Error",
			                JOptionPane.PLAIN_MESSAGE,
			                UIManager.getIcon("OptionPane.errorIcon"));
					return;
				}
				deprotectEditedSystem();
				unsavedChanges = false;
				DenoptimIO.addToRecentFiles(outFile, fileAndFormat.format);
			}
		});
		commandsPane.add(btnSaveFrags);

		JButton btnCanc = new JButton("Close Tab");
		btnCanc.setToolTipText("Closes this graph handler.");
		btnCanc.addActionListener(new removeCardActionListener(this));
		commandsPane.add(btnCanc);
		
		JButton btnHelp = new JButton("?");
		btnHelp.setToolTipText("<html>Hover over the buttons and fields "
                    + "to get a tip.</html>");
		btnHelp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String txt = "<html><body width='%1s'>"
						+ "<p>This tab allows to create, inspect, and edit "
						+ "DENOPTIM's graph representation of chemical "
						+ "objects (i.e., DENOPTIMGraph).</p>"
						+ "<br>"
						+ "<p>In general, you can hover over any button or"
						+ "viewer to get a tip on its usage.</p>"
						+ "<br>"
						+ "<p>DENOPTIMGraphs is drawn in the "
						+ "central panel (i.e., the graph viewer). "
						+ "Each vertex is shown as a rounded square, and each "
						+ "edge as an arrow (or a line, for undirected edges)."
						+ " The color code identified the type of fragment "
						+ "contained in a node:<ul>"
						+ "<li>red for the scaffold,</li>"
						+ "<li>orange for ring-closing vertices,</li>"
						+ "<li>green for capping groups,</li>"
						+ "<li>blue for standard fragments.</li>"
						+ "</ul></p>"
						+ "<p>If the loaded DENOPTIMGraph is associated with "
						+ "a chemical structure, the latter is shown in the "
						+ "molecular viewer (bottom-left panel).</p>"
						+ "<p>The molecular fragment contained in a node is "
						+ "shown in the fragment viewer (top-left panel) upon "
						+ "clicking on that node in the graph viewer. "
						+ "A fragment space must be loaded in order to "
						+ "inspect the chemical structure of "
						+ "fragments, and also to build new DENOPTIMGraphs"
						+ " from manually.</p>"
						+ "<br>"
						+ "<p><b>Control the graph view</b></p>"
						+ "<p>Move the mouse up/down while holding the "
						+ "<code>ctrl</code> key to zoom in/out.</p>"
						+ "<p>Move the mouse left/right while holding the "
						+ "<code>Alt</code> key to pan the view.</p>"
						+ "<br>"
						+ "<p><b>Control the fragment and molecular views</b>"
						+ "</p>"
						+ "<p>Right-click on the Jmol viewer will open the "
						+ "Jmol menu. However, any change on the molecular "
						+ "object will not be saved in the "
						+ "fragment or in the chemical structure associated"
						+ " with the graph.</p></html>";
				JOptionPane.showMessageDialog(null, 
						String.format(txt, 450),
	                    "Tips",
	                    JOptionPane.PLAIN_MESSAGE);
			}
		});
		commandsPane.add(btnHelp);
	}
	
//-----------------------------------------------------------------------------
	
	private class showHideLabelsListener implements ActionListener
	{
	    private LabelType labTyp;
	    private Map<LabelType,Boolean> lastIteration = new HashMap<>();
	    
	    public showHideLabelsListener(LabelType labTyp) 
	    {   
	        this.labTyp = labTyp;
	        for (LabelType lt : LabelType.values())
	            lastIteration.put(lt, false);
	    }
	    
        @Override
        public void actionPerformed(ActionEvent e) {
            if (visualPanel.hasSelectedNodes())
            {
                boolean show = !lastIteration.get(labTyp);
                visualPanel.alterLabels(labTyp, show);
                lastIteration.put(labTyp,show);
            }
            else
            {
                JOptionPane.showMessageDialog(null,
                        "<html>No elements selected! Drag the "
                        + "mouse to select elements."
                        + "<br>Click on background to unselect.</html>",
                        "Error",
                        JOptionPane.ERROR_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
            }
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void enableGraphDependentButtons(boolean enable)
	{
		btnAddLibVrtx.setEnabled(enable);
        //btnAddEmptyVrtx.setEnabled(enable); //Always enabled
		btnDelSel.setEnabled(enable);
		btnAddChord.setEnabled(enable);
		btnLabAPC.setEnabled(enable);
        btnLabBT.setEnabled(enable);
        btnLabBB.setEnabled(enable);
	}
	
//-----------------------------------------------------------------------------
	
	private void startGraphFromCreationOfEmptyVertex(
	        ArrayList<DENOPTIMAttachmentPoint> selAps)
	{   
        GUIEmptyVertexMaker makeEmptyVertexDialog = 
                new GUIEmptyVertexMaker();
        makeEmptyVertexDialog.pack();
        Object evObj = makeEmptyVertexDialog.showDialog();
        if (evObj == null)
        {
            return;
        }
        DENOPTIMVertex ev = (EmptyVertex) evObj;
        ArrayList<DENOPTIMVertex> lst = new ArrayList<DENOPTIMVertex>(1);
        lst.add(ev);
        GUIVertexSelector fragSelector = new GUIVertexSelector(lst);
        fragSelector.ctrlPane.setVisible(false);
        if (selAps.size() == 0)
        {
            fragSelector.btnDone.setText("Confirm");
            fragSelector.setRequireApSelection(false); 
        } else {
            fragSelector.btnDone.setText("Confirm Selected AP");
            fragSelector.setRequireApSelection(true); 
        }
        Object selected = fragSelector.showDialog();
        if (selected == null)
        {
            return;
        }
        
        @SuppressWarnings("unchecked")
        ArrayList<Integer> trgFragApId = 
            ((ArrayList<ArrayList<Integer>>)selected).get(0);
        int incomingAPId = trgFragApId.get(1);
                      
        if (selAps.size() == 0)
        {
            currGrphIdx = dnGraphLibrary.size();
            initializeCurrentGraph();
            ev.setLevel(-1);
            try
            {
                dnGraph.addVertex(ev);
            } catch (DENOPTIMException e1)
            {
                e1.printStackTrace();
                JOptionPane.showMessageDialog(null,"Could not make the "
                        + "new graph. " + e1.getMessage(),
                        "Error",
                        JOptionPane.PLAIN_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
                return;
            }
        } else {
            extendCurrentGraph(ev.getAP(incomingAPId), selAps);
        }
        
        // Update viewer
        visualPanel.loadDnGraphToViewer(dnGraph,true,hasFragSpace);
        enableGraphDependentButtons(true);
        
        // Protect edited system
        unsavedChanges = true;
        protectEditedSystem();

        // The molecular representation is updated when we save changes
        visualPanel.renderMolVieverToNeedUpdate();
        updateMolViewer = true;
	}

//-----------------------------------------------------------------------------

	/**
	 * Start the construction of a new graph from scratch
	 * @throws DENOPTIMException 
	 */
	private void startGraphFromFragSpaceOrCreationOfEmptyVertex() 
	        throws DENOPTIMException
	{
	    BBType rootType = BBType.SCAFFOLD;
	    String msg = "<html><body width='%1s'>"
                + "Please choose the type of building block to use as first "
                + "vertex of the graph.";
	    String[] options = null;
	    String defaultOpt = null;
	    if (hasFragSpace)
	    {
	        options = new String[]{"Scaffold", "Fragment", "EmptyVertex", 
	            "Cancel"};
	        defaultOpt = options[3];
	        msg = msg + "Use a scaffold if the graph is meant to "
	                + "represent a necessary portion of a candidate entity.</html>";
	    } else {
	        options = new String[]{"EmptyVertex", "Cancel"};
	        defaultOpt = options[1];
	    }
        int res = JOptionPane.showOptionDialog(null,String.format(msg,350),
                "Specify type of initial building block",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.warningIcon"),
                options,
                defaultOpt);
        if (!hasFragSpace)
        {
            res = res + 10;
        }
        
        ArrayList<DENOPTIMVertex> vrtxLib = new  ArrayList<DENOPTIMVertex>();
        switch (res)
        {
            case 0:
                rootType = BBType.SCAFFOLD;
                for (DENOPTIMVertex bb : FragmentSpace.getScaffoldLibrary())
                {
                    vrtxLib.add(bb.clone());
                }
                break;
                
            case 1:
                rootType = BBType.FRAGMENT;
                for (DENOPTIMVertex bb : FragmentSpace.getFragmentLibrary())
                {
                    vrtxLib.add(bb.clone());
                }
                break;
                
            case 10:
            case 2:
                // In this case we do not use the fragment space. So, all the 
                // index-based operations on the fragment space that are done 
                // after this 'switch' block make no sense. Instead, we use the 
                // same method called by the "Add Empty Vertex" button.
                ArrayList<DENOPTIMAttachmentPoint> selectedAPs = new ArrayList<>();
                startGraphFromCreationOfEmptyVertex(selectedAPs);
                return;
            
            case 11: 
            case 3:
                return;
        }
        if (vrtxLib.size() == 0)
		{
			JOptionPane.showMessageDialog(null,"No building blocks of the "
			        + "choosen type.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Select the scaffold
		GUIVertexSelector fragSelector = new GUIVertexSelector(vrtxLib);
		fragSelector.setRequireApSelection(false);
		Object selected = fragSelector.showDialog();
		if (selected == null)
		{
			return;
		}
		
		@SuppressWarnings("unchecked")
        ArrayList<Integer> trgFragApId = ((ArrayList<ArrayList<Integer>>)selected)
                .get(0);
        int scaffFragId = trgFragApId.get(0);
		
		// Create the new graph
		currGrphIdx = dnGraphLibrary.size();
		initializeCurrentGraph();
		
		// Create the node
		int firstBBId = 1;
		DENOPTIMVertex firstVertex = null;
        try
        {
            firstVertex = DENOPTIMVertex.newVertexFromLibrary(
                    firstBBId, scaffFragId, rootType);
        } catch (DENOPTIMException e)
        {
            JOptionPane.showMessageDialog(null,"Could not retrieve the "
                    + "requested building blocks. " + e.getMessage(),
                    "Error",
                    JOptionPane.PLAIN_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
            return;
        }

		firstVertex.setLevel(-1);
		dnGraph.addVertex(firstVertex);
		
		// Put the graph to the viewer
        visualPanel.loadDnGraphToViewer(dnGraph,false,hasFragSpace);
		enableGraphDependentButtons(true);
		unsavedChanges = true;
		updateMolViewer = true;
        protectEditedSystem();
	}
	
//-----------------------------------------------------------------------------
	
	private void initializeCurrentGraph()
	{
        dnGraph = new DENOPTIMGraph();
        dnGraph.setGraphId(graphUID.getAndIncrement());
        // Add new graph and corresponding mol representation (must exist)
        dnGraphLibrary.add(dnGraph);
        //NB: we add an empty molecular representation to keep the list
        // of graphs and that of mol.rep. in sync
        molLibrary.add(builder.newAtomContainer());
        visualPanel.clearMolecularViewer();
        updateGraphListSpinner();
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Edits the currently loaded graph by adding a chord involving the two
	 * selected vertices.
	 * @param rcvs the selected vertices. Must be two vertices.
	 */
	private void addChordOnGraph(ArrayList<DENOPTIMVertex> rcvs)
	{
        if (rcvs.size() != 2)
        {
            JOptionPane.showMessageDialog(null,
                    "<html>Number of selected vertices: "
                    + rcvs.size() + " <br>"
                    + "Please, drag the mouse and "
                    + "select only two vertices!<br> "
                    + "Click again to unselect.</html>",
                    "Error",
                    JOptionPane.ERROR_MESSAGE,
                    UIManager.getIcon("OptionPane.errorIcon"));
            return;
        }
        
        try
        {
            dnGraph.addRing(rcvs.get(0), rcvs.get(1));
        } catch (DENOPTIMException e)
        {
            BondType bt = BondType.UNDEFINED;
            dnGraph.addRing(rcvs.get(0), rcvs.get(1), bt);
        }
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Extends the current graph by appending a node to a specific free AP on 
	 * the growing graph. 
	 * This method will prompt a question on which incoming fragment to append 
	 * @param selAps attachment points on the growing graph.
	 */
	private void extendGraphFromFragSpace(
	        ArrayList<DENOPTIMAttachmentPoint> selAps)
	{
		// For extensions of existing graphs we need to know where to extend
		if (selAps.size() == 0)
		{
			JOptionPane.showMessageDialog(null,"No AP selected in the "
					+ "graph.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Create clones of fragments and put the into 'compatFrags'
		collectFragAndAPsCompatibleWithSelectedAPs(selAps);
		
		DENOPTIMVertex.BBType trgFrgType = DENOPTIMVertex.BBType.UNDEFINED;
		ArrayList<DENOPTIMVertex> vertxLib = new ArrayList<DENOPTIMVertex>();		
		String[] options = new String[]{"Any Vertex",
				"Compatible Vertices ("+compatVrtxs.size()+")",
				"Capping group"};
		int res = JOptionPane.showOptionDialog(null,
                "<html>Choose a subset of possible vertices:</html>",
                "Choose Vertex Subset",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                UIManager.getIcon("OptionPane.warningIcon"),
                options,
                options[0]);
		
		switch (res)
		{
			case 0:
			    ArrayList<DENOPTIMVertex> tmp = FragmentSpace.getFragmentLibrary();
				vertxLib = new  ArrayList<DENOPTIMVertex>();
		        for (DENOPTIMVertex bb : FragmentSpace.getFragmentLibrary())
		        {
		        	vertxLib.add(bb.clone());
		        }
				trgFrgType = DENOPTIMVertex.BBType.FRAGMENT;
				break;
				
			case 1:
				vertxLib = compatVrtxs;
				trgFrgType = DENOPTIMVertex.BBType.FRAGMENT;
				break;
				
			case 2:
				vertxLib = new ArrayList<DENOPTIMVertex>();
		        for (DENOPTIMVertex bb : FragmentSpace.getCappingLibrary())
		        {
		            vertxLib.add(bb.clone());
		        }
				trgFrgType = DENOPTIMVertex.BBType.CAP;
				break;
			default:
				return;
		}

		if (vertxLib.size() == 0)
		{
			JOptionPane.showMessageDialog(null,"No fragments in the library",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Select the incoming fragment and its AP to use
		GUIVertexSelector fragSelector = new GUIVertexSelector(vertxLib);
		fragSelector.setRequireApSelection(true);
		Object selected = fragSelector.showDialog();
		if (selected == null)
		{
			return;
		}
		ArrayList<Integer> trgFragApId = 
		        ((ArrayList<ArrayList<Integer>>)selected).get(0);
		DENOPTIMVertex chosenVrtx = vertxLib.get(trgFragApId.get(0));
		
		extendCurrentGraph(chosenVrtx.getAP(trgFragApId.get(1)),selAps);
	}
	
//-----------------------------------------------------------------------------
	
	private void extendCurrentGraph(DENOPTIMAttachmentPoint apOnIncomingVrtx,
            ArrayList<DENOPTIMAttachmentPoint> selAps)
	{   
        DENOPTIMVertex chosenVrtx = apOnIncomingVrtx.getOwner();
        if (chosenVrtx == null)
            return;
        
        int apIdOnIncVrtx = apOnIncomingVrtx.getIndexInOwner();
       
        for (int i=0; i<selAps.size(); i++)
        {
            DENOPTIMAttachmentPoint srcAp = selAps.get(i);
            DENOPTIMVertex trgVertex = chosenVrtx.clone();
            trgVertex.setVertexId(dnGraph.getMaxVertexId()+1);
            DENOPTIMAttachmentPoint trgAp = trgVertex.getAP(apIdOnIncVrtx);
            try
            {
                dnGraph.appendVertexOnAP(srcAp, trgAp);
            } catch (DENOPTIMException e) {
                JOptionPane.showMessageDialog(null,"Unable to make new edge. "
                        + e.getMessage(),
                        "Error",
                        JOptionPane.PLAIN_MESSAGE,
                        UIManager.getIcon("OptionPane.errorIcon"));
                return;
            }
	    }
	}

//-----------------------------------------------------------------------------
	
	private void collectFragAndAPsCompatibleWithSelectedAPs(
			ArrayList<DENOPTIMAttachmentPoint> srcAPs) 
	{
		compatVrtxs = new ArrayList<DENOPTIMVertex>();
		
		// WARNING: here I re-do most of what is already done in
		// FragmentSpace.getFragmentsCompatibleWithTheseAPs.
		// However, here we add additional data to (clones) of the 
		// fragments, so that I can easily highlight the compatible APs in 
		// the selection GUI.
		
    	// First we get all possible APs on any fragment
    	ArrayList<DENOPTIMAttachmentPoint> compatAps = 
				FragmentSpace.getAPsCompatibleWithThese(srcAPs);
    	
    	// then keep unique fragment identifiers, and store unique
		genToLocIDMap = new HashMap<Integer,Integer>();
		
		String PRESELPROP = GUIVertexSelector.PRESELECTEDAPSFIELD;
		String PRESELPROPSEP = GUIVertexSelector.PRESELECTEDAPSFIELDSEP;
		
		for (DENOPTIMAttachmentPoint ap : compatAps)
		{
		    int vId = ap.getOwner().hashCode();
		    //TODO-V3 replace hash with Id
			//int vId = ap.getOwner().getVertexId();
			int apId = ap.getOwner().getIndexOfAP(ap);
			if (genToLocIDMap.keySet().contains(vId))
			{
				DENOPTIMVertex vrtx = compatVrtxs.get(genToLocIDMap.get(vId));
				String prop = vrtx.getProperty(PRESELPROP).toString();
				vrtx.setProperty(PRESELPROP,prop+PRESELPROPSEP+apId);
			}
			else
			{
			    DENOPTIMVertex bb = ap.getOwner().clone();
				bb.setProperty(PRESELPROP,apId);
				genToLocIDMap.put(vId,compatVrtxs.size());
				compatVrtxs.add(bb);
			}
		}
	}
	
//-----------------------------------------------------------------------------

	/**
	 * Changes the GUI appearance compatibly to no loaded fragment space
	 */
	public void renderForLackOfFragSpace() 
	{
	    mainPanel.toolBar.renderForLackOfFragSpace();
	}
	
//-----------------------------------------------------------------------------

	/**
     * Changes the GUI appearance and activated buttons that depend on the
     * fragment space being loaded
     */
	public void renderForPresenceOfFragSpace() 
	{
	    mainPanel.toolBar.renderForPresenceOfFragSpace();
	}
	
//-----------------------------------------------------------------------------

    /**
     * Changes the GUI appearance compatibly to no loaded fragment space
     */
    void renderThisForLackOfFragSpace() 
    {
        hasFragSpace = false;
        visualPanel.bringCardToTopOfVertexViewer(visualPanel.NOFSCARDNAME);
    }
    
//-----------------------------------------------------------------------------

    /**
     * Changes the GUI appearance and activated buttons that depend on the
     * fragment space being loaded
     */
    void renderThisForPresenceOfFragSpace() 
    {
        hasFragSpace = true;
    }
//-----------------------------------------------------------------------------

	/**
	 * Imports graphs from file. 
	 * @param file the file to open
	 */
	public void importGraphsFromFile(File file)
	{	
		dnGraphLibrary = readGraphsFromFile(file);
		
		try {
			molLibrary = DenoptimIO.readMoleculeData(
					file.getAbsolutePath());
		} catch (DENOPTIMException e) {
			System.out.println("Could not read molecules from " + file);
			for (int i=0; i<dnGraphLibrary.size(); i++)
			{
				molLibrary.add(builder.newAtomContainer());
			}
		}
			
		// Display the first
		currGrphIdx = 0;
		
		loadCurrentGraphIdxToViewer(false);
		updateGraphListSpinner();
	}

//-----------------------------------------------------------------------------

	private void appendGraphsFromFile(File file)
	{
	    // Reading graphs is format-agnostic
		ArrayList<DENOPTIMGraph> graphs = readGraphsFromFile(file);
		
		// Try to read or make molecular representations
		ArrayList<IAtomContainer> mols = new ArrayList<IAtomContainer>();
        FileFormat ff = null;
        try
        {
            ff = DenoptimIO.detectFileFormat(file);
        } catch (Exception e1)
        {
            // we'll ignore the format specific tasks
        }
        switch (ff)
        {
            case GRAPHSDF:
                try {
                    molLibrary.addAll(DenoptimIO.readMoleculeData(
                            file.getAbsolutePath()));
                } catch (DENOPTIMException e) {
                    System.err.println("WARNING: Could not read molecular "
                            + "representation from " + file);
                    for (int i=0; i<graphs.size(); i++)
                    {
                        molLibrary.add(builder.newAtomContainer());
                    }
                }
                break;
                
            default:
                // Add empty place holders
                for (int i=0; i<graphs.size(); i++)
                {
                    molLibrary.add(builder.newAtomContainer());
                }   
                break;    
        }
		
		int oldSize = dnGraphLibrary.size();
		if (graphs.size() > 0)
		{
			dnGraphLibrary.addAll(graphs);
			molLibrary.addAll(mols);
			
			// WE choose to display the first of the imported ones
			currGrphIdx = oldSize;
			
			loadCurrentGraphIdxToViewer(false);
			updateGraphListSpinner();
		}
	}
	
//-----------------------------------------------------------------------------

	private ArrayList<DENOPTIMGraph> readGraphsFromFile(File file)
	{
		ArrayList<DENOPTIMGraph> graphs = new ArrayList<DENOPTIMGraph>();
		try
		{
    		try 
    		{
    			graphs = DenoptimIO.readDENOPTIMGraphsFromFile(file, 
    			        hasFragSpace);	
    		} 
    		catch (UndetectedFileFormatException uff) 
    		{
                String[] options = {"Abandon", "SDF", "JSON"};
                FileFormat[] ffs = {null,
                        FileFormat.GRAPHSDF,
                        FileFormat.GRAPHJSON};
                int res = JOptionPane.showOptionDialog(null,
                    "<html>Failed to detect file type from file's "
                    + "extension.<br>"
                    + "Please, tell me how to interpret file <br>"
                    + "'" + file.getAbsolutePath() + "'<br>"
                    + "or 'Abandon' to give up.</html>",
                    "Specify File Type",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    UIManager.getIcon("OptionPane.warningIcon"),
                    options,
                    options[0]);
                FileFormat ff = null;
                switch (res)
                {
                    case 0:
                        graphs = new ArrayList<DENOPTIMGraph>();
                        break;
                        
                    case 1:
                        graphs = DenoptimIO.readDENOPTIMGraphsFromSDFile(
                                file.getAbsolutePath(), hasFragSpace);
                        break;
                        
                    case 2:
                        graphs = DenoptimIO.readDENOPTIMGraphsFromJSONFile(
                                file.getAbsolutePath(), hasFragSpace);
                        break;
                }
    		} 
    	}
		catch (Exception e) 
        {
			e.printStackTrace();
			String msg = "<html>Could not read graph from file <br> "
					+ "'" + file.getAbsolutePath() 
	                + "'<br>Hint on cause: ";
			msg = msg + e.getClass().getName()+ " (";
			if (e.getCause() != null)
			{
				msg = msg + e.getCause();
			}
			if (e.getMessage() != null)
			{
				msg = msg + " " + e.getMessage();
			}
			msg = msg + ")";
			if (hasFragSpace)
			{
				msg = msg + "<br>This could be due to a mistmatch between "
						+ "the fragment IDs in the<br>"
						+ "graph you are trying to load, "
						+ "and the currently loaded fragment space.<br>"
						+ "Aborting import of graphs.";
			}
			msg = msg + "</html>";
			JOptionPane.showMessageDialog(null,msg,
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
		}
		return graphs;
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Loads the graph corresponding to the field {@link #currGrphIdx}
	 */
	private void loadCurrentGraphIdxToViewer(boolean keepSprites)
	{
		if (dnGraphLibrary == null)
		{
			JOptionPane.showMessageDialog(null,
	                "No list of graphs loaded.",
	                "Error",
	                JOptionPane.PLAIN_MESSAGE,
	                UIManager.getIcon("OptionPane.errorIcon"));
			return;
		}
		
		// Clears the "dnGraph" and GUI components, but keep memory of the 
    	// status of the graph of an easy recovery
    	clearCurrentSystem();
    	
		dnGraph = dnGraphLibrary.get(currGrphIdx);
		
		if (molLibrary.get(currGrphIdx).getAtomCount() > 0)
		{
		    visualPanel.loadDnGraphToViewer(dnGraphLibrary.get(currGrphIdx), 
		            molLibrary.get(currGrphIdx), keepSprites, hasFragSpace);
		}
		else
		{
		    visualPanel.loadDnGraphToViewer(dnGraphLibrary.get(currGrphIdx),
		            keepSprites, hasFragSpace);
		}
		
		enableGraphDependentButtons(true);
	}
	
//-----------------------------------------------------------------------------
	
	/**
	 * Clears the current graph viewer but keeps track of the latest graph 
	 * loaded. 
	 */
	private void clearCurrentSystem()
	{	
		// Get rid of currently loaded graph
		dnGraph = null;
        visualPanel.clearCurrentSystem();
	}

//-----------------------------------------------------------------------------

	private void updateGraphListSpinner()
	{		
		graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 1, 
				dnGraphLibrary.size(), 1));
		totalGraphsLabel.setText(Integer.toString(dnGraphLibrary.size()));
	}

//-----------------------------------------------------------------------------

	private class GraphSpinnerChangeEvent implements ChangeListener
	{
		private boolean inEnabled = true;
		
		public GraphSpinnerChangeEvent()
		{}
		
		/**
		 * Enables/disable the listener
		 * @param var <code>true</code> to activate listener, 
		 * <code>false</code> to disable.
		 */
		public void setEnabled(boolean var)
		{
			this.inEnabled = var;
		}
		
        @Override
        public void stateChanged(ChangeEvent event)
        {
        	if (!inEnabled)
        	{
        		return;
        	}
        	
        	//NB here we convert from 1-based index in GUI to 0-based index
        	currGrphIdx = ((Integer) graphNavigSpinner.getValue())
        			.intValue() - 1;
        	loadCurrentGraphIdxToViewer(false);
        }
	}
	
//-----------------------------------------------------------------------------
	
	private void loadFragmentSpace()
	{
		// Define the fragment space via a new dialog
		FSParamsDialog fsParams = new FSParamsDialog(this);
        fsParams.pack();
        fsParams.setVisible(true);
        visualPanel.resetFragViewerCardDeck();
	}
	
//-----------------------------------------------------------------------------
	
	private void deprotectEditedSystem()
	{
		btnSaveEdits.setEnabled(false);
		btnAddGraph.setEnabled(true);
		btnOpenGraphs.setEnabled(true);
		
		if (dnGraphLibrary.size()==0)
		{
			graphNavigSpinner.setModel(new SpinnerNumberModel(0,0,0,0));
		}
		else
		{
			graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 1, 
					dnGraphLibrary.size(), 1));
		}
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setEditable(true); 
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setForeground(Color.BLACK);
		
		graphSpinnerListener.setEnabled(true);
	}
	
//-----------------------------------------------------------------------------
	
	private void protectEditedSystem()
	{
		btnSaveEdits.setEnabled(true);
		btnAddGraph.setEnabled(false);
		btnOpenGraphs.setEnabled(false);
		
		graphNavigSpinner.setModel(new SpinnerNumberModel(currGrphIdx+1, 
				currGrphIdx+1, currGrphIdx+1, 1));
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setEditable(false); 
		((DefaultEditor) graphNavigSpinner.getEditor())
			.getTextField().setForeground(Color.GRAY);
		
		graphSpinnerListener.setEnabled(false);
	}
	
//-----------------------------------------------------------------------------
    
    private void removeCurrentDnGraph() throws DENOPTIMException
    {
    	// Clears the "dnGraph" and GUI components, but keep memory of the 
    	// status of the graph of an easy recovery, though since the old graph
    	// is being removed, the recovered data is not needed anymore.
        clearCurrentSystem();
    	
    	// Actual removal from the library
    	if (dnGraphLibrary.size()>0)
    	{
    		dnGraphLibrary.remove(currGrphIdx);
    		molLibrary.remove(currGrphIdx);
    		int libSize = dnGraphLibrary.size();
    		
    		if (libSize > 0)
    		{
	    		if (currGrphIdx>=0 && currGrphIdx<libSize)
	    		{
	    			//we keep currGrphIdx as it will correspond to the next item
	    		}
	    		else
	    		{
	    			currGrphIdx = currGrphIdx-1;
	    		}
	
	    		// We use the currGrphIdx to load another dnGraph
		    	loadCurrentGraphIdxToViewer(false); 
		    	updateGraphListSpinner();
    		}
    		else
    		{
    			currGrphIdx = -1;
    			//Spinner will be fixed by the deprotection routine
    			totalGraphsLabel.setText(Integer.toString(
    					dnGraphLibrary.size()));
				visualPanel.bringCardToTopOfMolViewer(
				        visualPanel.EMPTYCARDNAME);
    			enableGraphDependentButtons(false);
    		}
    		deprotectEditedSystem();
    	}
    }

//-----------------------------------------------------------------------------

  	private void saveUnsavedChanges() 
  	{	      		
  		// Overwrite dnGraph in library
  		dnGraphLibrary.set(currGrphIdx, dnGraph);
  		
  		// WARNING: the dnGraph in the visualPanel should be in sync because any
  		// changes to it has resulted in an updaate of the graphViewer.
  		// Still, it is possible to introduce code modifications that make it
  		// go out of sync.
  		// Here, we ASSUME the dhGraph displayed in the graphViewer component
  		// of the visualPanel is in sync with dnGraph. Therefore, we just
  		// update the molecular viewer.
  		
  		if (updateMolViewer)
  		{
  			if (hasFragSpace)
  			{
  			    IAtomContainer mol = visualPanel.updateMolevularViewer();
  			    if (mol != null)
  			    {
      				try
                    {
                        DENOPTIMMoleculeUtils.removeUsedRCA(mol,dnGraph);
                        molLibrary.set(currGrphIdx, mol);
                        mol.setProperty(DENOPTIMConstants.GMSGTAG,
                                "ManuallyBuilt");
                    } catch (DENOPTIMException e)
                    {
                        System.err.println("Could not remove RCAs from while "
                                + "updating mol-viewer.");
                        e.printStackTrace();
                    }
	        	} else {
	        	    // Logging done within visualPanel
	        		molLibrary.set(currGrphIdx, builder.newAtomContainer());
	        	}
  			}
  			updateMolViewer = false;
  		}
  		
  		// Release constraints
        deprotectEditedSystem();
  	}
  	
//-----------------------------------------------------------------------------

	/**
	 * Check whether there are unsaved changes.
	 * @return <code>true</code> if there are unsaved changes.
	 */
	
	public boolean hasUnsavedChanges()
	{
		return unsavedChanges;
	}

//-----------------------------------------------------------------------------

	/*
	 * This is needed to stop JUNG and Jmol threads upon closure of this
	 * gui card.
	 */
	public void dispose() 
	{
		visualPanel.dispose();
	}
		
//-----------------------------------------------------------------------------
  	
}

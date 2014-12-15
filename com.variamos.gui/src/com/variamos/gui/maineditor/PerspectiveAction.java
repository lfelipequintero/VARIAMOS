package com.variamos.gui.maineditor;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JOptionPane;

import com.mxgraph.model.mxCell;
import com.mxgraph.util.mxResources;
import com.mxgraph.view.mxGraph;
import com.variamos.gui.pl.editor.PLGraphEditorFunctions;
import com.variamos.gui.refas.editor.MetaGraphEditorFunctions;
import com.variamos.gui.refas.editor.RefasGraph;
import com.variamos.gui.refas.editor.RefasGraphEditorFunctions;
import com.variamos.gui.refas.editor.SimulationGraphEditorFunctions;

@SuppressWarnings("serial")
public class PerspectiveAction extends AbstractEditorAction {
	
	private PerspectiveToolBar perspective;

	
	public PerspectiveAction (PerspectiveToolBar perspective)
	{
		this.perspective = perspective;
	}
	/**
		 * 
		 */
	public void actionPerformed(ActionEvent e) {


		VariamosGraphEditor editor = getEditor(e);
		int perspectiveInd = editor.getPerspective();
		if (editor != null) {
			if (!editor.isModified()
					|| JOptionPane.showConfirmDialog(editor,
							mxResources.get("loseChanges")) == JOptionPane.YES_OPTION) {
				mxGraph graph = editor.getGraphComponent().getGraph();
				JButton jb = (JButton) e.getSource();
				if (perspectiveInd != 0
						&& jb.getText().equals(
								mxResources.get("plPerspButton"))) {

					editor.setGraphEditorFunctions (new PLGraphEditorFunctions(editor));
					editor.clearPalettes();
					System.out.println("productButton");
					editor.updateEditor();
					editor.setPerspective(0);					
				}
				if (perspectiveInd != 1
						&& jb.getText().equals(mxResources.get("defectAnalyzerPerspButton"))) {
					editor.updateEditor();
					editor.clearPalettes();
					System.out.println("defectButton");
					editor.setPerspective(1);
				}
				if (perspectiveInd != 2
						&& jb.getText().equals(mxResources.get("modelingPerspButton"))) {
					editor.setGraphEditorFunctions (new RefasGraphEditorFunctions(editor));
					editor.setPerspective(2);
					editor.clearPalettes();
					editor.updateEditor();					
					
					System.out.println("modelingButton");
					((RefasGraph)graph).defineInitialGraph();
				}
				if (perspectiveInd != 3
						&& jb.getText().equals(mxResources.get("metamodelingPerspButton"))) {
					editor.setGraphEditorFunctions (new MetaGraphEditorFunctions(editor));
					editor.setPerspective(3);
					editor.clearPalettes();
					editor.updateEditor();		
					System.out.println("metamodelingButton");
				}
				
				if (perspectiveInd != 4
						&& jb.getText().equals(mxResources.get("simulationPerspButton"))) {
					editor.setGraphEditorFunctions (new SimulationGraphEditorFunctions(editor));
					editor.setPerspective(4);
					editor.clearPalettes();
					editor.updateEditor();		
					System.out.println("simulationButton");
				}
				// Check modified flag and display save dialog

				editor.setModified(false);
				editor.setCurrentFile(null);
				editor.getGraphComponent().zoomAndCenter();
				
			}
		}
		perspective.updateButtons();
	}
}
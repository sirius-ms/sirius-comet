package de.unijena.bioinf.sirius.gui.mainframe.results;

import de.unijena.bioinf.sirius.gui.configs.ConfigStorage;
import de.unijena.bioinf.sirius.gui.fingerid.CompoundCandidateView;
import de.unijena.bioinf.sirius.gui.mainframe.MainFrame;
import de.unijena.bioinf.sirius.gui.structure.ExperimentContainer;
import de.unijena.bioinf.sirius.gui.structure.SiriusResultElement;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.ArrayList;

public class ResultPanel extends JPanel implements ListSelectionListener{
	
	private ResultTreeListModel listModel;
	private JList<SiriusResultElement> resultsJList;
	private TreeVisualizationPanel tvp;
	private SpectraVisualizationPanel svp;
	private CompoundCandidateView ccv;
	private ResultTreeListTextCellRenderer cellRenderer;
	
	private ExperimentContainer ec;
	
	public void dispose() {
		ccv.dispose();
	}

	public ResultPanel(MainFrame owner, ConfigStorage config) {
		this(null, owner, config);
	}
	
	public ResultPanel(ExperimentContainer ec, MainFrame owner, ConfigStorage config) {
		super();
		this.setLayout(new BorderLayout());
		this.setToolTipText("Results");
		this.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(1,5,0,0),"Molecular formulas"));
// this.setBorder(BorderFactory.createEmptyBorder(5,0,0,0));
		this.ec = ec;

		if(this.ec!=null) this.listModel = new ResultTreeListModel(ec.getResults());
		else this.listModel = new ResultTreeListModel();
		this.resultsJList = new ResultsTreeList(this.listModel);
		this.listModel.setJList(this.resultsJList);
//		if(this.ec!=null){
//			listRenderer = new ResultTreeListThumbnailCellRenderers(ec.getResults());
//		}else{
//			listRenderer = new ResultTreeListThumbnailCellRenderers(new ArrayList<SiriusResultElement>());
//		}
//		resultsJList.setCellRenderer(listRenderer);
        cellRenderer = new ResultTreeListTextCellRenderer();
		resultsJList.setCellRenderer(cellRenderer);
		resultsJList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		resultsJList.setLayoutOrientation(JList.HORIZONTAL_WRAP);
		resultsJList.setVisibleRowCount(1);
//		resultsJList.getPreferredSize()
		resultsJList.setMinimumSize(new Dimension(0,45));
		resultsJList.setPreferredSize(new Dimension(0,45));
		resultsJList.addListSelectionListener(this);
		
		JScrollPane listJSP = new JScrollPane(resultsJList,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JPanel temp = new JPanel(new BorderLayout());
//		temp.setBorder(new TitledBorder(BorderFactory.createEmptyBorder(),"Molecular formulas"));
		temp.add(listJSP,BorderLayout.NORTH);
		this.add(temp,BorderLayout.NORTH);
		
		JTabbedPane centerPane = new JTabbedPane();
		centerPane.setBorder(BorderFactory.createEmptyBorder());
		tvp = new TreeVisualizationPanel(owner,config);
		centerPane.addTab("Tree view",tvp);
		
		svp = new SpectraVisualizationPanel(ec);
		centerPane.addTab("Spectra view",svp);

        ccv = new CompoundCandidateView(owner);
        centerPane.addTab("CSI:FingerId", ccv);
		
		this.add(centerPane,BorderLayout.CENTER);

//		this.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),"Results"));
	}

	public void changeData(final ExperimentContainer ec){
		this.ec = ec;
        cellRenderer.ec = ec;
		SiriusResultElement sre = null;
		resultsJList.removeListSelectionListener(this);
		if(this.ec!=null&&ec.getResults()!=null&&!this.ec.getResults().isEmpty()){
			this.listModel.setData(ec.getResults());
			if(this.listModel.getSize()>0){
				this.resultsJList.setSelectedIndex(0);
				sre = ec.getResults().get(0);
			}
		}else{
			this.listModel.setData(new ArrayList<SiriusResultElement>());
		}
		resultsJList.addListSelectionListener(this);
		final SiriusResultElement element = sre;
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				svp.changeExperiment(ec,element);
				if(element==null) tvp.showTree(null);
				else tvp.showTree(element);
			}
		});
        ccv.changeData(ec, sre);

	}

	public void select(SiriusResultElement sre, boolean fireEvent) {
        if (fireEvent) resultsJList.setSelectedValue(sre, true);
        if(sre==null){
            tvp.showTree(null);
            svp.changeSiriusResultElement(null);
            ccv.changeData(ec, sre);
        }else{
            tvp.showTree(sre);
            svp.changeSiriusResultElement(sre);
            ccv.changeData(ec, sre);
        }
	}

	@Override
	public void valueChanged(ListSelectionEvent e) {
		SiriusResultElement sre = this.resultsJList.getSelectedValue();
		select(sre, false);
	}

}

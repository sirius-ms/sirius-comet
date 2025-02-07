package de.unijena.bioinf.ms.gui.dialogs;
/*
 *
 *  This file is part of the SIRIUS library for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2021 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with SIRIUS. If not, see <https://www.gnu.org/licenses/lgpl-3.0.txt>
 */

import de.unijena.bioinf.ChemistryBase.chem.FormulaConstraints;
import de.unijena.bioinf.ChemistryBase.chem.PeriodicTable;
import de.unijena.bioinf.ChemistryBase.chem.PrecursorIonType;
import de.unijena.bioinf.jjobs.TinyBackgroundJJob;
import de.unijena.bioinf.ms.gui.SiriusGui;
import de.unijena.bioinf.ms.gui.actions.DeleteExperimentAction;
import de.unijena.bioinf.ms.gui.actions.SiriusActions;
import de.unijena.bioinf.ms.gui.compute.DBSelectionList;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.dialogs.asms.CMLFilterPanel;
import de.unijena.bioinf.ms.gui.mainframe.instance_panel.CompoundList;
import de.unijena.bioinf.ms.gui.utils.*;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.CheckBoxListItem;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckBoxList;
import de.unijena.bioinf.ms.gui.utils.jCheckboxList.JCheckboxListPanel;
import de.unijena.bioinf.ms.nightsky.sdk.model.AlignedFeature;
import de.unijena.bioinf.ms.nightsky.sdk.model.SearchableDatabase;
import de.unijena.bioinf.projectspace.InstanceBean;
import org.jdesktop.swingx.JXTitledSeparator;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Dialog allows to adjust filter criteria of the {@link CompoundFilterModel} which is used to filter compound list.
 */
public class CompoundFilterOptionsDialog extends JDialog implements ActionListener {

    final PlaceholderTextField searchField;
    final JTextField searchFieldDialogCopy;
    final JSpinner minMzSpinner, maxMzSpinner, minRtSpinner, maxRtSpinner, minConfidenceSpinner, maxConfidenceSpinner, candidateSpinner;
    public final JCheckboxListPanel<PrecursorIonType> adductOptions;
    JButton discard, apply, reset;
    final JCheckBox invertFilter, deleteSelection, elementsMatchFormula, elementsMatchPrecursorFormula, hasMs1, hasMsMs;

    final CompoundFilterModel filterModel;
    final CompoundList compoundList;


    final JComboBox<CompoundFilterModel.LipidFilter> lipidFilterBox;
    final PlaceholderTextField elementsField;

    final JCheckboxListPanel<SearchableDatabase> searchDBList;

    private final QualityFilterPanel overallQualityPanel;
    private final List<QualityFilterPanel> qualityPanels;

    private final CMLFilterPanel cmlFilterPanel;

    private JCheckBoxList<String> hideTagList;
    private JCheckBox subtract;
    private JSpinner subtractRatioSpinner;
    private JSpinner subtractMzPPMSpinner;
    private JSpinner subtractRTDevSpinner;

    final SiriusGui gui;

    public CompoundFilterOptionsDialog(SiriusGui gui, PlaceholderTextField searchField, CompoundFilterModel filterModel, CompoundList compoundList) {
        super(gui.getMainFrame(), "Filter configuration", true);
        this.gui = gui;
        this.searchField = searchField;
        this.filterModel = filterModel;
        this.compoundList = compoundList;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout());
        setResizable(false);

        final JTabbedPane centerTab = new JTabbedPane();
        add(centerTab, BorderLayout.CENTER);

        final TwoColumnPanel generalParameters = new TwoColumnPanel();
        centerTab.addTab("General", generalParameters);

        {
            searchFieldDialogCopy = new JTextField(searchField.getText());
            final TwoColumnPanel fullTextPanel = new TwoColumnPanel();
            fullTextPanel.addNamed("Fulltext search", searchFieldDialogCopy);
            add(fullTextPanel, BorderLayout.NORTH);
        }
        //general filter
        {
            generalParameters.add(Box.createVerticalStrut(5));
            generalParameters.add(new JXTitledSeparator("Thresholds"));

            {
                TwoColumnPanel min = new TwoColumnPanel();
                TwoColumnPanel max = new TwoColumnPanel();
                Box box = Box.createHorizontalBox();

                box.add(min);
                box.add(Box.createHorizontalGlue());
                box.add(max);

                minMzSpinner = makeSpinner(filterModel.getCurrentMinMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
                maxMzSpinner = makeSpinner(filterModel.getCurrentMaxMz(), filterModel.getMinMz(), filterModel.getMaxMz(), 10);
                ((JSpinner.DefaultEditor) maxMzSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxMzSpinner.getModel(), filterModel.getMaxMz()));
                min.addNamed("Min m/z", minMzSpinner);
                max.addNamed("Max m/z", maxMzSpinner);
                ensureCompatibleBounds(minMzSpinner, maxMzSpinner);

                minRtSpinner = makeSpinner(filterModel.getCurrentMinRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
                maxRtSpinner = makeSpinner(filterModel.getCurrentMaxRt(), filterModel.getMinRt(), filterModel.getMaxRt(), 10);
                ((JSpinner.DefaultEditor) maxRtSpinner.getEditor()).getTextField().setFormatterFactory(new MaxDoubleAsInfinityTextFormatterFactory((SpinnerNumberModel) maxRtSpinner.getModel(), filterModel.getMaxRt()));

                min.addNamed("Min RT (in sec)", minRtSpinner);
                max.addNamed("Max RT (in sec)", maxRtSpinner);
                ensureCompatibleBounds(minRtSpinner, maxRtSpinner);

                minConfidenceSpinner = makeSpinner(filterModel.getCurrentMinConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
                maxConfidenceSpinner = makeSpinner(filterModel.getCurrentMaxConfidence(), filterModel.getMinConfidence(), filterModel.getMaxConfidence(), .05);
                min.addNamed("Min confidence", minConfidenceSpinner);
                max.addNamed("Max confidence", maxConfidenceSpinner);
                ensureCompatibleBounds(minConfidenceSpinner, maxConfidenceSpinner);

                generalParameters.add(box);
            }

            {
                generalParameters.add(Box.createVerticalStrut(5));
                generalParameters.add(new JXTitledSeparator("Lipid Class Filter"));

                //lipid filter
                lipidFilterBox = new JComboBox<>();
                java.util.List.copyOf(EnumSet.allOf(CompoundFilterModel.LipidFilter.class)).forEach(lipidFilterBox::addItem);
                generalParameters.addNamed("Lipid filter", lipidFilterBox);
                lipidFilterBox.setSelectedItem(filterModel.getLipidFilter());
            }

            final TwoColumnPanel dataParameters = new TwoColumnPanel();
            centerTab.addTab("Data Quality", dataParameters);

            //MS data availability filter
            {
                dataParameters.add(Box.createVerticalStrut(5));
                dataParameters.add(new JXTitledSeparator("MS Data Quality"));
                hasMs1 = new JCheckBox("MS1");
                hasMs1.setToolTipText("Feature must have a least one MS1 Spectrum");
                hasMs1.setSelected(filterModel.isHasMs1());
                hasMsMs = new JCheckBox("MS/MS");
                hasMsMs.setToolTipText("Feature must have a least one MS/MS Spectrum");
                hasMsMs.setSelected(filterModel.isHasMsMs());

                Box box = Box.createHorizontalBox();
                box.add(hasMs1);
                box.add(Box.createHorizontalStrut(50));
                box.add(hasMsMs);
                box.add(Box.createHorizontalStrut(10));
                dataParameters.add(box);
            }

            //quality filter
            {
                dataParameters.add(Box.createVerticalStrut(5));
                dataParameters.add(new JXTitledSeparator("Feature Quality"));
                overallQualityPanel = new QualityFilterPanel(filterModel.getFeatureQualityFilter());
                dataParameters.addNamed("<html><b>Overall quality</b></html>", overallQualityPanel);
                dataParameters.add(Box.createVerticalStrut(5));
                qualityPanels = filterModel.getIoQualityFilters().stream().map(qf -> {
                    QualityFilterPanel qfp = new QualityFilterPanel(qf);
                    dataParameters.addNamed(qf.getName(), qfp);
                    return qfp;
                }).toList();
            }

            dataParameters.addVerticalGlue();

        }

        final JPanel resultParameters = new JPanel();
        resultParameters.setLayout(new BoxLayout(resultParameters, BoxLayout.Y_AXIS));
        resultParameters.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
        centerTab.addTab("Results", resultParameters);

        // Element filter
        {
            resultParameters.add(Box.createVerticalStrut(5));
            resultParameters.add(new JXTitledSeparator("Elements"));

            JPanel elementSelector = new JPanel();
            elementSelector.setLayout(new BoxLayout(elementSelector, BoxLayout.X_AXIS));
            JButton selectElements = new JButton("...");
            elementsField = new PlaceholderTextField(20);
            if (filterModel.getElementFilter().isActive())
                elementsField.setText(filterModel.getElementFilter().getConstraints().toString());

            selectElements.addActionListener(e -> {
                FormulaConstraints elements = new CompoundFilterModel.ElementFilter(elementsField.getText()).getConstraints();
                ElementSelectionDialog diag = new ElementSelectionDialog(this, "Filter Elements", elements);
                elements = diag.getConstraints();
                if (elements.equals(FormulaConstraints.empty()))
                    elementsField.setText(null);
                else
                    elementsField.setText(elements.toString());
            });
            elementsField.setPlaceholder("Insert or Select formula constraints");
            elementSelector.add(elementsField);
            elementSelector.add(selectElements);
            elementsMatchFormula = new JCheckBox("Molecular Formula");
            elementsMatchFormula.setSelected(filterModel.getElementFilter().isMatchFormula());
            elementsMatchPrecursorFormula = new JCheckBox("Precursor Formula");
            elementsMatchPrecursorFormula.setSelected(filterModel.getElementFilter().isMatchPrecursorFormula());

            final Box group = Box.createHorizontalBox();
            group.add(elementsMatchFormula);
            group.add(Box.createHorizontalStrut(25));
            group.add(elementsMatchPrecursorFormula);
            group.add(Box.createHorizontalGlue());

            resultParameters.add(elementSelector);
            resultParameters.add(group);
        }

        // Adduct filter
        {
            resultParameters.add(Box.createVerticalStrut(5));
            adductOptions = new JCheckboxListPanel<>(new JCheckBoxList<>(), "Adducts", GuiUtils.formatToolTip("Select adducts to  filter by. Selecting all or none mean every adducts can pass"));
            adductOptions.checkBoxList.setPrototypeCellValue(new CheckBoxListItem<>(PrecursorIonType.fromString("[M + H20 + Na]+"), false));

            List<PrecursorIonType> ionizations = new ArrayList<>(PeriodicTable.getInstance().getAdductsAndUnKnowns());
            Collections.sort(ionizations);

            adductOptions.checkBoxList.replaceElements(ionizations);
            adductOptions.checkBoxList.uncheckAll();
            adductOptions.setEnabled(true);

//            generalParameters.add(adductOptions);
            adductOptions.checkBoxList.checkAll(filterModel.getAdducts());


            // db filter
            searchDBList = new JCheckboxListPanel<>(DBSelectionList.fromSearchableDatabases(gui.getSiriusClient()), "Hit in structure DB");
//            generalParameters.add(searchDBList);
            searchDBList.remove(searchDBList.buttons);
            searchDBList.checkBoxList.uncheckAll();

            candidateSpinner = makeSpinner(1, 1, 100, 1);
            searchDBList.addFooter(new TwoColumnPanel("Candidates to check", candidateSpinner));

            // create adduct lists
            Box box = Box.createHorizontalBox();
            box.add(adductOptions);
            box.add(Box.createHorizontalStrut(50));
            box.add(searchDBList);
            box.add(Box.createHorizontalStrut(10));

            resultParameters.add(box);
            resultParameters.add(Box.createVerticalBox());

            if (filterModel.isDbFilterEnabled()) { //null check
                searchDBList.checkBoxList.checkAll(filterModel.getDbFilter().getDbs());
                candidateSpinner.setValue(filterModel.getDbFilter().getNumOfCandidates());
            }
        }

        // subtraction filter
        {
            Set<String> tags = gui.applySiriusClient((client, pid) ->
                    client.features().getAlignedFeatures(pid, List.of()).stream().map(AlignedFeature::getTag).filter(Objects::nonNull).filter(tag -> !tag.isEmpty() && !tag.isBlank()).collect(Collectors.toSet())
            );

            final JPanel tagParameters = new JPanel(new BorderLayout());
            tagParameters.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
            centerTab.addTab("Tags", tagParameters);

            centerTab.setEnabledAt(3, !tags.isEmpty());

            if (!tags.isEmpty()) {
                List<String> tagList = tags.stream().filter(tag -> !tag.isBlank()).sorted().toList();
                hideTagList = new JCheckBoxList<>(tagList, String::equalsIgnoreCase);
                for (String tag : filterModel.getHiddenTags()) {
                    hideTagList.check(tag);
                }
                JCheckboxListPanel<String> hidePanel = new JCheckboxListPanel<>(hideTagList, "Hide tags", GuiUtils.formatToolTip("Select tagged compounds/features to hide."));

                subtract = new JCheckBox();
                subtract.setToolTipText(GuiUtils.formatToolTip("Hide all compounds with mass and retention time matching compounds/features with hidden tags."));

                subtractRatioSpinner = makeSpinner(2, 1, 100, 1.0);
                subtractMzPPMSpinner = makeSpinner(10, 0.1, 100, 1);
                subtractRTDevSpinner = makeSpinner(2, 0.1, 100, 1);

                subtract.setSelected(filterModel.isFeatureSubtractionEnabled());
                subtractRatioSpinner.setEnabled(subtract.isSelected());
                subtractMzPPMSpinner.setEnabled(subtract.isSelected());
                subtractRTDevSpinner.setEnabled(subtract.isSelected());

                subtract.addActionListener(evt -> {
                    subtractRatioSpinner.setEnabled(subtract.isSelected());
                    subtractMzPPMSpinner.setEnabled(subtract.isSelected());
                    subtractRTDevSpinner.setEnabled(subtract.isSelected());
                });

                subtractMzPPMSpinner.setToolTipText(GuiUtils.formatToolTip("Max m/z deviation of matching features"));
                subtractRTDevSpinner.setToolTipText(GuiUtils.formatToolTip("Max retention time deviation of matching features"));
                subtractRatioSpinner.setToolTipText(GuiUtils.formatToolTip("Hide features with intensity <= ratio * intensity of the matched hidden feature"));

                TwoColumnPanel subtractPanel = new TwoColumnPanel();
                subtractPanel.add(new JXTitledSeparator("Hide matching compounds"));
                subtractPanel.addNamed("Enabled", subtract);
                subtractPanel.addNamed("MS1 m/z accuracy [ppm]", subtractMzPPMSpinner);
                subtractPanel.addNamed("RT accuracy [sec]", subtractRTDevSpinner);
                subtractPanel.addNamed("Max intensity ratio", subtractRatioSpinner);
                subtractPanel.addVerticalGlue();

                Box box = Box.createHorizontalBox();
                box.add(hidePanel);
                box.add(Box.createHorizontalStrut(50));
                box.add(subtractPanel);
                box.add(Box.createHorizontalStrut(10));

                tagParameters.add(box);
            }


        }

        // filter modifiers
        {
            generalParameters.add(Box.createVerticalStrut(5));
            generalParameters.add(new JXTitledSeparator("Filter modifiers"));

            invertFilter = new JCheckBox("Invert Filter");
            invertFilter.setSelected(compoundList.isFilterInverted());

            deleteSelection = new JCheckBox("<html>Delete all <b>non-</b>matching compounds</html>");
            deleteSelection.setSelected(false);

            final Box group = Box.createHorizontalBox();
            group.add(invertFilter);
            group.add(Box.createHorizontalStrut(25));
            group.add(deleteSelection);
            group.add(Box.createHorizontalGlue());
            generalParameters.add(group);

        }

        final JPanel cometParameters = new JPanel();
        cometParameters.setLayout(new BoxLayout(cometParameters, BoxLayout.Y_AXIS));
        cometParameters.setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 5));
        centerTab.addTab("COMET", cometParameters);

        // combinatorial molecule library filters
        {
            cometParameters.add(Box.createVerticalStrut(5));
            cometParameters.add(new JXTitledSeparator("COMET Filter for Affinity selection-mass spectrometry"));
            this.cmlFilterPanel = new CMLFilterPanel(this.filterModel);
            cometParameters.add(this.cmlFilterPanel);
        }

        generalParameters.addVerticalGlue();

        reset = new JButton("Reset");
        reset.addActionListener(this);
        generalParameters.add(new JSeparator(SwingConstants.VERTICAL));

        discard = new JButton("Discard");
        discard.addActionListener(this);
        apply = new JButton("Apply");
        apply.addActionListener(this);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        buttons.add(reset);
        buttons.add(Box.createHorizontalGlue());
        buttons.add(discard);
        buttons.add(apply);

        add(buttons, BorderLayout.SOUTH);

        setMaximumSize(GuiUtils.getEffectiveScreenSize(getGraphicsConfiguration()));
        configureActions();
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    private void configureActions() {
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        KeyStroke enterKey = KeyStroke.getKeyStroke("ENTER");
        KeyStroke escKey = KeyStroke.getKeyStroke("ESCAPE");
        String enterAction = "compute";
        String escAction = "abort";
        inputMap.put(enterKey, enterAction);
        inputMap.put(escKey, escAction);
        getRootPane().getActionMap().put(enterAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveChanges();
                dispose();
            }
        });
        getRootPane().getActionMap().put(escAction, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void saveChanges() {
        if (deleteSelection.isSelected()) {
            deleteSelectedCompoundsAndResetFilter();

        } else {
            applyToModel(filterModel);
            if (invertFilter.isSelected() != compoundList.isFilterInverted())
                compoundList.toggleInvertFilter();

            filterModel.fireUpdateCompleted();
        }
    }

    private void applyToModel(@NotNull CompoundFilterModel filterModel) {
        filterModel.setCurrentMinMz(getMinMz());
        filterModel.setCurrentMaxMz(getMaxMz());
        filterModel.setCurrentMinRt(getMinRt());
        filterModel.setCurrentMaxRt(getMaxRt());
        filterModel.setCurrentMinConfidence(getMinConfidence());
        filterModel.setCurrentMaxConfidence(getMaxConfidence());
        filterModel.setHasMs1(hasMs1.isSelected());
        filterModel.setHasMsMs(hasMsMs.isSelected());
        filterModel.setAdducts(new HashSet<>(adductOptions.checkBoxList.getCheckedItems()));

        overallQualityPanel.updateModel(filterModel.getFeatureQualityFilter());

        Iterator<QualityFilterPanel> qualityPanelIt = qualityPanels.iterator();
        Iterator<CompoundFilterModel.QualityFilter> qualityFilterIt = filterModel.getIoQualityFilters().iterator();
        while (qualityPanelIt.hasNext() && qualityFilterIt.hasNext())
            qualityPanelIt.next().updateModel(qualityFilterIt.next());

        filterModel.setLipidFilter((CompoundFilterModel.LipidFilter) lipidFilterBox.getSelectedItem());

        filterModel.setElementFilter(new CompoundFilterModel.ElementFilter(
                        elementsField.getText() == null || elementsField.getText().isBlank()
                                ? FormulaConstraints.empty()
                                : FormulaConstraints.fromString(elementsField.getText()),
                        elementsMatchFormula.isSelected(), elementsMatchPrecursorFormula.isSelected()
                )
        );

        filterModel.setDbFilter(new CompoundFilterModel.DbFilter(searchDBList.checkBoxList.getCheckedItems(),
                ((SpinnerNumberModel) candidateSpinner.getModel()).getNumber().intValue()));

        filterModel.enableTagHiding(!hideTagList.getCheckedItems().isEmpty());
        filterModel.setHiddenTags(new HashSet<>(hideTagList.getCheckedItems()));
        filterModel.enableFeatureSubtraction(subtract.isSelected());
        filterModel.setFeatureSubtractionMinRatio(((SpinnerNumberModel) subtractRatioSpinner.getModel()).getNumber().doubleValue());
        filterModel.setFeatureSubtractionMzDev(((SpinnerNumberModel) subtractMzPPMSpinner.getModel()).getNumber().doubleValue());
        filterModel.setFeatureSubtractionRtDev(((SpinnerNumberModel) subtractRTDevSpinner.getModel()).getNumber().doubleValue());

        this.cmlFilterPanel.applyToModel(filterModel);

        saveTextFilter();
    }

    private void saveTextFilter() {
        searchField.setText(searchFieldDialogCopy.getText());
        searchField.postActionEvent();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reset) {
            resetFilter();
            return;
        }
        if (e.getSource() == apply) {
            saveChanges();
        }
        this.dispose();
    }

    private void deleteSelectedCompoundsAndResetFilter() {

        // create deletion matcher
        CompoundFilterModel tmpModel = new CompoundFilterModel(gui, compoundList);
        applyToModel(tmpModel);
        CompoundFilterMatcher matcher = new CompoundFilterMatcher(gui.getProperties(), tmpModel);
        boolean inverted = invertFilter.isSelected();
        // reset global filter and close
        resetFilter();
        saveChanges();
        dispose();

        // clear selection to prevent unnecessary updates during deletions
        gui.getMainFrame().getCompoundList().getCompoundListSelectionModel().clearSelection();

        // collect instances to delete
        List<InstanceBean> toDelete = Jobs.runInBackgroundAndLoad(gui.getMainFrame(), "Filtering...", new TinyBackgroundJJob<List<InstanceBean>>() {
                    @Override
                    protected List<InstanceBean> compute() {
                        final int max = compoundList.getCompoundList().size();
                        AtomicInteger progress = new AtomicInteger(0);
                        if (inverted) {
                            return compoundList.getCompoundList().stream()
                                    .peek(i -> updateProgress(max, progress.getAndIncrement(), i.getGUIName()))
                                    .filter(matcher::matches).collect(Collectors.toList());
                        } else {
                            return compoundList.getCompoundList().stream()
                                    .peek(i -> updateProgress(max, progress.getAndIncrement(), i.getGUIName()))
                                    .filter(i -> !matcher.matches(i)).collect(Collectors.toList());
                        }
                    }
                }
        ).getResult();

        //delete instances
        ((DeleteExperimentAction) SiriusActions.DELETE_EXP.getInstance(gui)).deleteCompounds(toDelete);
    }

    /**
     * only reset values in the dialog, not the actual filter model
     */
    private void resetFilter() {
        resetSpinnerValues();
        adductOptions.checkBoxList.uncheckAll();
        overallQualityPanel.reset();
        qualityPanels.forEach(QualityFilterPanel::reset);

        lipidFilterBox.setSelectedItem(CompoundFilterModel.LipidFilter.KEEP_ALL_COMPOUNDS);
        elementsField.setText(null);
        searchDBList.checkBoxList.uncheckAll();
        searchFieldDialogCopy.setText("");
        invertFilter.setSelected(false);
        deleteSelection.setSelected(false);
        hasMs1.setSelected(false);
        hasMsMs.setSelected(false);

        subtract.setSelected(false);
        hideTagList.uncheckAll();
        hideTagList.check("blank");
        hideTagList.check("control");
        cmlFilterPanel.reset();
    }

    private void resetSpinnerValues() {
        minMzSpinner.setValue(filterModel.getMinMz());
        maxMzSpinner.setValue(filterModel.getMaxMz());
        minRtSpinner.setValue(filterModel.getMinRt());
        maxRtSpinner.setValue(filterModel.getMaxRt());
        minConfidenceSpinner.setValue(filterModel.getMinConfidence());
        maxConfidenceSpinner.setValue(filterModel.getMaxConfidence());
        candidateSpinner.setValue(1);

        subtractMzPPMSpinner.setValue(10.0);
        subtractRTDevSpinner.setValue(2.0);
        subtractRatioSpinner.setValue(2.0);
    }

    public double getMinMz() {
        return getDoubleValue(minMzSpinner);
    }

    public double getMaxMz() {
        return getDoubleValue(maxMzSpinner);
    }

    public double getMinRt() {
        return getDoubleValue(minRtSpinner);
    }

    public double getMaxRt() {
        return getDoubleValue(maxRtSpinner);
    }

    public double getMinConfidence() {
        return getDoubleValue(minConfidenceSpinner);
    }

    public double getMaxConfidence() {
        return getDoubleValue(maxConfidenceSpinner);
    }


    public double getDoubleValue(JSpinner spinner) {
        return ((SpinnerNumberModel) spinner.getModel()).getNumber().doubleValue();
    }

    public int getIntValue(JSpinner spinner) {
        return ((SpinnerNumberModel) spinner.getModel()).getNumber().intValue();
    }

    private void ensureCompatibleBounds(JSpinner minSpinner, JSpinner maxSpinner) {
        minSpinner.addChangeListener(e -> {
            if (e.getSource() == minSpinner) {
                double min = ((SpinnerNumberModel) minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel) maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    maxSpinner.setValue(min);
                }
            }
        });

        maxSpinner.addChangeListener(e -> {
            if (e.getSource() == maxSpinner) {
                double min = ((SpinnerNumberModel) minSpinner.getModel()).getNumber().doubleValue();
                double max = ((SpinnerNumberModel) maxSpinner.getModel()).getNumber().doubleValue();
                if (min > max) {
                    minSpinner.setValue(max);
                }
            }
        });
    }

    public JSpinner makeSpinner(double value, double minimum, double maximum, double stepSize) {
        SpinnerNumberModel model = new SpinnerNumberModel(value, minimum, maximum, stepSize);
        JSpinner spinner = new JSpinner(model);
        spinner.setMinimumSize(new Dimension(200, 26));
        spinner.setPreferredSize(new Dimension(200, 26));

        return spinner;
    }

    private static class QualityFilterPanel extends JPanel {
        JCheckBox[] qualityBoxes;

        public QualityFilterPanel(@NotNull CompoundFilterModel.QualityFilter qualityFilterModel) {
            super();
            final BoxLayout groupLayout = new BoxLayout(this, BoxLayout.X_AXIS);
            setLayout(groupLayout);

            qualityBoxes = qualityFilterModel.getPossibleQualities().stream().map(JCheckBox::new).toArray(JCheckBox[]::new);
            for (int i = 0; i < qualityBoxes.length; ++i) {
                add(Box.createHorizontalGlue());
                add(qualityBoxes[i]);
                qualityBoxes[i].setSelected(qualityFilterModel.isQualitySelected(i));
            }
            add(Box.createHorizontalStrut(10));
        }

        public void reset() {
            for (JCheckBox jCheckBox : qualityBoxes)
                jCheckBox.setSelected(true);
        }

        public void updateModel(CompoundFilterModel.QualityFilter qualityFilter) {
            for (int k = 0; k < qualityBoxes.length; ++k)
                qualityFilter.setQualitySelected(k, qualityBoxes[k].isSelected());
        }
    }
}

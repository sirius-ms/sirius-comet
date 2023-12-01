/*
 *  This file is part of the SIRIUS Software for analyzing MS and MS/MS data
 *
 *  Copyright (C) 2013-2020 Kai Dührkop, Markus Fleischauer, Marcus Ludwig, Martin A. Hoffman, Fleming Kretschmer, Marvin Meusel and Sebastian Böcker,
 *  Chair of Bioinformatics, Friedrich-Schiller University.
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either
 *  version 3 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with SIRIUS.  If not, see <https://www.gnu.org/licenses/agpl-3.0.txt>
 */

package de.unijena.bioinf.ms.gui.fingerid.custom_db;

import de.unijena.bioinf.chemdb.DataSources;
import de.unijena.bioinf.chemdb.SearchableDatabases;
import de.unijena.bioinf.chemdb.custom.CustomDatabase;
import de.unijena.bioinf.ms.frontend.subtools.custom_db.CustomDBOptions;
import de.unijena.bioinf.ms.gui.compute.jjobs.Jobs;
import de.unijena.bioinf.ms.gui.configs.Buttons;
import de.unijena.bioinf.ms.gui.configs.Icons;
import de.unijena.bioinf.ms.gui.dialogs.DialogHeader;
import de.unijena.bioinf.ms.gui.dialogs.QuestionDialog;
import de.unijena.bioinf.ms.gui.dialogs.StacktraceDialog;
import de.unijena.bioinf.ms.gui.utils.GuiUtils;
import de.unijena.bioinf.ms.gui.utils.ListAction;
import de.unijena.bioinf.ms.gui.utils.TextHeaderBoxPanel;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static de.unijena.bioinf.ms.gui.mainframe.MainFrame.MF;

public class DatabaseDialog extends JDialog {

    protected JList<String> dbList;
    protected Map<String, CustomDatabase> customDatabases;

    protected DatabaseView dbView;
    JButton deleteDB, editDB, addCustomDb;


    public DatabaseDialog(final Frame owner) {
        super(owner, true);
        setTitle("Custom Databases");
        setLayout(new BorderLayout());

        //============= NORTH (Header) =================
        JPanel header = new DialogHeader(Icons.DB_64);
        add(header, BorderLayout.NORTH);


        this.customDatabases = Jobs.runInBackgroundAndLoad(owner, "Loading DBs...", (Callable<List<CustomDatabase>>) SearchableDatabases::getCustomDatabases).getResult()
                .stream().collect(Collectors.toMap(CustomDatabase::name, k -> k));
        this.dbList = new DatabaseList(customDatabases.keySet().stream().sorted().collect(Collectors.toList()));
        JScrollPane scroll = new JScrollPane(dbList, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        TextHeaderBoxPanel pane = new TextHeaderBoxPanel("Custom Databases", scroll);
        pane.setBorder(BorderFactory.createEmptyBorder(GuiUtils.SMALL_GAP, GuiUtils.SMALL_GAP, 0, 0));

        addCustomDb = Buttons.getAddButton16("Create custom DB");
        deleteDB = Buttons.getRemoveButton16("Delete Custom Database");
        editDB = Buttons.getEditButton16("Edit Custom Database");

        final Box but = Box.createHorizontalBox();
        but.add(Box.createHorizontalGlue());
        but.add(deleteDB);
        but.add(editDB);
        but.add(addCustomDb);
        editDB.setEnabled(false);
        deleteDB.setEnabled(false);

        this.dbView = new DatabaseView();

        add(but, BorderLayout.SOUTH);
        add(pane, BorderLayout.CENTER);
        add(dbView, BorderLayout.EAST);


        dbList.addListSelectionListener(e -> {
            final int i = dbList.getSelectedIndex();
            if (i >= 0) {
                final String s = dbList.getModel().getElementAt(i);
                if (customDatabases.containsKey(s)) {
                    final CustomDatabase c = customDatabases.get(s);
                    dbView.updateContent(c);
                    editDB.setEnabled(!c.needsUpgrade());
                    deleteDB.setEnabled(true);
                } else {
                    editDB.setEnabled(false);
                    deleteDB.setEnabled(false);
                }

            }
        });

        dbList.setSelectedIndex(0);

        addCustomDb.addActionListener(e -> new ImportDatabaseDialog(this));


        //klick on Entry ->  open import dialog
        new ListAction(dbList, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final int k = dbList.getSelectedIndex();
                if (k >= 0 && k < dbList.getModel().getSize()) {
                    String key = dbList.getModel().getElementAt(k);
                    CustomDatabase db = customDatabases.get(key);
                    new ImportDatabaseDialog(DatabaseDialog.this, db);
                }

            }
        });

        //edit button ->  open import dialog
        editDB.addActionListener(e -> {
            final int k = dbList.getSelectedIndex();
            if (k >= 0 && k < dbList.getModel().getSize()) {
                String key = dbList.getModel().getElementAt(k);
                CustomDatabase db = customDatabases.get(key);
                new ImportDatabaseDialog(this, db);
            }
        });

        deleteDB.addActionListener(e -> {
            final int index = dbList.getSelectedIndex();
            if (index < 0 || index >= dbList.getModel().getSize())
                return;
            final String name = dbList.getModel().getElementAt(index);
            final String msg = "Do you really want to remove the custom database (will not be deleted from disk)'" + name + "'?";
            if (new QuestionDialog(getOwner(), msg).isSuccess()) {

                try {
                    Jobs.runCommandAndLoad(Arrays.asList(
                                            CustomDBOptions.class.getAnnotation(CommandLine.Command.class).name(),
                                            "--remove", name), null, null, owner,
                                    "Deleting database '" + name + "'...", true)
                            .awaitResult();
                } catch (ExecutionException ex) {
                    LoggerFactory.getLogger(getClass()).error("Error during Custom DB removal.", ex);

                    if (ex.getCause() != null)
                        new StacktraceDialog(this, ex.getCause().getMessage(), ex.getCause());
                    else
                        new StacktraceDialog(this, "Unexpected error when removing custom DB!", ex);
                } catch (Exception ex2) {
                    LoggerFactory.getLogger(getClass()).error("Fatal Error during Custom DB removal.", ex2);
                    new StacktraceDialog(MF, "Fatal Error during Custom DB removal.", ex2);
                }

                final String[] dbs = Jobs.runInBackgroundAndLoad(owner, "Reloading DBs...", (Callable<List<CustomDatabase>>) SearchableDatabases::getCustomDatabases).getResult()
                        .stream().map(CustomDatabase::name).toArray(String[]::new);
                dbList.setListData(dbs);
            }

        });

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(375, getMinimumSize().height));
        pack();
        setLocationRelativeTo(getParent());
        setVisible(true);
    }

    protected void whenCustomDbIsAdded(final String dbName) {
        CustomDatabase db = SearchableDatabases.getCustomDatabaseByPathOrThrow(Path.of(dbName));
        this.customDatabases.put(db.name(), db);
        dbList.setListData(this.customDatabases.keySet().stream().sorted().toArray(String[]::new));
        dbList.setSelectedValue(db.name(), true);
    }

    protected static class DatabaseView extends JPanel {
        JLabel content;

        protected DatabaseView() {
            this.content = new JLabel("No DB selected!");
            content.setHorizontalAlignment(JLabel.CENTER);
            content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
            setLayout(new BorderLayout());
            add(content, BorderLayout.CENTER);
            setPreferredSize(new Dimension(200, 240));
        }

        public void updateContent(CustomDatabase c) {
            if (c.getStatistics().getCompounds() > 0) {
                content.setText("<html><b>" + c.name() + "</b>"
                        + "<br><b>"
                        + c.getStatistics().getCompounds() + "</b> compounds with <b>" + c.getStatistics().getFormulas()
                        + "</b> different molecular formulas"
                        + (c.getStatistics().getSpectra() > 0 ? " and <b>" + c.getStatistics().getSpectra() + "</b> reference spectra." : ".")
                        + "<br>"
                        + ((c.getSettings().isInheritance() ? "<br>This database will also include all compounds from '" + DataSources.getDataSourcesFromBitFlags(c.getFilterFlag()).stream().filter(n -> !SearchableDatabases.NON_SLECTABLE_LIST.contains(n)).collect(Collectors.joining("', '")) + "'." : "")
                                + (c.needsUpgrade() ? "<br><b>This database schema is outdated. You have to upgrade the database before you can use it.</b>" : "")
                                + "</html>"));

                content.setToolTipText(c.storageLocation());
            } else {
                content.setText("Empty custom database.");
                content.setToolTipText(null);
            }
        }
    }

    protected static class DatabaseList extends JList<String> {
        protected DatabaseList(List<String> databaseList) {
            super(new Vector<>(databaseList));
            setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }

    }

}

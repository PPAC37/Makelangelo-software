/*
 * Copyright (C) 2022 Marginally Clever Robots, Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package com.marginallyclever.makelangelo;

import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.util.PreferencesHelper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 *
 * @author q6
 */
public class TranslatorOnlyMissingTableModel implements TableModel {

    private Set<String> setTKey = new HashSet<>();
    Map<String, TranslatorLanguage> languages;

    // KEY definition of the table colum REQUIRED ( Warning should have distinc value ) TODO as enum.
    final static String COL_KEY_ROW_NUM = "*";
    final static String COL_KEY_KEY = "Key";
    // SPECIAL CASE ( todo to implemente as negative colIndex to hide this to the user but to have a way to get the object used at the row ... )
    final static String SPECIAL_COL_KEY_OBJECT = "...";

    public TranslatorOnlyMissingTableModel() {
	String currentLanguage = Translator.getCurrentLanguage();
	languages = Translator.getLanguages();
//	for (String langName : languages.keySet()) {
//	    System.out.println(""+langName);
//	    // 
//	    setTKey.addAll(languages.get(langName).getStrings().keySet());
//	}
	TranslatorTableModel base = new TranslatorTableModel();
	// filtrage là où il y a un Missing ... pas trés top comme thechnique car cela va surment etre cassé quand je vais travailler sur l'autre table model ...
	for (int r=0 ; r< base.getRowCount() ; r++){
	    String tmpK = (String)base.getValueAt(r, 0);
	    for ( int c=1 ; c < base.getColumnCount() ; c++){
		String val = (String) base.getValueAt(r, c);
		if ( val != null){
		    if ( val.startsWith("Missing:")){
			setTKey.add(tmpK);
		    }
		}
	    }
	}
    }

    @Override
    public int getRowCount() {
	return setTKey.size();
    }

    @Override
    public int getColumnCount() {
	return languages.keySet().size() + 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
	switch (columnIndex) {
	    case 0:
		return "key";
	}
	if ( columnIndex-1 >=0 && columnIndex-1< languages.keySet().size()){
	    return (String)languages.keySet().toArray()[columnIndex-1];
	}
	return "" + columnIndex;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
	return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
	switch (columnIndex) {
	    case 0:
		return setTKey.toArray()[rowIndex];
	}
	if ( columnIndex-1 >=0 && columnIndex-1< languages.keySet().size()){
	    String langName = (String)languages.keySet().toArray()[columnIndex-1];
	    return languages.get(langName).get((String)setTKey.toArray()[rowIndex]);
	}

	return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    //
    //
    //
    protected void fireUpdateTableModel() {
	for (TableModelListener tml : arrayListTableModelListener) {
	    tml.tableChanged(new TableModelEvent(this));
	}
    }

    protected void fireUpdateTableModel(int row) {
	for (TableModelListener tml : arrayListTableModelListener) {
	    tml.tableChanged(new TableModelEvent(this, row));
	}
    }
    private ArrayList<TableModelListener> arrayListTableModelListener = new ArrayList<>();

    @Override
    public void addTableModelListener(TableModelListener l) {
	arrayListTableModelListener.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
	arrayListTableModelListener.remove(l);
    }

    //
    //
    //
    // test
    public static void main(String[] args) {
	Log.start();
	PreferencesHelper.start();
	CommandLineOptions.setFromMain(args);
	Translator.start();
	//
	/* Create and display the form */
	java.awt.EventQueue.invokeLater(new Runnable() {
	    public void run() {
//?                Translator.start();
		JFrame jf = new JFrame("Test " + TranslatorOnlyMissingTableModel.class.getName());

		JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
		JTable jTable1 = new javax.swing.JTable();
//        jMenuBar1 = new javax.swing.JMenuBar();
//        jMenu1 = new javax.swing.JMenu();
//        jMenu2 = new javax.swing.JMenu();

		jf.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

//        jToolBar1.setRollover(true);
//        jLabel1.setText("jLabel1");
//        jToolBar1.add(jLabel1);
		//jf.getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);
		jTable1.setAutoCreateRowSorter(true);
		jTable1.setModel(new TranslatorOnlyMissingTableModel());
		
		jTable1.setDragEnabled(true);
		jTable1.setCellSelectionEnabled(true);
		
		jScrollPane1.setViewportView(jTable1);

		jf.getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);
		jf.pack();
		jf.setVisible(true);
	    }
	});
    }
}

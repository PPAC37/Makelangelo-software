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
package com.marginallyclever.util;

import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.makelangelo.Translator;
import com.marginallyclever.makelangelo.makelangeloSettingsPanel.LanguagePreferences;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.MatchResult;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author PPAC37
 */
public class FindAllTraductionGetTableModel implements TableModel {
    
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FindAllTraductionGetTableModel.class);


    // KEY definition of the table colum REQUIRED ( Warning should have distinc value ) TODO as enum.
    final static String COL_KEY_ROW_NUM = "*";
    final static String COL_KEY_ARGS = "Traduction.get(...)";
    final static String COL_KEY_ARGS_IS_SIMPLE_STRING = "isS";
    
    final static String COL_KEY_LINE = "Line";
    final static String COL_KEY_FILE_NAME = "FileName";
    final static String COL_KEY_ARGS_TRADUCTED = "Traduction";
    final static String COL_KEY_ARGS_TRADUCTED_START_WITH_MISSING = "Traduction start with MISSING";
    // SPECIAL CASE ( todo to implemente as negative colIndex to hide this to the user but to have a way to get the object used at the row ... )
    final static String SPECIAL_COL_KEY_OBJECT = "...";
    /**
     * Adaptable just have to use an existing key ... if you add/create a key
     * you have to implmente its case in getColumnClass / isCellEditable /
     * getValueAt / setValueAt But you can remove some key this will normaly be
     * as if it hidden ...
     *
     * // The definition of the colum for the table model ( can be permuted,
     * and you can add a new key the implmentation use a getColumKey ... ) TODO
     * distinc Key Value assert. TODO ToolTips, Hidden, pos, size min max ?
     */
    protected String columKey[] = {
	COL_KEY_ROW_NUM,
	COL_KEY_ARGS,
	COL_KEY_ARGS_IS_SIMPLE_STRING,
	COL_KEY_FILE_NAME,
	COL_KEY_LINE,
	COL_KEY_ARGS_TRADUCTED_START_WITH_MISSING,
	COL_KEY_ARGS_TRADUCTED
    };

    private Map<FindAllTraductionResult, Path> map = null;

    public FindAllTraductionGetTableModel() {
	map = FindAllTraductionGet.matchTraductionGetInAllSrcJavaFiles(new File("."));
    }

    @Override
    public int getRowCount() {
	return map.size();
    }

    @Override
    public int getColumnCount() {
	return columKey.length;
    }

    /**
     * A way to get the internatColumKey used at the columIndex ... (so to avoid
     * having to implement a columTableModel ...)
     *
     * @param columnIndex
     * @return
     */
    public String getColumnKey(int columnIndex) {
	return columKey[columnIndex];
    }

    @Override
    public String getColumnName(int columnIndex) {
	final String columnKey = getColumnKey(columnIndex);
//        if (useTraslator) {
//            return Translator.get(columnKey); //TODO but in this implementation this is dangerous if the traduction do not assert distinct value for the traduction of each key ...
//        }
	return columnKey;
    }

    /**
     * To help the JTable to use adapted cell renderer/editor (and
     * sorter/comparator if the jtable have setAutoCreateRowSorter(true);)
     *
     * @param columnIndex
     * @return
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
	final String columnKey = getColumnKey(columnIndex);
	if (COL_KEY_ROW_NUM.equals(columnKey) || COL_KEY_LINE.equals(columnKey)) {
	    return Integer.class;
	}
//	else if (.equals(columnKey)) {
//            return Long.class;
//        } else if (.equals(columnKey)) {
//            return String.class;
//        }
	else if (COL_KEY_ARGS_IS_SIMPLE_STRING.equals(columnKey)
		|| COL_KEY_ARGS_TRADUCTED_START_WITH_MISSING.equals(columnKey)
		) {
            return Boolean.class;
        } 
// else if (.equals(columnKey)) {
//            return JButton.class;
//        }
	return Object.class;
    }

    /**
     *
     * @param rowIndex
     * @param columnIndex
     * @return
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
	final String columnKey = getColumnKey(columnIndex);
	if (COL_KEY_ARGS.equals(columnKey)) {
	    return true;
	}
	return false;
    }

    /**
     *
     * @param rowIndex
     * @param columnIndex
     * @return the value at row,col or null.
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
	Object obj = map.keySet().toArray()[rowIndex];
	FindAllTraductionResult mr = null;
	if (obj != null && obj instanceof FindAllTraductionResult) {
	    mr = (FindAllTraductionResult) obj;
	} else {
	    return null;
	}
	
	final String columnKey = getColumnKey(columnIndex);
	if (COL_KEY_ROW_NUM.equals(columnKey)) {
	    return rowIndex + 1;
	} else if (COL_KEY_ARGS.equals(columnKey)) {
	    return mr.argsMatch;
	} else if (COL_KEY_FILE_NAME.equals(columnKey)) {
	    return mr.pSrc.toFile().getName();
	} else if (COL_KEY_LINE.equals(columnKey)) {
	    return mr.lineInFile;
	} else if (SPECIAL_COL_KEY_OBJECT.equals(columnKey)) {
	    return mr;
	} else if (COL_KEY_ARGS_IS_SIMPLE_STRING.equals(columnKey)) {
	    return mr.isArgsMatchASimpleString();
	} else if (COL_KEY_ARGS_TRADUCTED_START_WITH_MISSING.equals(columnKey)) {
	    return mr.isTraductionStartWithMissing();
	} else if (COL_KEY_ARGS_TRADUCTED.equals(columnKey)) {
	    // 
	    String tmpS = mr.getSimpleStringFromArgs();
	    if (tmpS!=null) {
		return "\"" + Translator.get(tmpS) + "\"";
	    }

	}

	//
	return null;
    }

    /**
     * TODO ... find a way to impact a modification ... dangerous if not well
     * done ... not implemented yet.
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
	final String columnKey = getColumnKey(columnIndex);
	System.out.printf("Not implemented yet. TODO setValueAt(value=%s,row=%d,col=%d -> %s )\n", aValue, rowIndex, columnIndex, columnKey);
	if (COL_KEY_ARGS.equals(columnKey)) {
	    Object obj = map.keySet().toArray()[rowIndex];
	    FindAllTraductionResult mr = null;
	    if (obj != null && obj instanceof FindAllTraductionResult) {
		mr = (FindAllTraductionResult) obj;
	    }
	    if (mr != null) {
		System.out.printf("Not implemented yet. TODO safely modifiy %s to change at line %d, %s in %s\n", mr.pSrc, mr.lineInFile, mr.argsMatch, aValue);

		// TODO is this posible to "securly" modifiy a .java file with this ...
		// TODO but first we need to be sure that this is perfectly well done ...
		// TODO and then we may have to change the key/or create a new on in the traductions files tha used this key ...
	    }

	}
	//throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
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
    //
    // ? Notifieur / Notifié pattern implementation.
    //
    private ArrayList<TableModelListener> arrayListTableModelListener = new ArrayList<>();

    /**
     * When a JTable use a TableModel normaly the JTable register a
     * TableModelListener. This allow later to fireUpdate TableModelChange to
     * the TableModelListener JTable when the table model have change. (so the
     * JTable reload / refresh )
     *
     * @param l
     */
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
     public static void main(String[] args) {

	 Log.start();
	// lazy init to be able to purge old files
	//logger = LoggerFactory.getLogger(Makelangelo.class);

	PreferencesHelper.start();
	 CommandLineOptions.setFromMain(args);
	Translator.start();
	
	if (Translator.isThisTheFirstTimeLoadingLanguageFiles()) {
	    LanguagePreferences.chooseLanguage();
	}
	try {
	    // TODO arg 0 as dirToSearch 
	    if (args != null && args.length > 0) {
		//

	    }
	    String baseDirToSearch = "src" + File.separator + "main" + File.separator + "java";
	    System.out.printf("PDW=%s\n", new File(".").getAbsolutePath());
	    File srcDir = new File(".", baseDirToSearch);
	    try {
		System.out.printf("srcDir=%s\n", srcDir.getCanonicalPath());
	    } catch (IOException ex) {
		ex.printStackTrace();
		//Logger.getLogger(FindAllTraductionGetTableModel.class.getName()).log(Level.SEVERE, null, ex);
	    }
	    // list all .java files in srcDir.

	    List<Path> paths = FindAllTraductionGet.listFiles(srcDir.toPath(), ".java");
	    // search in the file ...
	    paths.forEach(x -> FindAllTraductionGet.searchInAFile(x, srcDir.toPath(), "Translator\\s*\\.\\s*get\\s*\\(([^\\)]*)\\)"));
	} catch (IOException ex) {
	    ex.printStackTrace();
	    //Logger.getLogger(FindAllTraductionGetTableModel.class.getName()).log(Level.SEVERE, null, ex);
	}
	
	 JFrame jf = new JFrame();
	jf.setLayout(new BorderLayout());
	 JTable jtab = new JTable(new FindAllTraductionGetTableModel());
	jtab.setAutoCreateRowSorter(true);
	 JScrollPane jsp = new JScrollPane();
	jsp.setViewportView(jtab);
	jf.getContentPane().add(jsp, BorderLayout.CENTER);
	jf.setMinimumSize(new Dimension(800, 600));
	jf.pack();
	jf.setVisible(true);
	jf.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    }

}

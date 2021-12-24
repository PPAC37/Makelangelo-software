/*
 */
package com.marginallyclever.makelangelo.paper;

//import com.marginallyclever.convenience.CommandLineOptions;
//import com.marginallyclever.convenience.log.Log;
//import com.marginallyclever.makelangelo.Translator;
//import com.marginallyclever.util.PreferencesHelper;
import java.util.ArrayList;
import java.util.TreeMap;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/**
 * A table model for Paper / PaperSize / (PaperSettings). Objectif : un moyen de
 * rajouter des format perso et de la sauvegarder / filtrer dans l'IU.
 *
 * Etat de dev : In progreese / Experimental / To review
 *
 * Remarques : As a JTable can have or not some sorter the selected Index from
 * the JTable are not to be used directly on the table model. You have to ... ?
 * use the JTable not the TableModel for GUI selection ...
 *
 *
 * For not custom PaperSize modification this create a new PaperSize (altered by
 * SetValueAt ou name appendded with "_custom") but do not alter the source.
 *
 * For custom PaperSize modification ... name / w / h + TODO
 *
 * Has a TreeSet is used to store custom Paper size normaly no double entry (if
 * le Paper Size comparable méthode is correctly done ...)
 *
 * So ther is a sort of filter in adding a new PaperSize ... (to check
 * limitation )
 *
 *
 * ToDO Plotter limites (warn out of ... ? hard limite TOP (if no pliage )?
 *
 * TODO Papper Setting info ( LandScape mode ) Marging % vs Margin Size , Paper
 * Color , Paper type ( crepon, dessin, ??? ) et grammage ( 60g , 80g ..., ) ?
 *
 * TODO import / export CVS, .ini / SGBD model ? ...
 *
 * TODO perfrenes node !
 *
 * TODO Class adaptator for multiple Sub class Type ... ? to light the actual implementted code in this Class 
 * ??? Good heritage model ....
 *
 * version 0.1 aplha 21/21/2021
 *
 * @author PPAC
 */
public class PaperTableModel implements TableModel {

    static boolean useTraslator = true;

    // KEY definition of the table colum REQUIRED
    final static String COL_KYE_PaperSizeObject = "papersize.toString()";
    final static String COL_KYE_NAME = "papersize.name";
    final static String COL_KYE_W = "papersize.w";
    final static String COL_KYE_H = "papersize.h";
    final static String COL_KYE_S = "papersize.s";
    final static String COL_KYE_isCust = "papersize.isAlterable";
    //
    final static String COL_KYE_DUP = "papersize.duplicate";
    // for special case il i want the getValueAt return not visible in the GUI special case relative to the row
    // TODO 
    final static String COL_FOR_DirectelyGetThePaperSizeObjectOfThisRow = "papersize* row relative";

    // For the user custom paper size
    private static TreeMap<PaperSize, Boolean> tmapPaperSizeGUIVisible = new TreeMap<>();
    // TODO getter setter // event fire 

    // The actual PaperSetting
    // from PaperSetting
    /**
     * Adaptable just have to use an existing key ... if you add/create a key
     * you have to implmente its case in getColumnClass / isCellEditable /
     * getValueAt / setValueAt But you can remove some key this will normaly be
     * as if it hidden ...
     *
     * // The definition of the colum for the table model ( can be permuted,
     * and you can add a new key the implmentation use a getColumKey ... )
     *
     * TODO ToolTips, Hidden, pos, size min max ?
     */
    protected String columKey[] = {
        COL_KYE_PaperSizeObject,
        COL_KYE_NAME,
        COL_KYE_W,
        COL_KYE_H,
        COL_KYE_S,
        COL_KYE_isCust// Pour les custom on peut modifier les dim, nom ...
    //,         COL_KYE_DUP
    };

    /**
     * Culumating PaperSettings.commonPaperSizes and tmapPaperSizeGUIVisible. //
     * TODO current paperSettings // TODO ? current RenderPaper ?
     *
     * @return
     */
    @Override
    public int getRowCount() {
        return (PaperSettings.commonPaperSizes.length + tmapPaperSizeGUIVisible.size());

    }

    /**
     * Adaptaive !?
     *
     * @return
     */
    @Override
    public int getColumnCount() {
        return columKey.length;
    }

    @Override
    public String getColumnName(int columnIndex) {
//        if (useTraslator) {
//            return Translator.get(getColumnKey(columnIndex)); //TODO 
//        }
        return getColumnKey(columnIndex);

    }

    public String getColumnKey(int columnIndex) {
        return columKey[columnIndex]; // Todo 
    }

    /*
    Certainely a better way to do that ...
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
//        switch (columnIndex) {
//            case 0:
//                
//                break;
//            default:
//                throw new AssertionError();
//        }
        if (COL_KYE_W.equals(getColumnKey(columnIndex)) || COL_KYE_H.equals(getColumnKey(columnIndex))) {
            return Integer.class;
        } else if (COL_KYE_S.equals(getColumnKey(columnIndex))) {
            return Long.class;
        } else if (COL_KYE_NAME.equals(getColumnKey(columnIndex))) {
            return String.class;
        } else if (COL_KYE_isCust.equals(getColumnKey(columnIndex))) {
            return Boolean.class;
        } else if (COL_KYE_DUP.equals(getColumnKey(columnIndex))) {
            return JButton.class;
        }
        return Object.class;// 
    }

    /*
    Certainely a better way to do that ...
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        if (rowIndex < PaperSettings.commonPaperSizes.length) {
            if (true) {    // dev mode
                if (COL_KYE_W.equals(getColumnKey(columnIndex)) || COL_KYE_H.equals(getColumnKey(columnIndex))) {
                    return true; //
                } else if (COL_KYE_NAME.equals(getColumnKey(columnIndex))) {
                    return true;
                } else if (COL_KYE_isCust.equals(getColumnKey(columnIndex))) {
                    return true;
                } else if (COL_KYE_S.equals(getColumnKey(columnIndex))) {
                    return false;//calculated
                } else if (COL_KYE_DUP.equals(getColumnKey(columnIndex))) {
                    return true;
                }
            }
        } else {
            return true;
        }
        return false;
        //throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /*
    Certainely a better way to do that ...
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < PaperSettings.commonPaperSizes.length) {
            if (COL_KYE_PaperSizeObject.equals(getColumnKey(columnIndex))) {
                return PaperSettings.commonPaperSizes[rowIndex].toString();
            } else if (COL_KYE_W.equals(getColumnKey(columnIndex))) {
                return PaperSettings.commonPaperSizes[rowIndex].width;
            } else if (COL_KYE_H.equals(getColumnKey(columnIndex))) {
                return PaperSettings.commonPaperSizes[rowIndex].height;
            } else if (COL_KYE_NAME.equals(getColumnKey(columnIndex))) {
                return PaperSettings.commonPaperSizes[rowIndex].name;
            } else if (COL_KYE_isCust.equals(getColumnKey(columnIndex))) {
                return false;// this is not a custom papersize.
            } else if (COL_KYE_S.equals(getColumnKey(columnIndex))) {
                return PaperSettings.commonPaperSizes[rowIndex].height * PaperSettings.commonPaperSizes[rowIndex].width;
            }
        } else {
            int relativIndex = rowIndex - PaperSettings.commonPaperSizes.length;
            if (relativIndex >= 0 && relativIndex < tmapPaperSizeGUIVisible.size()) {
                Object t = (tmapPaperSizeGUIVisible.keySet().toArray())[relativIndex];
                if (t instanceof PaperSize) {
                    PaperSize custPs = (PaperSize) t;
                    if (custPs != null) {
                        if (COL_KYE_PaperSizeObject.equals(getColumnKey(columnIndex))) {
                            return custPs.toString();
                        } else if (COL_KYE_W.equals(getColumnKey(columnIndex))) {
                            return custPs.width;
                        } else if (COL_KYE_H.equals(getColumnKey(columnIndex))) {
                            return custPs.height;
                        } else if (COL_KYE_NAME.equals(getColumnKey(columnIndex))) {
                            return custPs.name;
                        } else if (COL_KYE_isCust.equals(getColumnKey(columnIndex))) {
                            return true;// this is a custom papersize.
                        } else if (COL_KYE_S.equals(getColumnKey(columnIndex))) {
                            return custPs.height * custPs.width;
                        }
                    }
                }
            }
        }

        return "";
    }

    /*
    Certainely a better way to do that ...
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        System.out.printf("TODO com.marginallyclever.makelangelo.paper.PaperTableModel.setValueAt(val=%s,row=%d,col=%d)\n", aValue, rowIndex, columnIndex);

        if (rowIndex < PaperSettings.commonPaperSizes.length) {
            // create a dup to customize
            PaperSize source = PaperSettings.commonPaperSizes[rowIndex];
            PaperSize custom = new PaperSize(source.name + "_custom", source.width, source.height);

            //TODO fair la modif iniitallement demandé pour se nouveau PaperSize
            if (COL_KYE_PaperSizeObject.equals(getColumnKey(columnIndex))) {
                // calculated so no modification posible // throw exception ?
            } else if (COL_KYE_W.equals(getColumnKey(columnIndex))) {
                custom.width = (int) aValue;
            } else if (COL_KYE_H.equals(getColumnKey(columnIndex))) {
                custom.height = (int) aValue;
            } else if (COL_KYE_NAME.equals(getColumnKey(columnIndex))) {
                custom.name = (String) aValue;
            } else if (COL_KYE_isCust.equals(getColumnKey(columnIndex))) {
                // calculated so no modification posible // throw exception ?
            } else if (COL_KYE_S.equals(getColumnKey(columnIndex))) {
                // calculated so no modification posible // throw exception ?
            }

            // ... ? TODO a class to match properties in PapperSettings ?
            if (tmapPaperSizeGUIVisible.keySet().contains(custom)) {
                System.out.println("com.marginallyclever.makelangelo.paper.PaperTableModel.setValueAt() IGNORED");
            } else {
                tmapPaperSizeGUIVisible.put(custom, Boolean.FALSE);
                System.out.println("com.marginallyclever.makelangelo.paper.PaperTableModel.setValueAt() CREATED A NEW ROW ....");
                fireUpdateTableModel();// Full table redraw ( TODO ? rafiner c'est juste une nouvl ligne ?
            }

            // todo change JTableSelection ... pour se retrouver sur se nouvelle element.
        } else {
            int relativIndex = rowIndex - PaperSettings.commonPaperSizes.length;
            if (relativIndex >= 0 && relativIndex < tmapPaperSizeGUIVisible.size()) {
                Object t = (tmapPaperSizeGUIVisible.keySet().toArray())[relativIndex];
                if (t instanceof PaperSize) {
                    PaperSize custPs = (PaperSize) t;
                    if (custPs != null) {
                        if (COL_KYE_PaperSizeObject.equals(getColumnKey(columnIndex))) {
                            //
                        } else if (COL_KYE_W.equals(getColumnKey(columnIndex))) {
                            custPs.width = (int) aValue;
                        } else if (COL_KYE_H.equals(getColumnKey(columnIndex))) {
                            custPs.height = (int) aValue;
                        } else if (COL_KYE_NAME.equals(getColumnKey(columnIndex))) {
                            custPs.name = (String) aValue;
                        } else if (COL_KYE_isCust.equals(getColumnKey(columnIndex))) {
                            //
                        } else if (COL_KYE_S.equals(getColumnKey(columnIndex))) {
                            //
                        }
                        fireUpdateTableModel(rowIndex);//To refresh calculated value in this row if anny change... // do not redo the sorting if any
                        //
                        fireUpdateTableModel();// to redo the sorting if any
                        System.out.println("com.marginallyclever.makelangelo.paper.PaperTableModel.setValueAt() May have CHANGE A PAPER SIZE Custom values ....");
                    }
                }
            } else {
                System.out.println("com.marginallyclever.makelangelo.paper.PaperTableModel.setValueAt() TODO ? ....");
            }
        }
        System.out.printf("TODO com.marginallyclever.makelangelo.paper.PaperTableModel.setValueAt(val=%s,row=%d,col=%d) END !\n", aValue, rowIndex, columnIndex);

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
//        Log.start();
//        PreferencesHelper.start();
//        CommandLineOptions.setFromMain(args);
//        Translator.start();
        //
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
//?                Translator.start();
                JFrame jf = new JFrame("Test " + PaperTableModel.class.getName());

                JScrollPane jScrollPane1 = new javax.swing.JScrollPane();
                JTable jTable1 = new javax.swing.JTable();
//        jMenuBar1 = new javax.swing.JMenuBar();
//        jMenu1 = new javax.swing.JMenu();
//        jMenu2 = new javax.swing.JMenu();

                jf.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

//        jToolBar1.setRollover(true);
//        jLabel1.setText("jLabel1");
//        jToolBar1.add(jLabel1);
                //jf.getContentPane().add(jToolBar1, java.awt.BorderLayout.NORTH);
                jTable1.setAutoCreateRowSorter(true);
                jTable1.setModel(new PaperTableModel());
                jScrollPane1.setViewportView(jTable1);

                jf.getContentPane().add(jScrollPane1, java.awt.BorderLayout.CENTER);
                jf.pack();
                jf.setVisible(true);
            }
        });
    }
}

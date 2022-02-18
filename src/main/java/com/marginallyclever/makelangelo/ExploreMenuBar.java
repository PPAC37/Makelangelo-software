/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.marginallyclever.makelangelo;

import java.awt.event.KeyListener;
import javax.swing.ActionMap;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.MenuElement;

/**
 *
 * @author q6
 */
public class ExploreMenuBar {
	
	protected static String exploreSubElementsMdHeader = "|class|name|text|mnemo|accelerator|\n|-|-|-|-|-|\n";
	
	public static void exploreSubElements(MenuElement[] subElements){
		exploreSubElements(subElements, 0);
	}
	
	protected static void exploreSubElements(MenuElement[] subElements, int level) {
			if ( level == 0 ){
				System.out.print(exploreSubElementsMdHeader);
			}
			if (subElements != null) {
				String slevel = level > 0 ? String.format("%"+(level)+"s", "") : "";
				//slevel = slevel.replace(" ","&nbsp;");
				//System.out.println(slevel + subElements.length);
				for ( MenuElement kl : subElements){
					
					
					if ( kl instanceof JMenuItem){
						JMenuItem jmi = (JMenuItem) kl;
							final String text = jmi.getText();
							final String name = jmi.getName();
						
							System.out.printf("|%s`%-"+(40-level)+"s`|`%-20s`|`%-30s`|%s|",slevel,kl.getClass().getName(),name,text,jmi.getMnemonic()==0?"":(char)jmi.getMnemonic());
						KeyStroke accelerator = jmi.getAccelerator();
						if ( accelerator != null){
							System.out.print( accelerator.toString()+"|");
						}
						System.out.println();
					} else if ( kl instanceof JPopupMenu){
						JPopupMenu jpm = (JPopupMenu) kl;
						System.out.printf("|%s`%-"+(40-level)+"s`|||||%dx%d|\n",slevel,kl.getClass().getName(),jpm.getWidth(),jpm.getHeight());
					}
						else{
						System.out.printf("|%s`%-"+(40-level)+"s`|||||\n",slevel,kl.getClass().getName(),kl);
					}
					exploreSubElements(kl.getSubElements(),level+1);
				}
			}
		}
	
	public static void reportJFrameActionMenuBar(JFrame jframe){
		
			JMenuBar jMenuBar = jframe.getJMenuBar();
			
			// ?
			ActionMap actionMap = jMenuBar.getActionMap();
			if (actionMap != null) {
				System.out.println("" + actionMap.size());
				if ( actionMap.size() > 0 )
				for (Object ok : actionMap.keys()) {
					System.out.println(" " + ok.toString());
				}
			}
			
			// F10 
			KeyStroke[] registeredKeyStrokes = jMenuBar.getRegisteredKeyStrokes();
			if (registeredKeyStrokes != null) {
				System.out.println("" + registeredKeyStrokes.length);
				for ( KeyStroke ks : registeredKeyStrokes){
					System.out.println(" " + ks.toString());
				}
			}
			
			// ?
			KeyListener[] keyListeners = jMenuBar.getKeyListeners();
			if (keyListeners != null) {
				System.out.println("" + keyListeners.length);
				for ( KeyListener kl : keyListeners){
					System.out.println(" " + kl.toString());
				}
			}
			
			// !
			MenuElement[] subElements = jMenuBar.getSubElements();
			exploreSubElements(subElements);
	}
}

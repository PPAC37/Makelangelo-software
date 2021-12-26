package com.marginallyclever.makelangelo.select;

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.AbstractAction;
import javax.swing.JButton;

/**
 * A button that does nothing until you attach an observer.
 * @author Dan Royer
 * @since 7.24.0
 */
public class SelectButton extends Select {
	/**
	 * 
	 */
	private static final long serialVersionUID = 7096181261934634708L;
	private JButton button = new JButton();

	public SelectButton(String internalName,AbstractAction action) {
		super(internalName);
		button.setAction(action);
	}
	
	public SelectButton(String internalName,String labelText) {
		super(internalName);
		
		button.setText(labelText);
		button.addActionListener((e) -> {
			// TODO this should be notifyActionEventListeners()?
			notifyPropertyChangeListeners(null,null);
		});

		this.add(button,BorderLayout.CENTER);
	}
	
	public void doClick() {
		if(button!=null) button.doClick();
	}
	
	public void setText(String label) {
		if(button!=null) button.setText(label);
	}

	@Override
	public void setEnabled(boolean b) {
		if(button!=null) button.setEnabled(b);
	}

	@Override
	public void setForeground(Color fg) {
		if(button!=null) button.setForeground(fg);
	}
}

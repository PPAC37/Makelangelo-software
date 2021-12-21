package com.marginallyclever.makelangelo;

import javax.swing.*;
import javax.swing.event.ChangeEvent;

import org.apache.batik.ext.swing.GridBagConstants;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.Serial;

import com.marginallyclever.convenience.CommandLineOptions;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.makelangelo.turtle.Turtle;
import com.marginallyclever.util.PreferencesHelper;

public class ScaleTurtlePanel extends JPanel {
	/**
	 * 
	 */
	@Serial
	private static final long serialVersionUID = -4566997988723228869L;

	private final Turtle originalTurtle;
	private final Turtle myTurtle;
	private final JSpinner width;
	private final JSpinner height;
	private final JComboBox<String> units = new JComboBox<String>(new String[]{"mm","%"});
	private final JCheckBox lockRatio = new JCheckBox("🔒");
	private final Rectangle2D.Double myOriginalBounds;
	private double ratioAtTimeOfLock=1;
	private boolean ignoreChange=false;
	
	public ScaleTurtlePanel(Turtle t) {
		super();
		originalTurtle = t;
		myTurtle=new Turtle(t);

		myOriginalBounds = myTurtle.getBounds();
		width = new JSpinner(new SpinnerNumberModel(myOriginalBounds.width,null,null,1));
		height = new JSpinner(new SpinnerNumberModel(myOriginalBounds.height,null,null,1));
		
		setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets=new Insets(10,10,3,10);

		c.gridx=0;
		c.gridy=0;
		c.weightx=1;
		c.anchor=GridBagConstants.NORTHWEST;
		add(new JLabel(Translator.get("Width")),c);
		
		c.gridx=0;
		c.gridy=1;
		c.anchor=GridBagConstants.NORTHWEST;
		add(new JLabel(Translator.get("Height")),c);
		
		c.gridx=1;
		c.gridy=0;
		c.anchor=GridBagConstants.NORTHWEST;
		c.fill=GridBagConstants.HORIZONTAL;
		add(width,c);

		c.gridx=1;
		c.gridy=1;
		c.anchor=GridBagConstants.NORTHWEST;
		c.fill=GridBagConstants.HORIZONTAL;
		add(height,c);
		
		c.gridx=2;
		c.gridy=0;
		c.gridheight=2;
		c.anchor=GridBagConstants.CENTER;
		add(lockRatio,c);
		
		c.gridx=3;
		c.gridy=0;
		c.gridheight=2;
		c.anchor=GridBagConstants.CENTER;
		add(units,c);
		
		width.addChangeListener(this::onWidthChange);
		height.addChangeListener(this::onHeightChange);
		units.addActionListener(this::onUnitChange);
		lockRatio.addActionListener(e -> onLockChange());
		lockRatio.setSelected(true);
		onLockChange();

		updateMinimumWidth(width);
		updateMinimumWidth(height);
	}
	
	private void updateMinimumWidth(JSpinner spinner) {
		JComponent field = spinner.getEditor();
	    Dimension prefSize = field.getPreferredSize();
	    prefSize = new Dimension(80, prefSize.height);
	    field.setPreferredSize(prefSize);
	}

	private void onWidthChange(ChangeEvent e) {
		if(lockRatio.isSelected()) {
			double w1 = (Double)width.getValue();
			height.setValue(w1 / ratioAtTimeOfLock);
		}
		if(!ignoreChange) scaleNow();
	}

	private void onHeightChange(ChangeEvent e) {
		if(lockRatio.isSelected()) {
			double h1 = (Double)height.getValue();
			width.setValue(h1 * ratioAtTimeOfLock);
		}
		if(!ignoreChange) scaleNow();
	}
	
	private void scaleNow() {
		double ow = myOriginalBounds.getWidth();
		double oh = myOriginalBounds.getHeight();
		ow = (ow == 0) ? 1 : ow;
		oh = (oh == 0) ? 1 : oh;
		
		double w1 = (Double)width.getValue();
		double h1 = (Double)height.getValue();
		if(units.getSelectedIndex()==0) {
			// mm
			w1 /= ow;
			h1 /= oh;			
		} else {
			// %
			w1*=0.01;
			h1*=0.01;
		}

		Log.message("new scale="+w1+" by "+h1);
		revertOriginalTurtle();
		originalTurtle.scale(w1, h1);
	}

	private void revertOriginalTurtle() {
		// reset original turtle to original scale.
		originalTurtle.history.clear();
		originalTurtle.history.addAll(myTurtle.history);
	}
	
	private void onUnitChange(ActionEvent e) {
		double ow = myOriginalBounds.getWidth();
		double oh = myOriginalBounds.getHeight();
		ow = (ow == 0) ? 1 : ow;
		oh = (oh == 0) ? 1 : oh;

		double w1 = (Double)width.getValue();
		double h1 = (Double)height.getValue();

		ignoreChange=true;
		if(units.getSelectedIndex()==0) {
			// switching to mm
			width.setValue(w1*0.01 * ow);
			height.setValue(h1*0.01 * oh);
		} else {
			// switching to %
			width.setValue(100.0*w1 / ow);
			height.setValue(100.0*h1 / oh);
		}
		ignoreChange=false;
	}

	private void onLockChange() {
		if(lockRatio.isSelected()) {
			ratioAtTimeOfLock = (Double)width.getValue() / (Double)height.getValue();
		}
	}

	public static void runAsDialog(JFrame frame,Turtle t) {
		ScaleTurtlePanel panel = new ScaleTurtlePanel(t);

		JDialog dialog = new JDialog(frame,Translator.get("Scale"));

		JButton okButton = new JButton(Translator.get("OK"));
		JButton cancelButton = new JButton(Translator.get("Cancel"));

		JPanel outerPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx=0;
		c.gridy=0;
		c.gridwidth=3;
		c.anchor=GridBagConstraints.NORTHWEST;
		c.fill=GridBagConstraints.BOTH;
		outerPanel.add(panel,c);

		c.gridx=1;
		c.gridy=1;
		c.gridwidth=1;
		c.weightx=1;
		outerPanel.add(okButton,c);
		c.gridx=2;
		c.gridwidth=1;
		c.weightx=1;
		outerPanel.add(cancelButton,c);
		
		okButton.addActionListener((e)-> dialog.dispose());
		cancelButton.addActionListener((e)-> {
			panel.revertOriginalTurtle();
			dialog.dispose();
		});
		
		dialog.add(outerPanel);
		dialog.pack();
		dialog.setVisible(true);
	}
	
	// TEST
	
	public static void main(String[] args) {
		Log.start();
		PreferencesHelper.start();
		CommandLineOptions.setFromMain(args);
		Translator.start();

		JFrame frame = new JFrame(ScaleTurtlePanel.class.getSimpleName());
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		runAsDialog(frame,new Turtle());
	}

}

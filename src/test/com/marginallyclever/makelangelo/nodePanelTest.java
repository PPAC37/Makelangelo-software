package com.marginallyclever.makelangelo;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.ServiceLoader;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.junit.Test;

import com.marginallyclever.core.log.Log;
import com.marginallyclever.core.node.Node;
import com.marginallyclever.core.node.NodePanel;

public class nodePanelTest {
	
	@Test
	static public void testOnePanel() throws Exception {
		System.out.println("testOnePanel() start");
		Log.start();
		Translator.start();
		JFrame frame = new JFrame("Node panel test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//frame > 
		//  combo box > names of nodes
		//  cards > one card for each node
		frame.setLayout(new BorderLayout());
		JPanel comboBoxPane = new JPanel();
		JPanel cards = new JPanel(new CardLayout());
		frame.add(comboBoxPane, BorderLayout.PAGE_START);
		frame.add(cards, BorderLayout.CENTER);

		// build combo box
		ArrayList<String> names = new ArrayList<String>();
		ServiceLoader<Node> nodes = ServiceLoader.load(Node.class);
		for( Node n : nodes ) names.add(n.getName());
		String [] comboBoxItems = names.toArray(new String[] {});
		JComboBox<String> cb = new JComboBox<String>(comboBoxItems);
		cb.setEditable(false);
		// when combo box changes, visible card changes. 
		cb.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent evt) {
			    CardLayout cl = (CardLayout)(cards.getLayout());
			    cl.show(cards, (String)evt.getItem());
			}
		});
		comboBoxPane.add(cb);

		// build cards
		for( Node n : nodes ) {
			NodePanel p = new NodePanel(n);
			p.buildInputPanel();

			cards.add(n.getName(),p);
		}
		
		frame.pack();
		frame.setVisible(true);
		System.out.println("testOnePanel() end");
	}
}

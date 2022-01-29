package com.marginallyclever.makelangelo.select;

import com.marginallyclever.makelangelo.DialogAbout;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.swing.JEditorPane;

import javax.swing.JLabel;
import javax.swing.ToolTipManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read only HTML-rich text that can display AT MOST one clickable link.
 * @author Dan Royer
 * @since 7.24.0
 */
public class SelectReadOnlyText extends Select {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8918068053490064344L;
	
	private static final Logger logger = LoggerFactory.getLogger(SelectReadOnlyText.class);
	
	/*
	private static final String A_HREF = "<a href=";
	private static final String HREF_CLOSED = ">";
	*/
	
	//private JLabel label;// TODO as in DialogAbout a JEditorPane (setEditable(false)) with a 
	private JEditorPane jEdPane;
	
	public SelectReadOnlyText(String internalName,String labelKey) {
		super(internalName);
		
		/*
		label = new JLabel("<html>"+ labelKey+"</html>",JLabel.LEADING);
		label.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent evt) {
		        String text = label.getText();
		        try {
		        	String link = getPlainLink(text);
		        	if(link==null) return;
		            URI uri = new java.net.URI(link);
		            Desktop.getDesktop().browse(uri);
		        } catch (URISyntaxException | IOException e) {
		            throw new AssertionError(e.getMessage() + ": " + text); //NOI18N
		        }
			}
		});
		this.add(label,BorderLayout.CENTER);
		*/
		
	    String sToSetAsTextToTheHtmlEditorPane = "<html>"+ labelKey+"</html>";
		jEdPane = new JEditorPane();
		jEdPane.setEditable(false);
		jEdPane.setOpaque(false);
		jEdPane.setContentType("text/html");
		jEdPane.setText(sToSetAsTextToTheHtmlEditorPane);
		jEdPane.addHyperlinkListener(new HyperlinkListener() {
		    @Override
		    public void hyperlinkUpdate(HyperlinkEvent hyperlinkEvent) {
			HyperlinkEvent.EventType eventType = hyperlinkEvent.getEventType();
			if (eventType == HyperlinkEvent.EventType.ACTIVATED) {
					if (Desktop.isDesktopSupported()) {
						try {
							URI u = hyperlinkEvent.getURL().toURI();
							Desktop desktop = Desktop.getDesktop();
							if ( desktop.isSupported(Desktop.Action.BROWSE)){
							    logger.debug("Desktop.Action.BROWSE {}", u);
							    desktop.browse(u);
							}else{
							    logger.error("Desktop.Action.BROWSE not supported. Cant browse {}", u);
							}
						} catch (IOException | URISyntaxException e) {
							logger.error("Failed to open the browser to the url", e);
						}
					}else{
					    logger.error("Desktop not supported. Cant browse {}", hyperlinkEvent.getURL());
					}
				}
			else if ( eventType == HyperlinkEvent.EventType.ENTERED ){
			    jEdPane.setToolTipText(hyperlinkEvent.getURL().toExternalForm());
			    ToolTipManager.sharedInstance().setInitialDelay(0);
			    ToolTipManager.sharedInstance().setDismissDelay(5000);
			}
			else if ( eventType == HyperlinkEvent.EventType.EXITED ){
			    jEdPane.setToolTipText(null);// null to turn off the tooltips.
			    //ToolTipManager.sharedInstance().setInitialDelay(0);
			    //ToolTipManager.sharedInstance().setDismissDelay(500);
			}
		    }
		});
		this.add(jEdPane,BorderLayout.CENTER);
	}
	
	/*
	private String getPlainLink(String s) {
		int first = s.indexOf(A_HREF);
		if(first<0) return null;
		int last = s.indexOf(HREF_CLOSED,first);
		if(last<0) return null;
	    return s.substring(first + A_HREF.length()+1, last-1);
	}
	*/
}

package net.schwehla.matrosdms.rcp.swt.popupshell.text.navigation;

import java.net.URL;

import org.eclipse.swt.graphics.Rectangle;

/**
 * Hyperlink is a class representing a section of clickable text.  It is a
 * composite of a Token, representing a URL, and a rectangular boundary.  
 */
public class Hyperlink {
	private String token;
	private Rectangle bounds;
	
	/**
	 * Creates a Hyperlink with the given token and visual boundary.
	 * 
	 * @param token : {@link TextToken}
	 * @param bounds : {@link Rectangle}
	 */
	public Hyperlink(final String token, final Rectangle bounds) {
		this.token = token;
		this.bounds = bounds;
	}
	
	/**
	 * Gets the URL from the internal Token.
	 * @return {@link URL} 
	 */
	public URL getURL() {
		return null; // token.getURL();
	}
	
	/**
	 * Test whether a coordinate is within the Hyperlink boundary.
	 * @return boolean
	 */
	public boolean contains(final int x, final int y) {
		return bounds.contains(x, y);
	}
}

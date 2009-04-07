package org.hibernate.search.cfg;

import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.StringBridge;

/**
 * @author Emmanuel Bernard
 */
public class ConcatStringBridge implements StringBridge, ParameterizedBridge{
	public static final String SIZE = "size";
	private int size;

	public String objectToString(Object object) {
		if (object == null) return "";
		if ( ! (object instanceof String) ) {
			throw new RuntimeException( "not a string" );
		}
		String string = object.toString();
		int maxSize = string.length() >= size ? size : string.length();
		return string.substring( 0, maxSize );
	}

	public void setParameterValues(Map parameters) {
		size =  Integer.valueOf( (String) parameters.get( SIZE ) );
	}
}

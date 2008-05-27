//$Id$
package org.hibernate.search.test.bridge;

import java.util.Map;

import org.hibernate.search.bridge.ParameterizedBridge;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * Padding Integer bridge.
 * All numbers will be padded with 0 to match 5 digits
 *
 * @author Emmanuel Bernard
 */
public class PaddedIntegerBridge implements TwoWayStringBridge, ParameterizedBridge {

	public static final String PADDING_PROPERTY = "padding";

	private int padding = 5; //default

	public void setParameterValues(Map parameters) {
		Object padding = parameters.get( PADDING_PROPERTY );
		if (padding != null) this.padding = (Integer) padding;
	}

	public String objectToString(Object object) {
		String rawInteger = object.toString();
		if (rawInteger.length() > padding) throw new IllegalArgumentException( "Try to pad on a number too big" );
		StringBuilder paddedInteger = new StringBuilder( );
		for ( int padIndex = rawInteger.length() ; padIndex < padding ; padIndex++ ) {
			paddedInteger.append('0');
		}
		return paddedInteger.append( rawInteger ).toString();
	}

	public Object stringToObject(String stringValue) {
		return new Integer(stringValue);
	}
}

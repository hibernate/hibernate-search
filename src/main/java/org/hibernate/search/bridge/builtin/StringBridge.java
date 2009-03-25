//$Id$
package org.hibernate.search.bridge.builtin;

/**
 * Map a string element
 *
 * @author Emmanuel Bernard
 */
public class StringBridge implements org.hibernate.search.bridge.TwoWayStringBridge {
	public Object stringToObject(String stringValue) {
		return stringValue;
	}

	public String objectToString(Object object) {
		return (String) object;
	}
}

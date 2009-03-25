//$Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.util.StringHelper;

/**
 * Map an integer element
 *
 * @author Emmanuel Bernard
 */
public class IntegerBridge extends NumberBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		return new Integer( stringValue );
	}
}

//$Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.util.StringHelper;

/**
 * Map a float element
 *
 * @author Emmanuel Bernard
 */
public class FloatBridge extends NumberBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		return new Float( stringValue );
	}
}

//$Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.util.StringHelper;

/**
 * Map a long element
 *
 * @author Emmanuel Bernard
 */
public class LongBridge extends NumberBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		return new Long( stringValue );
	}
}

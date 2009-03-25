//$Id$
package org.hibernate.search.bridge.builtin;

import org.hibernate.util.StringHelper;

/**
 * Map a short element
 *
 * @author Emmanuel Bernard
 */
public class ShortBridge extends NumberBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		return new Short( stringValue );
	}
}

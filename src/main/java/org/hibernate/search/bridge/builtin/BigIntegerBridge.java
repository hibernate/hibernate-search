//$Id$
package org.hibernate.search.bridge.builtin;

import java.math.BigInteger;

import org.hibernate.util.StringHelper;

/**
 * Map a <code>BigInteger</code> element.
 *
 * @author Emmanuel Bernard
 */
public class BigIntegerBridge extends NumberBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) {
			return null;
		}
		return new BigInteger( stringValue );
	}
}

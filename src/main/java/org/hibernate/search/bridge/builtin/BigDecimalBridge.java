//$Id$
package org.hibernate.search.bridge.builtin;

import java.math.BigDecimal;

import org.hibernate.util.StringHelper;

/**
 * Map a BigDecimal element
 *
 * @author Emmanuel Bernard
 */
public class BigDecimalBridge extends NumberBridge {
	public Object stringToObject(String stringValue) {
		if ( StringHelper.isEmpty( stringValue ) ) return null;
		return new BigDecimal( stringValue );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public class NumericBridgeProvider extends ExtendedBridgeProvider {
	private static final Map<String, NumericFieldBridge> numericBridges;

	static {
		numericBridges = new HashMap<String, NumericFieldBridge>();
		numericBridges.put( Integer.class.getName(), NumericFieldBridge.INT_FIELD_BRIDGE );
		numericBridges.put( int.class.getName(), NumericFieldBridge.INT_FIELD_BRIDGE );
		numericBridges.put( Long.class.getName(), NumericFieldBridge.LONG_FIELD_BRIDGE );
		numericBridges.put( long.class.getName(), NumericFieldBridge.LONG_FIELD_BRIDGE );
		numericBridges.put( Double.class.getName(), NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		numericBridges.put( double.class.getName(), NumericFieldBridge.DOUBLE_FIELD_BRIDGE );
		numericBridges.put( Float.class.getName(), NumericFieldBridge.FLOAT_FIELD_BRIDGE );
		numericBridges.put( float.class.getName(), NumericFieldBridge.FLOAT_FIELD_BRIDGE );
	}

	@Override
	public FieldBridge provideFieldBridge(ExtendedBridgeProviderContext bridgeContext) {
		if ( bridgeContext.getNumericField() != null ) {
			return numericBridges.get( bridgeContext.getReturnType().getName() );
		}
		return null;
	}
}

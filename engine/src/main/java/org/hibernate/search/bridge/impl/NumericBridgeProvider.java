/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
class NumericBridgeProvider extends ExtendedBridgeProvider {

	private static final Map<String, NumericFieldBridge> numericBridges;

	static {
		numericBridges = new HashMap<>();
		numericBridges.put( Byte.class.getName(), NumericFieldBridge.BYTE_FIELD_BRIDGE );
		numericBridges.put( byte.class.getName(), NumericFieldBridge.BYTE_FIELD_BRIDGE );
		numericBridges.put( Short.class.getName(), NumericFieldBridge.SHORT_FIELD_BRIDGE );
		numericBridges.put( short.class.getName(), NumericFieldBridge.SHORT_FIELD_BRIDGE );
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
		if ( numericBridges.containsKey( bridgeContext.getReturnType().getName() ) ) {

			// document id should only be indexed numerically in case there is an explicit @NumericField
			if ( bridgeContext.isId() && !bridgeContext.getAnnotatedElement().isAnnotationPresent( NumericField.class ) ) {
				return null;
			}

			return numericBridges.get( bridgeContext.getReturnType().getName() );
		}
		return null;
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.util.impl.CollectionHelper;

/**
 * @author Emmanuel Bernard
 */
class NumericBridgeProvider extends ExtendedBridgeProvider {

	private static final Map<String, NumericFieldBridge> numericBridges;

	/**
	 * Those numeric types for which a String field will be used by default; Only if explicitly marked via {@code NumericField}
	 * they will be encoded numerically.
	 */
	// TODO HSEARCH-1779 Remove and use numeric fields for all number types by default
	private static final Set<Class<?>> TYPES_USING_STRING_FIELD_BY_DEFAULT = CollectionHelper.<Class<?>>asSet( Short.class, short.class, Byte.class, byte.class );

	static {
		numericBridges = new HashMap<>( 12 );
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
		// For id and short/byte use numeric fields only if explicitly requested via @NumericField
		if ( !bridgeContext.isExplicitlyMarkedAsNumeric() &&
				( bridgeContext.isId() || encodeWithStringFieldByDefault( bridgeContext.getReturnType() ) ) ) {
			return null;
		}

		return numericBridges.get( bridgeContext.getReturnType().getName() );
	}

	private boolean encodeWithStringFieldByDefault(Class<?> clazz) {
		return TYPES_USING_STRING_FIELD_BY_DEFAULT.contains( clazz );
	}
}

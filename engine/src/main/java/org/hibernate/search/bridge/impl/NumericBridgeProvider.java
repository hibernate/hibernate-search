/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.builtin.EnumBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.spi.BridgeProvider;

/**
 * Provide a bridge for enums.
 *
 * @author Emmanuel Bernard
 */
class EnumBridgeProvider implements BridgeProvider {
	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		if ( bridgeProviderContext.getReturnType().isEnum() ) {
			//we return one enum type bridge instance per property as it is customized per ReturnType
			final EnumBridge enumBridge = new EnumBridge();
			//AppliedOnTypeAwareBridge is called in a generic way later and fills up the enum type to the bridge
			return new TwoWayString2FieldBridgeAdaptor( enumBridge );
		}
		return null;
	}
}

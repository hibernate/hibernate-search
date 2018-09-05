/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge.provider;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.StringBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.bridge.util.impl.String2FieldBridgeAdaptor;

/**
 * @author Emmanuel Bernard
 */
public class TheaterBridgeProvider2 implements BridgeProvider {
	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		if ( bridgeProviderContext.getReturnType().equals( Theater.class ) ) {
			return new String2FieldBridgeAdaptor( new StringBridge() {

				@Override
				public String objectToString(Object object) {
					return ( (Theater) object ).toString();
				}
			} );
		}
		return null;
	}
}

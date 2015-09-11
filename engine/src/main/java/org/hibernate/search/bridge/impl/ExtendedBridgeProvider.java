/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.bridge.impl;

import java.lang.reflect.AnnotatedElement;

import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Internal contract extending {@code BridgeProvider} to handle some of the
 * specificity of Hibernate Search bridges (like annotations requirements).
 *
 * @author Emmanuel Bernard
 */
abstract class ExtendedBridgeProvider implements BridgeProvider {

	/**
	 * Same as {@link org.hibernate.search.bridge.spi.BridgeProvider#provideFieldBridge(org.hibernate.search.bridge.spi.BridgeProvider.BridgeProviderContext)}
	 * but accepts an extended context.
	 */
	public abstract FieldBridge provideFieldBridge(ExtendedBridgeProviderContext bridgeContext);

	@Override
	public FieldBridge provideFieldBridge(BridgeProviderContext bridgeProviderContext) {
		if ( ! ( bridgeProviderContext instanceof ExtendedBridgeProviderContext ) ) {
			throw new AssertionFailure( "We should always receive an ExtendedBridgeProviderContext instance: " + bridgeProviderContext
					.getClass() );
		}
		return provideFieldBridge( (ExtendedBridgeProviderContext) bridgeProviderContext );
	}

	interface ExtendedBridgeProviderContext extends BridgeProviderContext {

		/**
		 * Offers access to the annotations hosted on the member seeking a bridge.
		 */
		AnnotatedElement getAnnotatedElement();

		/**
		 * Return the member name for log and exception report purposes.
		 */
		String getMemberName();

		/**
		 * @return {@code true} if the indexed type is the document id, {@code false} otherwise.
		 */
		boolean isId();

		/**
		 * Whether the field in question is marked as numeric field by means of the {code NumericField} annotation or
		 * not.
		 */
		boolean isExplicitlyMarkedAsNumeric();
	}
}

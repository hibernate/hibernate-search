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
import org.hibernate.search.bridge.spi.BridgeProvider.BridgeProviderContext;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Internal contract extending {@code BridgeProvider} to handle some of the
 * specificity of Hibernate Search bridges (like annotations requirements).
 *
 * @author Emmanuel Bernard
 */
public abstract class ExtendedBridgeProvider implements BridgeProvider {

	/**
	 * Same as {@link org.hibernate.search.bridge.spi.BridgeProvider#provideFieldBridge(org.hibernate.search.bridge.spi.BridgeProvider.BridgeProviderContext)}
	 * but accepts an extended context.
	 * @param bridgeContext the {@link ExtendedBridgeProviderContext}
	 * @return a {@link org.hibernate.search.bridge.FieldBridge} instance if the provider can
	 * build a bridge for the calling context. {@code null} otherwise.
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

	public interface ExtendedBridgeProviderContext extends BridgeProviderContext {

		/**
		 * @return The {@link AnnotatedElement} for the member seeking a bridge,
		 * offering access to its annotations.
		 */
		AnnotatedElement getAnnotatedElement();

		/**
		 * @return the member name for log and exception report purposes.
		 */
		String getMemberName();

		/**
		 * @return {@code true} if the indexed type is the document id, {@code false} otherwise.
		 */
		boolean isId();

		/**
		 * @return whether the field in question is marked as numeric field by means of the {code NumericField} annotation or
		 * not.
		 */
		boolean isExplicitlyMarkedAsNumeric();

		/**
		 * Returns the type of the indexed member/property; it works for arrays and collections too.
		 *
		 * @see #getReturnType()
		 * @return the type of the indexed member
		 */
		Class<?> getElementOrContainerReturnType();
	}
}

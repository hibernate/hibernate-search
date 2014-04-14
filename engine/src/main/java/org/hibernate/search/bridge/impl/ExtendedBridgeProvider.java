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

import java.lang.reflect.AnnotatedElement;

import org.hibernate.search.annotations.NumericField;
import org.hibernate.search.bridge.FieldBridge;
import org.hibernate.search.bridge.spi.BridgeProvider;
import org.hibernate.search.exception.AssertionFailure;

/**
 * Internal contract extending {@code BridgeProvider} to handle some of the
 * specificity of Hibernate Search bridges (like annotations requirements).
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public abstract class ExtendedBridgeProvider implements BridgeProvider {

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
		 * Return the appropriate {@link org.hibernate.search.annotations.NumericField} annotation
		 * if present.
		 */
		NumericField getNumericField();
	}
}

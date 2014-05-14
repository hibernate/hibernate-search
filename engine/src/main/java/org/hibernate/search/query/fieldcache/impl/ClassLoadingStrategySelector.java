/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.fieldcache.impl;

import org.hibernate.search.bridge.builtin.NumericFieldBridge;
import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.TwoWayStringBridge;

/**
 * A {@code FieldCacheCollectorFactory} requires two parameters which are inferred from
 * the type of field and its applied bridges. Not all cases can be covered.
 * This class contains helpers to extract the proper parameters to create such a factory.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public final class ClassLoadingStrategySelector {

	private ClassLoadingStrategySelector() {
		//not allowed
	}

	public static FieldCacheLoadingType guessAppropriateCollectorType(TwoWayFieldBridge fieldBridge) {
		if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
			NullEncodingTwoWayFieldBridge encoding = (NullEncodingTwoWayFieldBridge) fieldBridge;
			return guessAppropriateCollectorType( encoding.unwrap() );
		}
		else if ( fieldBridge instanceof TwoWayString2FieldBridgeAdaptor ) {
			return FieldCacheLoadingType.STRING;
		}
		else if ( fieldBridge instanceof NumericFieldBridge ) {
			return ((NumericFieldBridge) fieldBridge ).getFieldCacheLoadingType();
		}
		else {
			// we don't know how to extract this: no fieldCache will be available
			return null;
		}
	}

	/**
	 * Extracts (if possible) the two way string bridge from a given two way field bridge
	 *
	 * @param fieldBridge the field bridge from which to extract (unwrap) the two way string bridge
	 * @return the underlying string bridge or {@code null} if we can't extract it
	 */
	public static TwoWayStringBridge getTwoWayStringBridge(TwoWayFieldBridge fieldBridge) {
		if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
			NullEncodingTwoWayFieldBridge encoding = (NullEncodingTwoWayFieldBridge) fieldBridge;
			return getTwoWayStringBridge( encoding.unwrap() );
		}
		else if ( fieldBridge instanceof TwoWayString2FieldBridgeAdaptor ) {
			TwoWayString2FieldBridgeAdaptor adaptor = (TwoWayString2FieldBridgeAdaptor) fieldBridge;
			return adaptor.unwrap();
		}
		else {
			return null;
		}
	}
}

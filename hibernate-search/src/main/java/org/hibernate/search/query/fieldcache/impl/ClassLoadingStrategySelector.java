/* 
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

package org.hibernate.search.query.fieldcache.impl;

import org.hibernate.search.bridge.builtin.impl.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.builtin.impl.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.DoubleNumericFieldBridge;
import org.hibernate.search.bridge.builtin.FloatNumericFieldBridge;
import org.hibernate.search.bridge.builtin.IntegerNumericFieldBridge;
import org.hibernate.search.bridge.builtin.LongNumericFieldBridge;

/**
 * A {@code FieldCacheCollectorFactory} requires two parameters which are inferred from
 * the type of field and it's applied bridges. Not all cases can be covered.
 * This class contains helpers to extract the proper parameters to create such a factory.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ClassLoadingStrategySelector {

	public static FieldCacheLoadingType guessAppropriateCollectorType(TwoWayFieldBridge fieldBridge) {
		if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
			NullEncodingTwoWayFieldBridge encoding = (NullEncodingTwoWayFieldBridge) fieldBridge;
			return guessAppropriateCollectorType( encoding.unwrap() );
		}
		else if ( fieldBridge instanceof TwoWayString2FieldBridgeAdaptor ) {
			return FieldCacheLoadingType.STRING;
		}
		else if ( fieldBridge instanceof IntegerNumericFieldBridge ) {
			return FieldCacheLoadingType.INT;
		}
		else if ( fieldBridge instanceof LongNumericFieldBridge ) {
			return FieldCacheLoadingType.LONG;
		}
		else if ( fieldBridge instanceof DoubleNumericFieldBridge ) {
			return FieldCacheLoadingType.DOUBLE;
		}
		else if ( fieldBridge instanceof FloatNumericFieldBridge ) {
			return FieldCacheLoadingType.FLOAT;
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
	 *
	 * @return the underlying string bridge or {@code null} if we can't extract it
	 */
	public static TwoWayStringBridge getTwoWayStringBridge(TwoWayFieldBridge fieldBridge) {
		if ( fieldBridge instanceof NullEncodingTwoWayFieldBridge ) {
			NullEncodingTwoWayFieldBridge encoding = (NullEncodingTwoWayFieldBridge) fieldBridge;
			return getTwoWayStringBridge( encoding.unwrap() );
		}
		else if ( fieldBridge instanceof TwoWayString2FieldBridgeAdaptor ) {
			TwoWayString2FieldBridgeAdaptor adaptor = (TwoWayString2FieldBridgeAdaptor ) fieldBridge;
			return adaptor.unwrap();
		}
		else {
			return null;
		}
	}
}

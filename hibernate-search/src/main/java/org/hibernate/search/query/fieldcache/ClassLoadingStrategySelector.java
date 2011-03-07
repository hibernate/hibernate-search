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

package org.hibernate.search.query.fieldcache;

import org.hibernate.search.bridge.NullEncodingTwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.TwoWayString2FieldBridgeAdaptor;
import org.hibernate.search.bridge.TwoWayStringBridge;
import org.hibernate.search.bridge.builtin.DoubleNumericFieldBridge;
import org.hibernate.search.bridge.builtin.FloatNumericFieldBridge;
import org.hibernate.search.bridge.builtin.IntegerNumericFieldBridge;
import org.hibernate.search.bridge.builtin.LongNumericFieldBridge;

/**
 * A FieldCacheCollectorFactory requires two parameters which are inferred from
 * the type of field and it's applied bridges. Not all cases can be covered.
 * This class contains helpers to extract the proper parameters to create such a factory.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class ClassLoadingStrategySelector {

	public static FieldCollectorType guessAppropriateCollectorType(TwoWayFieldBridge idBridge) {
		if ( idBridge instanceof NullEncodingTwoWayFieldBridge ) {
			NullEncodingTwoWayFieldBridge encoding = (NullEncodingTwoWayFieldBridge) idBridge;
			return guessAppropriateCollectorType( encoding.unwrap() );
		}
		else if ( idBridge instanceof TwoWayString2FieldBridgeAdaptor ) {
			return FieldCollectorType.STRING;
		}
		else if ( idBridge instanceof IntegerNumericFieldBridge ) {
			return FieldCollectorType.INT;
		}
		else if ( idBridge instanceof LongNumericFieldBridge ) {
			return FieldCollectorType.LONG;
		}
		else if ( idBridge instanceof DoubleNumericFieldBridge ) {
			return FieldCollectorType.DOUBLE;
		}
		else if ( idBridge instanceof FloatNumericFieldBridge ) {
			return FieldCollectorType.FLOAT;
		}
		else {
			// we don't know how to extract this: no fieldCache will be available
			return null;
		}
	}

	/**
	 * @return null if we can't extract a TwoWayStringBridge
	 */
	public static TwoWayStringBridge getTwoWayStringBridge(TwoWayFieldBridge idBridge) {
		if ( idBridge instanceof NullEncodingTwoWayFieldBridge ) {
			NullEncodingTwoWayFieldBridge encoding = (NullEncodingTwoWayFieldBridge) idBridge;
			return getTwoWayStringBridge( encoding.unwrap() );
		}
		else if ( idBridge instanceof TwoWayString2FieldBridgeAdaptor ) {
			TwoWayString2FieldBridgeAdaptor adaptor = (TwoWayString2FieldBridgeAdaptor) idBridge;
			return adaptor.unwrap();
		}
		else {
			return null;
		}
	}

}

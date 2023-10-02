/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.documentation.mapper.orm.binding.bridgeresolver;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.bridge.ValueBridge;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeFromIndexedValueContext;
import org.hibernate.search.mapper.pojo.bridge.runtime.ValueBridgeToIndexedValueContext;

public class MyCoordinatesBridge implements ValueBridge<MyCoordinates, GeoPoint> {
	@Override
	public GeoPoint toIndexedValue(MyCoordinates value, ValueBridgeToIndexedValueContext context) {
		return GeoPoint.of( value.latitude(), value.longitude() );
	}

	@Override
	public MyCoordinates fromIndexedValue(GeoPoint value, ValueBridgeFromIndexedValueContext context) {
		return new MyCoordinates( value.latitude(), value.longitude() );
	}
}

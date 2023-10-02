/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class GeoPointPropertyTypeDescriptor extends PropertyTypeDescriptor<GeoPoint, GeoPoint> {

	public static final GeoPointPropertyTypeDescriptor INSTANCE = new GeoPointPropertyTypeDescriptor();

	private GeoPointPropertyTypeDescriptor() {
		super( GeoPoint.class );
	}

	@Override
	protected PropertyValues<GeoPoint, GeoPoint> createValues() {
		return PropertyValues.<GeoPoint>passThroughBuilder()
				.add( GeoPoint.of( 0.0, 0.0 ), "0.0, 0.0" )
				.add( GeoPoint.of( 100.123, 200.234 ), "100.123, 200.234" )
				.add( GeoPoint.of( 41.89193, 12.51133 ), "41.89193, 12.51133" )
				.add( GeoPoint.of( -26.4390917, 133.281323 ), "-26.4390917, 133.281323" )
				.add( GeoPoint.of( -14.2400732, -53.1805017 ), "-14.2400732, -53.1805017" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<GeoPoint> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<GeoPoint>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(GeoPoint identifier) {
				TypeWithIdentifierBridge1 instance = new TypeWithIdentifierBridge1();
				instance.id = identifier;
				return instance;
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge2() {
				return TypeWithIdentifierBridge2.class;
			}
		};
	}

	@Override
	public DefaultValueBridgeExpectations<GeoPoint, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<GeoPoint, GeoPoint>() {

			@Override
			public Class<GeoPoint> getIndexFieldJavaType() {
				return GeoPoint.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, GeoPoint propertyValue) {
				TypeWithValueBridge1 instance = new TypeWithValueBridge1();
				instance.id = identifier;
				instance.myProperty = propertyValue;
				return instance;
			}

			@Override
			public Class<?> getTypeWithValueBridge2() {
				return TypeWithValueBridge2.class;
			}

			@Override
			public GeoPoint getNullAsValueBridge1() {
				return GeoPoint.of( 0.0, 0.0 );
			}

			@Override
			public GeoPoint getNullAsValueBridge2() {
				return GeoPoint.of( 100.123, 200.234 );
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		GeoPoint id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		GeoPoint id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		GeoPoint myProperty;
		// see ParseUtils#GEO_POINT_SEPARATOR
		@GenericField(indexNullAs = "0, 0")
		GeoPoint indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		GeoPoint myProperty;
		// see ParseUtils#GEO_POINT_SEPARATOR
		@GenericField(indexNullAs = "100.123, 200.234")
		GeoPoint indexNullAsProperty;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;

public abstract class PropertyTypeDescriptor<V, F> {
	public static final int VECTOR_DIMENSION = 2;
	private static List<PropertyTypeDescriptor<?, ?>> all;

	public static List<PropertyTypeDescriptor<?, ?>> getAll() {
		if ( all == null ) {
			all = Collections.unmodifiableList( Arrays.asList(
					StringPropertyTypeDescriptor.INSTANCE,
					BoxedIntegerPropertyTypeDescriptor.INSTANCE,
					BoxedLongPropertyTypeDescriptor.INSTANCE,
					BoxedBooleanPropertyTypeDescriptor.INSTANCE,
					BoxedCharacterPropertyTypeDescriptor.INSTANCE,
					BoxedBytePropertyTypeDescriptor.INSTANCE,
					BoxedShortPropertyTypeDescriptor.INSTANCE,
					BoxedFloatPropertyTypeDescriptor.INSTANCE,
					BoxedDoublePropertyTypeDescriptor.INSTANCE,
					PrimitiveIntegerPropertyTypeDescriptor.INSTANCE,
					PrimitiveLongPropertyTypeDescriptor.INSTANCE,
					PrimitiveBooleanPropertyTypeDescriptor.INSTANCE,
					PrimitiveCharacterPropertyTypeDescriptor.INSTANCE,
					PrimitiveBytePropertyTypeDescriptor.INSTANCE,
					PrimitiveShortPropertyTypeDescriptor.INSTANCE,
					PrimitiveFloatPropertyTypeDescriptor.INSTANCE,
					PrimitiveDoublePropertyTypeDescriptor.INSTANCE,
					EnumPropertyTypeDescriptor.INSTANCE,
					InstantPropertyTypeDescriptor.INSTANCE,
					LocalDatePropertyTypeDescriptor.INSTANCE,
					JavaUtilDatePropertyTypeDescriptor.INSTANCE,
					JavaUtilCalendarPropertyTypeDescriptor.INSTANCE,
					BigDecimalPropertyTypeDescriptor.INSTANCE,
					BigIntegerPropertyTypeDescriptor.INSTANCE,
					UUIDPropertyTypeDescriptor.INSTANCE,
					LocalDateTimePropertyTypeDescriptor.INSTANCE,
					LocalTimePropertyTypeDescriptor.INSTANCE,
					ZonedDateTimePropertyTypeDescriptor.INSTANCE,
					YearPropertyTypeDescriptor.INSTANCE,
					YearMonthPropertyTypeDescriptor.INSTANCE,
					MonthDayPropertyTypeDescriptor.INSTANCE,
					OffsetDateTimePropertyTypeDescriptor.INSTANCE,
					OffsetTimePropertyTypeDescriptor.INSTANCE,
					ZoneOffsetPropertyTypeDescriptor.INSTANCE,
					ZoneIdPropertyTypeDescriptor.INSTANCE,
					DurationPropertyTypeDescriptor.INSTANCE,
					PeriodPropertyTypeDescriptor.INSTANCE,
					JavaNetURIPropertyTypeDescriptor.INSTANCE,
					JavaNetURLPropertyTypeDescriptor.INSTANCE,
					JavaSqlDatePropertyTypeDescriptor.INSTANCE,
					JavaSqlTimestampPropertyTypeDescriptor.INSTANCE,
					JavaSqlTimePropertyTypeDescriptor.INSTANCE,
					GeoPointPropertyTypeDescriptor.INSTANCE,
					PrimitiveByteArrayPropertyTypeDescriptor.INSTANCE,
					PrimitiveFloatArrayPropertyTypeDescriptor.INSTANCE
			) );
		}
		return all;
	}

	private final Class<V> javaType;
	private final Class<V> boxedJavaType;

	private final PropertyValues<V, F> values = createValues();

	protected PropertyTypeDescriptor(Class<V> javaType) {
		this( javaType, javaType );
	}

	protected PropertyTypeDescriptor(Class<V> javaType, Class<V> boxedJavaType) {
		this.javaType = javaType;
		this.boxedJavaType = boxedJavaType;
		if ( boxedJavaType.isPrimitive() ) {
			throw new IllegalArgumentException( "For primitive types, boxedJavaType must be provided" );
		}
	}

	public final Class<V> getJavaType() {
		return javaType;
	}

	public final Class<V> getBoxedJavaType() {
		return boxedJavaType;
	}

	public boolean isNullable() {
		return !javaType.isPrimitive();
	}

	public boolean isVectorType() {
		return false;
	}

	public boolean supportedAsIdentifier() {
		return true;
	}

	public PropertyValues<V, F> values() {
		return values;
	}

	protected abstract PropertyValues<V, F> createValues();

	public V toProjectedValue(V indexedValue) {
		return indexedValue;
	}

	public abstract DefaultIdentifierBridgeExpectations<V> getDefaultIdentifierBridgeExpectations();

	public abstract DefaultValueBridgeExpectations<V, ?> getDefaultValueBridgeExpectations();

	@Override
	public String toString() {
		return javaType.getSimpleName();
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;

public abstract class PropertyTypeDescriptor<V> {

	private static List<PropertyTypeDescriptor<?>> all;

	public static List<PropertyTypeDescriptor<?>> getAll() {
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
					GeoPointPropertyTypeDescriptor.INSTANCE
			) );
		}
		return all;
	}

	private final Class<V> javaType;

	protected PropertyTypeDescriptor(Class<V> javaType) {
		this.javaType = javaType;
	}

	public final Class<V> getJavaType() {
		return javaType;
	}

	public boolean isNullable() {
		return !javaType.isPrimitive();
	}

	public abstract Optional<DefaultIdentifierBridgeExpectations<V>> getDefaultIdentifierBridgeExpectations();

	public abstract DefaultValueBridgeExpectations<V, ?> getDefaultValueBridgeExpectations();

	@Override
	public String toString() {
		return javaType.getSimpleName();
	}
}

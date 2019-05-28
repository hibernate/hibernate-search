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
					new BoxedIntegerPropertyTypeDescriptor(),
					new BoxedLongPropertyTypeDescriptor(),
					new BoxedBooleanPropertyTypeDescriptor(),
					new BoxedCharacterPropertyTypeDescriptor(),
					new BoxedBytePropertyTypeDescriptor(),
					new BoxedShortPropertyTypeDescriptor(),
					new BoxedFloatPropertyTypeDescriptor(),
					new BoxedDoublePropertyTypeDescriptor(),
					new PrimitiveIntegerPropertyTypeDescriptor(),
					new PrimitiveLongPropertyTypeDescriptor(),
					new PrimitiveBooleanPropertyTypeDescriptor(),
					new PrimitiveCharacterPropertyTypeDescriptor(),
					new PrimitiveBytePropertyTypeDescriptor(),
					new PrimitiveShortPropertyTypeDescriptor(),
					new PrimitiveFloatPropertyTypeDescriptor(),
					new PrimitiveDoublePropertyTypeDescriptor(),
					new EnumPropertyTypeDescriptor(),
					new InstantPropertyTypeDescriptor(),
					new LocalDatePropertyTypeDescriptor(),
					new JavaUtilDatePropertyTypeDescriptor(),
					new JavaUtilCalendarPropertyTypeDescriptor(),
					new BigDecimalPropertyTypeDescriptor(),
					new BigIntegerPropertyTypeDescriptor(),
					new UUIDPropertyTypeDescriptor(),
					new LocalDateTimePropertyTypeDescriptor(),
					new LocalTimePropertyTypeDescriptor(),
					new ZonedDateTimePropertyTypeDescriptor(),
					new YearPropertyTypeDescriptor(),
					new MonthDayPropertyTypeDescriptor(),
					new OffsetDateTimePropertyTypeDescriptor(),
					new OffsetTimePropertyTypeDescriptor(),
					new ZoneOffsetPropertyTypeDescriptor(),
					new ZoneIdPropertyTypeDescriptor(),
					new DurationPropertyTypeDescriptor(),
					new PeriodPropertyTypeDescriptor(),
					new JavaNetURIPropertyTypeDescriptor(),
					new JavaNetURLPropertyTypeDescriptor(),
					new JavaSqlDatePropertyTypeDescriptor(),
					new JavaSqlTimestampPropertyTypeDescriptor(),
					new JavaSqlTimePropertyTypeDescriptor()
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

	public abstract Optional<DefaultValueBridgeExpectations<V, ?>> getDefaultValueBridgeExpectations();

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder( "PropertyTypeDescriptor{" );
		sb.append( "javaType=" ).append( javaType );
		sb.append( '}' );
		return sb.toString();
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class OffsetTimePropertyTypeDescriptor extends PropertyTypeDescriptor<OffsetTime> {

	OffsetTimePropertyTypeDescriptor() {
		super( OffsetTime.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<OffsetTime>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<OffsetTime, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<OffsetTime, OffsetTime>() {
			@Override
			public Class<OffsetTime> getProjectionType() {
				return OffsetTime.class;
			}

			@Override
			public Class<OffsetTime> getIndexFieldJavaType() {
				return OffsetTime.class;
			}

			@Override
			public List<OffsetTime> getEntityPropertyValues() {
				return Arrays.asList(
						OffsetTime.MIN,
						LocalTime.of( 7, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						LocalTime.of( 12, 0, 0 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						LocalTime.of( 12, 0, 1 ).atOffset( ZoneOffset.ofHours( 1 ) ),
						LocalTime.of( 12, 0, 1 ).atOffset( ZoneOffset.ofHours( -6 ) ),
						OffsetTime.MAX
				);
			}

			@Override
			public List<OffsetTime> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, OffsetTime propertyValue) {
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
			public OffsetTime getNullAsValueBridge1() {
				return LocalTime.MIDNIGHT.atOffset( ZoneOffset.UTC );
			}

			@Override
			public OffsetTime getNullAsValueBridge2() {
				return LocalTime.of( 12, 30, 55 ).atOffset( ZoneOffset.ofHours( -3 ) );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		OffsetTime myProperty;
		@GenericField(indexNullAs = "00:00:00Z")
		OffsetTime indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		OffsetTime myProperty;
		@GenericField(indexNullAs = "12:30:55-03:00")
		OffsetTime indexNullAsProperty;
	}
}

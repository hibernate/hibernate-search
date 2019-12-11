/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class OffsetDateTimePropertyTypeDescriptor extends PropertyTypeDescriptor<OffsetDateTime> {

	OffsetDateTimePropertyTypeDescriptor() {
		super( OffsetDateTime.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<OffsetDateTime>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<OffsetDateTime, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<OffsetDateTime, OffsetDateTime>() {
			@Override
			public Class<OffsetDateTime> getProjectionType() {
				return OffsetDateTime.class;
			}

			@Override
			public Class<OffsetDateTime> getIndexFieldJavaType() {
				return OffsetDateTime.class;
			}

			@Override
			public List<OffsetDateTime> getEntityPropertyValues() {
				return Arrays.asList(
						OffsetDateTime.of( LocalDateTime.MIN, ZoneOffset.ofHours( 1 ) ),
						OffsetDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 7, 0, 0 ), ZoneOffset.ofHours( 1 ) ),
						OffsetDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ), ZoneOffset.ofHours( 1 ) ),
						OffsetDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ), ZoneOffset.ofHours( -6 ) ),
						OffsetDateTime.of( LocalDateTime.MAX, ZoneOffset.ofHours( 1 ) )
				);
			}

			@Override
			public List<OffsetDateTime> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, OffsetDateTime propertyValue) {
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
			public OffsetDateTime getNullAsValueBridge1() {
				return OffsetDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0 ), ZoneOffset.UTC );
			}

			@Override
			public OffsetDateTime getNullAsValueBridge2() {
				return OffsetDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 30, 59 ), ZoneOffset.ofHours( -6 ) );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		OffsetDateTime myProperty;
		OffsetDateTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public OffsetDateTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "1970-01-01T00:00:00Z")
		public OffsetDateTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		OffsetDateTime myProperty;
		OffsetDateTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public OffsetDateTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "1999-01-01T07:30:59-06:00")
		public OffsetDateTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

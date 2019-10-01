/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class ZonedDateTimePropertyTypeDescriptor extends PropertyTypeDescriptor<ZonedDateTime> {

	ZonedDateTimePropertyTypeDescriptor() {
		super( ZonedDateTime.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<ZonedDateTime>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<ZonedDateTime, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<ZonedDateTime, ZonedDateTime>() {
			@Override
			public Class<ZonedDateTime> getProjectionType() {
				return ZonedDateTime.class;
			}

			@Override
			public Class<ZonedDateTime> getIndexFieldJavaType() {
				return ZonedDateTime.class;
			}

			@Override
			public List<ZonedDateTime> getEntityPropertyValues() {
				return Arrays.asList(
						ZonedDateTime.of( LocalDateTime.MIN, ZoneId.of( "Europe/Paris" ) ),
						ZonedDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 7, 0, 0 ), ZoneId.of( "Europe/Paris" ) ),
						ZonedDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ), ZoneId.of( "Europe/Paris" ) ),
						ZonedDateTime.of( LocalDateTime.of( 1999, Month.JANUARY, 1, 7, 0, 0 ), ZoneId.of( "America/Chicago" ) ),
						ZonedDateTime.of( LocalDateTime.MAX, ZoneId.of( "Europe/Paris" ) )
				);
			}

			@Override
			public List<ZonedDateTime> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, ZonedDateTime propertyValue) {
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
			public ZonedDateTime getNullAsValueBridge1() {
				return ZonedDateTime.of( LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0 ), ZoneId.of( "GMT" ) );
			}

			@Override
			public ZonedDateTime getNullAsValueBridge2() {
				return ZonedDateTime.of( LocalDateTime.of( 1999, Month.MAY, 31, 9, 30, 10 ), ZoneId.of( "America/Chicago" ) );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		ZonedDateTime myProperty;
		ZonedDateTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public ZonedDateTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "1970-01-01T00:00:00Z[GMT]")
		public ZonedDateTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		ZonedDateTime myProperty;
		ZonedDateTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public ZonedDateTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "1999-05-31T09:30:10-05:00[America/Chicago]")
		public ZonedDateTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

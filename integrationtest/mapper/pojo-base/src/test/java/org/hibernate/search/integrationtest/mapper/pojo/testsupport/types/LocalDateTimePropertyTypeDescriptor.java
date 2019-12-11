/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalDateTimePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalDateTime> {

	LocalDateTimePropertyTypeDescriptor() {
		super( LocalDateTime.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<LocalDateTime>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<LocalDateTime, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<LocalDateTime, LocalDateTime>() {
			@Override
			public Class<LocalDateTime> getProjectionType() {
				return LocalDateTime.class;
			}

			@Override
			public Class<LocalDateTime> getIndexFieldJavaType() {
				return LocalDateTime.class;
			}

			@Override
			public List<LocalDateTime> getEntityPropertyValues() {
				return Arrays.asList(
						LocalDateTime.MIN,
						LocalDateTime.of( 1970, Month.JANUARY, 1, 7, 0, 0 ),
						LocalDateTime.of( 1970, Month.JANUARY, 9, 7, 0, 0 ),
						LocalDateTime.of( 2017, Month.JANUARY, 9, 7, 0, 0 ),
						LocalDateTime.MAX
				);
			}

			@Override
			public List<LocalDateTime> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, LocalDateTime propertyValue) {
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
			public LocalDateTime getNullAsValueBridge1() {
				return LocalDateTime.of( 1970, Month.JANUARY, 1, 0, 0, 0 );
			}

			@Override
			public LocalDateTime getNullAsValueBridge2() {
				return LocalDateTime.of( 2030, Month.NOVEMBER, 21, 15, 15, 15 );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		LocalDateTime myProperty;
		LocalDateTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public LocalDateTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "1970-01-01T00:00:00")
		public LocalDateTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		LocalDateTime myProperty;
		LocalDateTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public LocalDateTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "2030-11-21T15:15:15")
		public LocalDateTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalDatePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalDate> {

	LocalDatePropertyTypeDescriptor() {
		super( LocalDate.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<LocalDate>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<LocalDate, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<LocalDate, LocalDate>() {
			@Override
			public Class<LocalDate> getProjectionType() {
				return LocalDate.class;
			}

			@Override
			public Class<LocalDate> getIndexFieldJavaType() {
				return LocalDate.class;
			}

			@Override
			public List<LocalDate> getEntityPropertyValues() {
				return Arrays.asList(
						LocalDate.MIN,
						LocalDate.parse( "1970-01-01" ),
						LocalDate.parse( "1970-01-09" ),
						LocalDate.parse( "2017-11-06" ),
						LocalDate.MAX
				);
			}

			@Override
			public List<LocalDate> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, LocalDate propertyValue) {
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
			public LocalDate getNullAsValueBridge1() {
				return LocalDate.parse( "1970-01-01" );
			}

			@Override
			public LocalDate getNullAsValueBridge2() {
				return LocalDate.parse( "2017-11-06" );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		LocalDate myProperty;
		LocalDate indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public LocalDate getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "1970-01-01")
		public LocalDate getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		LocalDate myProperty;
		LocalDate indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public LocalDate getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "2017-11-06")
		public LocalDate getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

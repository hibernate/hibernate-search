/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalTimePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalTime> {

	LocalTimePropertyTypeDescriptor() {
		super( LocalTime.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<LocalTime>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<LocalTime, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<LocalTime, LocalTime>() {
			@Override
			public Class<LocalTime> getProjectionType() {
				return LocalTime.class;
			}

			@Override
			public Class<LocalTime> getIndexFieldJavaType() {
				return LocalTime.class;
			}

			@Override
			public List<LocalTime> getEntityPropertyValues() {
				return Arrays.asList(
						LocalTime.MIN,
						LocalTime.of( 7, 0, 0 ),
						LocalTime.of( 12, 0, 0 ),
						LocalTime.of( 12, 0, 1 ),
						LocalTime.MAX
				);
			}

			@Override
			public List<LocalTime> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, LocalTime propertyValue) {
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
			public LocalTime getNullAsValueBridge1() {
				return LocalTime.MIDNIGHT;
			}

			@Override
			public LocalTime getNullAsValueBridge2() {
				return LocalTime.of( 12, 30, 15 );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		LocalTime myProperty;
		LocalTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public LocalTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "00:00:00")
		public LocalTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		LocalTime myProperty;
		LocalTime indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public LocalTime getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "12:30:15")
		public LocalTime getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

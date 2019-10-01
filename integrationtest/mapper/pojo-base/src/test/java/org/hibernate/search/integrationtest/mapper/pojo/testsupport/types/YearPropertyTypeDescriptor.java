/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Year;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class YearPropertyTypeDescriptor extends PropertyTypeDescriptor<Year> {

	YearPropertyTypeDescriptor() {
		super( Year.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Year>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Year, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Year, Year>() {
			@Override
			public Class<Year> getProjectionType() {
				return Year.class;
			}

			@Override
			public Class<Year> getIndexFieldJavaType() {
				return Year.class;
			}

			@Override
			public List<Year> getEntityPropertyValues() {
				return Arrays.asList(
						Year.of( Year.MIN_VALUE ), Year.of( -1 ), Year.of( 0 ), Year.of( 1 ), Year.of( 42 ), Year.of( Year.MAX_VALUE )
				);
			}

			@Override
			public List<Year> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Year propertyValue) {
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
			public Year getNullAsValueBridge1() {
				return Year.of( 0 );
			}

			@Override
			public Year getNullAsValueBridge2() {
				return Year.of( 2020 );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Year myProperty;
		Year indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Year getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "0000")
		public Year getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Year myProperty;
		Year indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Year getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "2020")
		public Year getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

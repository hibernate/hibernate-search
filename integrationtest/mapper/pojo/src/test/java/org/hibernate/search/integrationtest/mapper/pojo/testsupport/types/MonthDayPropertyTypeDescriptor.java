/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Month;
import java.time.MonthDay;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class MonthDayPropertyTypeDescriptor extends PropertyTypeDescriptor<MonthDay> {

	MonthDayPropertyTypeDescriptor() {
		super( MonthDay.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<MonthDay>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<MonthDay, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<MonthDay, MonthDay>() {
			@Override
			public Class<MonthDay> getProjectionType() {
				return MonthDay.class;
			}

			@Override
			public Class<MonthDay> getIndexFieldJavaType() {
				return MonthDay.class;
			}

			@Override
			public List<MonthDay> getEntityPropertyValues() {
				return Arrays.asList(
						MonthDay.of( Month.JANUARY, 1 ),
						MonthDay.of( Month.MARCH, 1 ),
						MonthDay.of( Month.MARCH, 2 ),
						MonthDay.of( Month.NOVEMBER, 21 ),
						MonthDay.of( Month.DECEMBER, 31 )
				);
			}

			@Override
			public List<MonthDay> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, MonthDay propertyValue) {
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
			public MonthDay getNullAsValueBridge1() {
				return MonthDay.of( Month.JANUARY, 1 );
			}

			@Override
			public MonthDay getNullAsValueBridge2() {
				return MonthDay.of( Month.NOVEMBER, 21 );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		MonthDay myProperty;
		MonthDay indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public MonthDay getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "--01-01")
		public MonthDay getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		MonthDay myProperty;
		MonthDay indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public MonthDay getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "--11-21")
		public MonthDay getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

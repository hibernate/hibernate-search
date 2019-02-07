/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Period;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class PeriodPropertyTypeDescriptor extends PropertyTypeDescriptor<Period> {

	PeriodPropertyTypeDescriptor() {
		super( Period.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Period>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Period, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Period, Period>() {
			@Override
			public Class<Period> getProjectionType() {
				return Period.class;
			}

			@Override
			public Class<Period> getIndexFieldJavaType() {
				return Period.class;
			}

			@Override
			public List<Period> getEntityPropertyValues() {
				return Arrays.asList(
						Period.ZERO,
						Period.ofDays( 1 ),
						Period.ofMonths( 3 ),
						Period.ofMonths( 4 ),
						Period.ofYears( 100 )
				);
			}

			@Override
			public List<Period> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Period propertyValue) {
				TypeWithValueBridge1 instance = new TypeWithValueBridge1();
				instance.id = identifier;
				instance.myProperty = propertyValue;
				return instance;
			}

			@Override
			public Class<?> getTypeWithValueBridge2() {
				return TypeWithValueBridge2.class;
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Period myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public Period getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Period myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public Period getMyProperty() {
			return myProperty;
		}
	}
}

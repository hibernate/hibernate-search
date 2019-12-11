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
		return Optional.of( new DefaultValueBridgeExpectations<Period, String>() {
			@Override
			public Class<Period> getProjectionType() {
				return Period.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
			}

			@Override
			public List<Period> getEntityPropertyValues() {
				return Arrays.asList(
						Period.ZERO,
						Period.ofDays( 1 ),
						Period.ofMonths( 4 ),
						Period.ofYears( 2050 ),
						Period.of( 1900, 12, 21 )
				);
			}

			@Override
			public List<String> getDocumentFieldValues() {
				return Arrays.asList(
						"+0000000000+0000000000+0000000000",
						"+0000000000+0000000000+0000000001",
						"+0000000000+0000000004+0000000000",
						"+0000002050+0000000000+0000000000",
						"+0000001900+0000000012+0000000021"
				);
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

			@Override
			public String getNullAsValueBridge1() {
				return "+0000000000+0000000000+0000000000";
			}

			@Override
			public String getNullAsValueBridge2() {
				return "+0000001900+0000000012+0000000021";
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Period myProperty;
		Period indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Period getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "P0D")
		public Period getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Period myProperty;
		Period indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Period getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "P1900Y12M21D")
		public Period getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

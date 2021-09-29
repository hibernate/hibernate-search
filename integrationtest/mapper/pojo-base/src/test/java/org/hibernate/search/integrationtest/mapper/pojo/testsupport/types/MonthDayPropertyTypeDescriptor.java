/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Month;
import java.time.MonthDay;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class MonthDayPropertyTypeDescriptor extends PropertyTypeDescriptor<MonthDay, MonthDay> {

	public static final MonthDayPropertyTypeDescriptor INSTANCE = new MonthDayPropertyTypeDescriptor();

	private MonthDayPropertyTypeDescriptor() {
		super( MonthDay.class );
	}

	@Override
	protected PropertyValues<MonthDay, MonthDay> createValues() {
		return PropertyValues.<MonthDay>passThroughBuilder()
				.add( MonthDay.of( Month.JANUARY, 1 ) )
				.add( MonthDay.of( Month.MARCH, 1 ) )
				.add( MonthDay.of( Month.MARCH, 2 ) )
				.add( MonthDay.of( Month.NOVEMBER, 21 ) )
				.add( MonthDay.of( Month.DECEMBER, 31 ) )
				.build();
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<MonthDay>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public DefaultValueBridgeExpectations<MonthDay, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<MonthDay, MonthDay>() {

			@Override
			public Class<MonthDay> getIndexFieldJavaType() {
				return MonthDay.class;
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
		};
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		MonthDay myProperty;
		@GenericField(indexNullAs = "--01-01")
		MonthDay indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		MonthDay myProperty;
		@GenericField(indexNullAs = "--11-21")
		MonthDay indexNullAsProperty;
	}
}

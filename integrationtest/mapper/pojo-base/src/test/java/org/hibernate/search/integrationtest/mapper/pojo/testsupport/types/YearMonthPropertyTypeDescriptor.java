/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class YearMonthPropertyTypeDescriptor extends PropertyTypeDescriptor<YearMonth, YearMonth> {

	public static final YearMonthPropertyTypeDescriptor INSTANCE = new YearMonthPropertyTypeDescriptor();

	private YearMonthPropertyTypeDescriptor() {
		super( YearMonth.class );
	}

	@Override
	protected PropertyValues<YearMonth, YearMonth> createValues() {
		return PropertyValues.<YearMonth>passThroughBuilder()
				.add( YearMonth.of( Year.MIN_VALUE, Month.NOVEMBER ) )
				.add( YearMonth.of( 2019, Month.JANUARY ) )
				.add( YearMonth.of( 2019, Month.FEBRUARY ) )
				.add( YearMonth.of( 2317, Month.NOVEMBER ) )
				.add( YearMonth.of( Year.MAX_VALUE, Month.NOVEMBER ) )
				.build();
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<YearMonth>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public DefaultValueBridgeExpectations<YearMonth, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<YearMonth, YearMonth>() {

			@Override
			public Class<YearMonth> getIndexFieldJavaType() {
				return YearMonth.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, YearMonth propertyValue) {
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
			public YearMonth getNullAsValueBridge1() {
				return YearMonth.of( 2019, Month.FEBRUARY );
			}

			@Override
			public YearMonth getNullAsValueBridge2() {
				return YearMonth.of( 2100, Month.NOVEMBER );
			}
		};
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		YearMonth myProperty;
		@GenericField(indexNullAs = "2019-02")
		YearMonth indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		YearMonth myProperty;
		@GenericField(indexNullAs = "2100-11")
		YearMonth indexNullAsProperty;
	}
}

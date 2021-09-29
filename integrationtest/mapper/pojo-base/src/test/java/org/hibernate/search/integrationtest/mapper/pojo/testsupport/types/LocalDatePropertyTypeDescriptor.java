/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.LocalDate;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class LocalDatePropertyTypeDescriptor extends PropertyTypeDescriptor<LocalDate, LocalDate> {

	public static final LocalDatePropertyTypeDescriptor INSTANCE = new LocalDatePropertyTypeDescriptor();

	private LocalDatePropertyTypeDescriptor() {
		super( LocalDate.class );
	}

	@Override
	protected PropertyValues<LocalDate, LocalDate> createValues() {
		return PropertyValues.<LocalDate>passThroughBuilder()
				.add( LocalDate.MIN )
				.add( LocalDate.parse( "1970-01-01" ) )
				.add( LocalDate.parse( "1970-01-09" ) )
				.add( LocalDate.parse( "2017-11-06" ) )
				.add( LocalDate.MAX )
				.build();
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<LocalDate>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public DefaultValueBridgeExpectations<LocalDate, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<LocalDate, LocalDate>() {

			@Override
			public Class<LocalDate> getIndexFieldJavaType() {
				return LocalDate.class;
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
		};
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		LocalDate myProperty;
		@GenericField(indexNullAs = "1970-01-01")
		LocalDate indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		LocalDate myProperty;
		@GenericField(indexNullAs = "2017-11-06")
		LocalDate indexNullAsProperty;
	}
}

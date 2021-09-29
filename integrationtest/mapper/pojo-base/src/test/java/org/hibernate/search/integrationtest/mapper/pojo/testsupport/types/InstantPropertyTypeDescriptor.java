/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Instant;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class InstantPropertyTypeDescriptor extends PropertyTypeDescriptor<Instant, Instant> {

	public static final InstantPropertyTypeDescriptor INSTANCE = new InstantPropertyTypeDescriptor();

	private InstantPropertyTypeDescriptor() {
		super( Instant.class );
	}

	@Override
	protected PropertyValues<Instant, Instant> createValues() {
		return PropertyValues.<Instant>passThroughBuilder()
				.add( Instant.MIN )
				.add( Instant.parse( "1970-01-01T00:00:00.00Z" ) )
				.add( Instant.parse( "1970-01-09T13:28:59.00Z" ) )
				.add( Instant.parse( "2017-11-06T19:19:00.54Z" ) )
				.add( Instant.MAX )
				.build();
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Instant>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public DefaultValueBridgeExpectations<Instant, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Instant, Instant>() {

			@Override
			public Class<Instant> getIndexFieldJavaType() {
				return Instant.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Instant propertyValue) {
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
			public Instant getNullAsValueBridge1() {
				return Instant.parse( "1970-01-01T00:00:00.00Z" );
			}

			@Override
			public Instant getNullAsValueBridge2() {
				return Instant.parse( "2017-11-06T19:19:03.54Z" );
			}
		};
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Instant myProperty;
		@GenericField(indexNullAs = "1970-01-01T00:00:00.00Z")
		Instant indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Instant myProperty;
		@GenericField(indexNullAs = "2017-11-06T19:19:03.54Z")
		Instant indexNullAsProperty;
	}
}

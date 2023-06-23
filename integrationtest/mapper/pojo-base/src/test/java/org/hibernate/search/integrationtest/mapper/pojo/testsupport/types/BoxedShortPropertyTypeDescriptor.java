/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class BoxedShortPropertyTypeDescriptor extends PropertyTypeDescriptor<Short, Short> {

	public static final BoxedShortPropertyTypeDescriptor INSTANCE = new BoxedShortPropertyTypeDescriptor();

	private BoxedShortPropertyTypeDescriptor() {
		super( Short.class );
	}

	@Override
	protected PropertyValues<Short, Short> createValues() {
		return PropertyValues.<Short>passThroughBuilder()
				.add( Short.MIN_VALUE, String.valueOf( Short.MIN_VALUE ) )
				.add( (short) -1, "-1" )
				.add( (short) 0, "0" )
				.add( (short) 1, "1" )
				.add( (short) 42, "42" )
				.add( Short.MAX_VALUE, String.valueOf( Short.MAX_VALUE ) )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Short> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Short>() {

			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Short identifier) {
				TypeWithIdentifierBridge1 instance = new TypeWithIdentifierBridge1();
				instance.id = identifier;
				return instance;
			}

			@Override
			public Class<?> getTypeWithIdentifierBridge2() {
				return TypeWithIdentifierBridge2.class;
			}
		};
	}

	@Override
	public DefaultValueBridgeExpectations<Short, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Short, Short>() {

			@Override
			public Class<Short> getIndexFieldJavaType() {
				return Short.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Short propertyValue) {
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
			public Short getNullAsValueBridge1() {
				return 0;
			}

			@Override
			public Short getNullAsValueBridge2() {
				return 531;
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Short id;
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Short id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Short myProperty;
		@GenericField(indexNullAs = "0")
		Short indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Short myProperty;
		@GenericField(indexNullAs = "531")
		Short indexNullAsProperty;
	}
}

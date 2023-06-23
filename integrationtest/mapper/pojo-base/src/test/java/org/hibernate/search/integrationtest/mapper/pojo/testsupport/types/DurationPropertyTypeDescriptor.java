/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Duration;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.values.PropertyValues;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class DurationPropertyTypeDescriptor extends PropertyTypeDescriptor<Duration, Long> {

	public static final DurationPropertyTypeDescriptor INSTANCE = new DurationPropertyTypeDescriptor();

	private DurationPropertyTypeDescriptor() {
		super( Duration.class );
	}

	@Override
	protected PropertyValues<Duration, Long> createValues() {
		return PropertyValues.<Duration, Long>builder()
				.add( Duration.ZERO, 0L, "PT0S" )
				.add( Duration.ofNanos( 1L ), 1L, "PT0.000000001S" )
				.add( Duration.ofSeconds( 1, 123L ), 1_000_000_123L,
						"PT1.000000123S" )
				.add( Duration.ofHours( 3 ), 3 * 60 * 60 * 1_000_000_000L,
						"PT3H" )
				.add( Duration.ofDays( 7 ), 7 * 24 * 60 * 60 * 1_000_000_000L,
						"PT168H" )
				.build();
	}

	@Override
	public DefaultIdentifierBridgeExpectations<Duration> getDefaultIdentifierBridgeExpectations() {
		return new DefaultIdentifierBridgeExpectations<Duration>() {
			@Override
			public Class<?> getTypeWithIdentifierBridge1() {
				return TypeWithIdentifierBridge1.class;
			}

			@Override
			public Object instantiateTypeWithIdentifierBridge1(Duration identifier) {
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
	public DefaultValueBridgeExpectations<Duration, ?> getDefaultValueBridgeExpectations() {
		return new DefaultValueBridgeExpectations<Duration, Long>() {

			@Override
			public Class<Long> getIndexFieldJavaType() {
				return Long.class;
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Duration propertyValue) {
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
			public Long getNullAsValueBridge1() {
				return Duration.ZERO.toNanos();
			}

			@Override
			public Long getNullAsValueBridge2() {
				return Duration.ofSeconds( 1, 123L ).toNanos();
			}
		};
	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_1_NAME)
	public static class TypeWithIdentifierBridge1 {
		@DocumentId
		Duration id;

	}

	@Indexed(index = DefaultIdentifierBridgeExpectations.TYPE_WITH_IDENTIFIER_BRIDGE_2_NAME)
	public static class TypeWithIdentifierBridge2 {
		@DocumentId
		Duration id;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		@DocumentId
		Integer id;
		@GenericField
		Duration myProperty;
		@GenericField(indexNullAs = "PT0S")
		Duration indexNullAsProperty;
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		@DocumentId
		Integer id;
		@GenericField
		Duration myProperty;
		@GenericField(indexNullAs = "PT1.000000123S")
		Duration indexNullAsProperty;
	}
}

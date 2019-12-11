/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class DurationPropertyTypeDescriptor extends PropertyTypeDescriptor<Duration> {

	DurationPropertyTypeDescriptor() {
		super( Duration.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Duration>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Duration, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Duration, Long>() {
			@Override
			public Class<Duration> getProjectionType() {
				return Duration.class;
			}

			@Override
			public Class<Long> getIndexFieldJavaType() {
				return Long.class;
			}

			@Override
			public List<Duration> getEntityPropertyValues() {
				return Arrays.asList(
						Duration.ZERO,
						Duration.ofNanos( 1L ),
						Duration.ofSeconds( 1, 123L ),
						Duration.ofHours( 3 ),
						Duration.ofDays( 7 )
				);
			}

			@Override
			public List<Long> getDocumentFieldValues() {
				return Arrays.asList(
						Duration.ZERO.toNanos(),
						1L,
						Duration.ofSeconds( 1, 123L ).toNanos(),
						Duration.ofHours( 3 ).toNanos(),
						Duration.ofDays( 7 ).toNanos()
				);
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
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Duration myProperty;
		Duration indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Duration getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "PT0S")
		public Duration getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Duration myProperty;
		Duration indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public Duration getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "PT1.000000123S")
		public Duration getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class ZoneOffsetPropertyTypeDescriptor extends PropertyTypeDescriptor<ZoneOffset> {

	ZoneOffsetPropertyTypeDescriptor() {
		super( ZoneOffset.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<ZoneOffset>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<ZoneOffset, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<ZoneOffset, Integer>() {
			@Override
			public Class<ZoneOffset> getProjectionType() {
				return ZoneOffset.class;
			}

			@Override
			public Class<Integer> getIndexFieldJavaType() {
				return Integer.class;
			}

			@Override
			public List<ZoneOffset> getEntityPropertyValues() {
				return Arrays.asList(
						ZoneOffset.MIN,
						ZoneOffset.ofHours( -1 ),
						ZoneOffset.UTC,
						ZoneOffset.ofHours( 1 ),
						ZoneOffset.ofHours( 7 ),
						ZoneOffset.MAX
				);
			}

			@Override
			public List<Integer> getDocumentFieldValues() {
				return Arrays.asList(
						ZoneOffset.MIN.getTotalSeconds(),
						ZoneOffset.ofHours( -1 ).getTotalSeconds(),
						ZoneOffset.UTC.getTotalSeconds(),
						ZoneOffset.ofHours( 1 ).getTotalSeconds(),
						ZoneOffset.ofHours( 7 ).getTotalSeconds(),
						ZoneOffset.MAX.getTotalSeconds()
				);
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, ZoneOffset propertyValue) {
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
			public Integer getNullAsValueBridge1() {
				return ZoneOffset.UTC.getTotalSeconds();
			}

			@Override
			public Integer getNullAsValueBridge2() {
				return ZoneOffset.ofHoursMinutesSeconds( -8, -30, -52 ).getTotalSeconds();
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		ZoneOffset myProperty;
		ZoneOffset indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public ZoneOffset getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "+00:00")
		public ZoneOffset getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		ZoneOffset myProperty;
		ZoneOffset indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public ZoneOffset getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "-08:30:52")
		public ZoneOffset getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

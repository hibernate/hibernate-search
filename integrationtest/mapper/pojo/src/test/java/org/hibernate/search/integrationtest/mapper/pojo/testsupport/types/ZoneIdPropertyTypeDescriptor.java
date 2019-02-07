/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class ZoneIdPropertyTypeDescriptor extends PropertyTypeDescriptor<ZoneId> {

	ZoneIdPropertyTypeDescriptor() {
		super( ZoneId.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<ZoneId>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<ZoneId, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<ZoneId, ZoneId>() {
			@Override
			public Class<ZoneId> getProjectionType() {
				return ZoneId.class;
			}

			@Override
			public Class<ZoneId> getIndexFieldJavaType() {
				return ZoneId.class;
			}

			@Override
			public List<ZoneId> getEntityPropertyValues() {
				return Arrays.asList(
						ZoneOffset.MIN,
						ZoneId.of( "America/Los_Angeles" ),
						ZoneOffset.UTC,
						ZoneId.of( "Europe/Paris" ),
						ZoneOffset.ofHours( 7 ),
						ZoneOffset.MAX
				);
			}

			@Override
			public List<ZoneId> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, ZoneId propertyValue) {
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
		ZoneId myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public ZoneId getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		ZoneId myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public ZoneId getMyProperty() {
			return myProperty;
		}
	}
}

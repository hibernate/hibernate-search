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
		return Optional.of( new DefaultValueBridgeExpectations<ZoneId, String>() {
			@Override
			public Class<ZoneId> getProjectionType() {
				return ZoneId.class;
			}

			@Override
			public Class<String> getIndexFieldJavaType() {
				return String.class;
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
			public List<String> getDocumentFieldValues() {
				return Arrays.asList(
						ZoneOffset.MIN.getId(),
						ZoneId.of( "America/Los_Angeles" ).getId(),
						ZoneOffset.UTC.getId(),
						ZoneId.of( "Europe/Paris" ).getId(),
						ZoneOffset.ofHours( 7 ).getId(),
						ZoneOffset.MAX.getId()
				);
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

			@Override
			public String getNullAsValueBridge1() {
				return ZoneOffset.UTC.getId();
			}

			@Override
			public String getNullAsValueBridge2() {
				return ZoneId.of( "Europe/Paris" ).getId();
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		ZoneId myProperty;
		ZoneId indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public ZoneId getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "Z")
		public ZoneId getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		ZoneId myProperty;
		ZoneId indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public ZoneId getMyProperty() {
			return myProperty;
		}

		@GenericField(indexNullAs = "Europe/Paris")
		public ZoneId getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

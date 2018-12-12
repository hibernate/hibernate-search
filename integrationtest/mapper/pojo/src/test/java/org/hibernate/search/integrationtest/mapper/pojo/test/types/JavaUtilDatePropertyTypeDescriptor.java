/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.test.types;

import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.test.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class JavaUtilDatePropertyTypeDescriptor extends PropertyTypeDescriptor<Date> {

	JavaUtilDatePropertyTypeDescriptor() {
		super( Date.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<Date>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<Date, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<Date, Instant>() {
			@Override
			public Class<Date> getProjectionType() {
				return Date.class;
			}

			@Override
			public Class<Instant> getIndexFieldJavaType() {
				return Instant.class;
			}

			@Override
			public List<Date> getEntityPropertyValues() {
				return Arrays.asList(
						new Date( Long.MIN_VALUE ),
						date( "1970-01-01T00:00:00.00Z" ),
						date( "1970-01-09T13:28:59.00Z" ),
						date( "2017-11-06T19:19:00.54Z" ),
						new Date( Long.MAX_VALUE )
				);
			}

			@Override
			public List<Instant> getDocumentFieldValues() {
				return Arrays.asList(
						Instant.ofEpochMilli( Long.MIN_VALUE ),
						Instant.parse( "1970-01-01T00:00:00.00Z" ),
						Instant.parse( "1970-01-09T13:28:59.00Z" ),
						Instant.parse( "2017-11-06T19:19:00.54Z" ),
						Instant.ofEpochMilli( Long.MAX_VALUE )
				);
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, Date propertyValue) {
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

	private static Date date(String toParse) {
		return Date.from( Instant.parse( toParse ) );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_INDEX_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		Date myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public Date getMyProperty() {
			return myProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_INDEX_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		Date myProperty;
		@DocumentId
		public Integer getId() {
			return id;
		}
		@GenericField
		public Date getMyProperty() {
			return myProperty;
		}
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.testsupport.types;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultIdentifierBridgeExpectations;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.types.expectations.DefaultValueBridgeExpectations;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;

public class GeoPointPropertyTypeDescriptor extends PropertyTypeDescriptor<GeoPoint> {

	GeoPointPropertyTypeDescriptor() {
		super( GeoPoint.class );
	}

	@Override
	public Optional<DefaultIdentifierBridgeExpectations<GeoPoint>> getDefaultIdentifierBridgeExpectations() {
		return Optional.empty();
	}

	@Override
	public Optional<DefaultValueBridgeExpectations<GeoPoint, ?>> getDefaultValueBridgeExpectations() {
		return Optional.of( new DefaultValueBridgeExpectations<GeoPoint, GeoPoint>() {
			@Override
			public Class<GeoPoint> getProjectionType() {
				return GeoPoint.class;
			}

			@Override
			public Class<GeoPoint> getIndexFieldJavaType() {
				return GeoPoint.class;
			}

			@Override
			public List<GeoPoint> getEntityPropertyValues() {
				List<GeoPoint> geoPoints = Arrays.asList(
						GeoPoint.of( 0.0, 0.0 ),
						GeoPoint.of( 100.123, 200.234 ),
						GeoPoint.of( 41.89193, 12.51133 ),
						GeoPoint.of( -26.4390917, 133.281323 ),
						GeoPoint.of( -14.2400732, -53.1805017 )
				);
				return geoPoints;
			}

			@Override
			public List<GeoPoint> getDocumentFieldValues() {
				return getEntityPropertyValues();
			}

			@Override
			public Class<?> getTypeWithValueBridge1() {
				return TypeWithValueBridge1.class;
			}

			@Override
			public Object instantiateTypeWithValueBridge1(int identifier, GeoPoint propertyValue) {
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
			public GeoPoint getNullAsValueBridge1() {
				return GeoPoint.of( 0.0, 0.0 );
			}

			@Override
			public GeoPoint getNullAsValueBridge2() {
				return GeoPoint.of( 100.123, 200.234 );
			}
		} );
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_1_NAME)
	public static class TypeWithValueBridge1 {
		Integer id;
		GeoPoint myProperty;
		GeoPoint indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public GeoPoint getMyProperty() {
			return myProperty;
		}

		// see ParseUtils#GEO_POINT_SEPARATOR
		@GenericField(indexNullAs = "0, 0")
		public GeoPoint getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}

	@Indexed(index = DefaultValueBridgeExpectations.TYPE_WITH_VALUE_BRIDGE_2_NAME)
	public static class TypeWithValueBridge2 {
		Integer id;
		GeoPoint myProperty;
		GeoPoint indexNullAsProperty;

		@DocumentId
		public Integer getId() {
			return id;
		}

		@GenericField
		public GeoPoint getMyProperty() {
			return myProperty;
		}

		// see ParseUtils#GEO_POINT_SEPARATOR
		@GenericField(indexNullAs = "100.123, 200.234")
		public GeoPoint getIndexNullAsProperty() {
			return indexNullAsProperty;
		}
	}
}

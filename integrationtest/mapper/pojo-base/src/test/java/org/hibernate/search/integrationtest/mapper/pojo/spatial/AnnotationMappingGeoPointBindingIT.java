/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.spatial;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.impl.integrationtest.mapper.pojo.standalone.StandalonePojoMappingSetupHelper;
import org.hibernate.search.mapper.pojo.standalone.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.GeoPointBinding;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Latitude;
import org.hibernate.search.mapper.pojo.bridge.builtin.annotation.Longitude;
import org.hibernate.search.mapper.pojo.standalone.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class AnnotationMappingGeoPointBindingIT {

	@Rule
	public BackendMock backendMock = new BackendMock();

	@Rule
	public StandalonePojoMappingSetupHelper setupHelper = StandalonePojoMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

	private SearchMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( GeoPointOnTypeEntity.INDEX, b -> b
				.field( "homeLocation", GeoPoint.class, b2 -> b2.projectable( Projectable.YES ).sortable( Sortable.YES ) )
				.field( "workLocation", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
		);
		backendMock.expectSchema( GeoPointOnCoordinatesPropertyEntity.INDEX, b -> b
				.field( "coord", GeoPoint.class )
				.field( "location", GeoPoint.class, b2 -> b2.projectable( Projectable.NO ) )
		);
		backendMock.expectSchema( GeoPointOnCustomCoordinatesPropertyEntity.INDEX, b -> b
				.field( "coord", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
				.field( "location", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
		);

		mapping = setupHelper.start()
				.withAnnotatedEntityTypes(
						GeoPointOnTypeEntity.class,
						GeoPointOnCoordinatesPropertyEntity.class,
						GeoPointOnCustomCoordinatesPropertyEntity.class
				)
				.withAnnotatedTypes(
						CustomCoordinates.class
				)
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		try ( SearchSession session = mapping.createSession() ) {
			GeoPointOnTypeEntity entity1 = new GeoPointOnTypeEntity();
			entity1.id = 1;
			entity1.homeLatitude = 1.1d;
			entity1.homeLongitude = 1.2d;
			entity1.workLatitude = 1.3d;
			entity1.workLongitude = 1.4d;
			GeoPointOnCoordinatesPropertyEntity entity2 = new GeoPointOnCoordinatesPropertyEntity();
			entity2.id = 2;
			entity2.coord = new GeoPoint() {
				@Override
				public double latitude() {
					return 2.1d;
				}

				@Override
				public double longitude() {
					return 2.2d;
				}
			};
			GeoPointOnCustomCoordinatesPropertyEntity entity3 = new GeoPointOnCustomCoordinatesPropertyEntity();
			entity3.id = 3;
			entity3.coord = new CustomCoordinates( 3.1d, 3.2d );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );
			session.indexingPlan().add( entity3 );

			backendMock.expectWorks( GeoPointOnTypeEntity.INDEX )
					.add( "1", b -> b
							.field( "homeLocation", GeoPoint.of(
									entity1.homeLatitude, entity1.homeLongitude
							) )
							.field( "workLocation", GeoPoint.of(
									entity1.workLatitude, entity1.workLongitude
							) )
					);
			backendMock.expectWorks( GeoPointOnCoordinatesPropertyEntity.INDEX )
					.add( "2", b -> b
							.field( "coord", entity2.coord )
							.field( "location", entity2.coord )
					);
			backendMock.expectWorks( GeoPointOnCustomCoordinatesPropertyEntity.INDEX )
					.add( "3", b -> b
							.field( "coord", GeoPoint.of(
									entity3.coord.lat, entity3.coord.lon
							) )
							.field( "location", GeoPoint.of(
									entity3.coord.lat, entity3.coord.lon
							) )
					);
		}
	}

	@Indexed(index = GeoPointOnTypeEntity.INDEX)
	@GeoPointBinding(fieldName = "homeLocation", markerSet = "home", projectable = Projectable.YES, sortable = Sortable.YES)
	@GeoPointBinding(fieldName = "workLocation", markerSet = "work")
	public static final class GeoPointOnTypeEntity {

		public static final String INDEX = "GeoPointOnTypeEntity";

		@DocumentId
		private Integer id;

		@Latitude(markerSet = "home")
		private Double homeLatitude;

		@Longitude(markerSet = "home")
		private Double homeLongitude;

		@Latitude(markerSet = "work")
		private Double workLatitude;

		@Longitude(markerSet = "work")
		private Double workLongitude;

	}

	@Indexed(index = GeoPointOnCoordinatesPropertyEntity.INDEX)
	public static final class GeoPointOnCoordinatesPropertyEntity {

		public static final String INDEX = "GeoPointOnCoordinatesPropertyEntity";

		@DocumentId
		private Integer id;

		@GenericField
		@GenericField(name = "location", projectable = Projectable.NO)
		private GeoPoint coord;

	}

	@Indexed(index = GeoPointOnCustomCoordinatesPropertyEntity.INDEX)
	public static final class GeoPointOnCustomCoordinatesPropertyEntity {

		public static final String INDEX = "GeoPointOnCustomCoordinatesPropertyEntity";

		@DocumentId
		private Integer id;

		@GeoPointBinding
		@GeoPointBinding(fieldName = "location")
		private CustomCoordinates coord;

	}

	// Does not implement GeoPoint on purpose
	public static class CustomCoordinates {

		// Test primitive type
		@Latitude
		private final double lat;
		// Test boxed type
		@Longitude
		private final Double lon;

		public CustomCoordinates(double lat, Double lon) {
			this.lat = lat;
			this.lon = lon;
		}
	}

}

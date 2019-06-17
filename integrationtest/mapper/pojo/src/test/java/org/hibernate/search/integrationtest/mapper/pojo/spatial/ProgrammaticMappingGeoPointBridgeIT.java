/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.pojo.spatial;

import org.hibernate.search.engine.backend.types.Projectable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.javabean.JavaBeanMapping;
import org.hibernate.search.mapper.pojo.bridge.builtin.spatial.GeoPointBridgeBuilder;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingDefinitionContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ProgrammaticMappingGeoPointBridgeIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = new JavaBeanMappingSetupHelper();

	private JavaBeanMapping mapping;

	@Before
	public void setup() {
		backendMock.expectSchema( GeoPointOnTypeEntity.INDEX, b -> b
				.field( "homeLocation", GeoPoint.class, b2 -> b2.projectable( Projectable.YES ).sortable( Sortable.YES ) )
				.field( "workLocation", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
		);
		backendMock.expectSchema( GeoPointOnCoordinatesPropertyEntity.INDEX, b -> b
				.field( "coord", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
				.field( "location", GeoPoint.class, b2 -> b2.projectable( Projectable.NO ).sortable( Sortable.DEFAULT ) )
		);
		backendMock.expectSchema( GeoPointOnCustomCoordinatesPropertyEntity.INDEX, b -> b
				.field( "coord", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
				.field( "location", GeoPoint.class, b2 -> b2.projectable( Projectable.DEFAULT ).sortable( Sortable.DEFAULT ) )
		);

		mapping = setupHelper.withBackendMock( backendMock )
				.withConfiguration( builder -> {
					builder.addEntityTypes( CollectionHelper.asSet(
							GeoPointOnTypeEntity.class,
							GeoPointOnCoordinatesPropertyEntity.class,
							GeoPointOnCustomCoordinatesPropertyEntity.class
					) );

					ProgrammaticMappingDefinitionContext mappingDefinition = builder.programmaticMapping();
					mappingDefinition.type( GeoPointOnTypeEntity.class )
							.indexed( GeoPointOnTypeEntity.INDEX )
							.bridge( GeoPointBridgeBuilder.forType()
									.fieldName( "homeLocation" )
									.markerSet( "home" )
									.projectable( Projectable.YES )
									.sortable( Sortable.YES )
							)
							.bridge(
							GeoPointBridgeBuilder.forType()
											.fieldName( "workLocation" )
											.markerSet( "work" )
							)
							.property( "id" )
									.documentId()
							.property( "homeLatitude" )
									.marker( GeoPointBridgeBuilder.latitude().markerSet( "home" ) )
							.property( "homeLongitude" )
									.marker( GeoPointBridgeBuilder.longitude().markerSet( "home" ) )
							.property( "workLatitude" )
									.marker( GeoPointBridgeBuilder.latitude().markerSet( "work" ) )
							.property( "workLongitude" )
									.marker( GeoPointBridgeBuilder.longitude().markerSet( "work" ) );

					mappingDefinition.type( GeoPointOnCoordinatesPropertyEntity.class )
							.indexed( GeoPointOnCoordinatesPropertyEntity.INDEX )
							.property( "id" )
									.documentId()
							.property( "coord" )
									.bridge( GeoPointBridgeBuilder.forProperty() )
									.bridge( GeoPointBridgeBuilder.forProperty()
											.fieldName( "location" )
											.projectable( Projectable.NO )
									);

					mappingDefinition.type( GeoPointOnCustomCoordinatesPropertyEntity.class )
							.indexed( GeoPointOnCustomCoordinatesPropertyEntity.INDEX )
							.property( "id" )
									.documentId()
							.property( "coord" )
									.bridge( GeoPointBridgeBuilder.forProperty() )
									.bridge( GeoPointBridgeBuilder.forProperty()
											.fieldName( "location" )
									);

					mappingDefinition.type( CustomCoordinates.class )
							.property( "lat" )
									.marker( GeoPointBridgeBuilder.latitude() )
							.property( "lon" )
									.marker( GeoPointBridgeBuilder.longitude() );
				} )
				.setup();

		backendMock.verifyExpectationsMet();
	}

	@Test
	public void index() {
		try ( SearchSession session = mapping.createSession() ) {
			GeoPointOnTypeEntity entity1 = new GeoPointOnTypeEntity();
			entity1.setId( 1 );
			entity1.setHomeLatitude( 1.1d );
			entity1.setHomeLongitude( 1.2d );
			entity1.setWorkLatitude( 1.3d );
			entity1.setWorkLongitude( 1.4d );
			GeoPointOnCoordinatesPropertyEntity entity2 = new GeoPointOnCoordinatesPropertyEntity();
			entity2.setId( 2 );
			entity2.setCoord( new GeoPoint() {
				@Override
				public double getLatitude() {
					return 2.1d;
				}
				@Override
				public double getLongitude() {
					return 2.2d;
				}
			} );
			GeoPointOnCustomCoordinatesPropertyEntity entity3 = new GeoPointOnCustomCoordinatesPropertyEntity();
			entity3.setId( 3 );
			entity3.setCoord( new CustomCoordinates( 3.1d, 3.2d ) );

			session.getMainWorkPlan().add( entity1 );
			session.getMainWorkPlan().add( entity2 );
			session.getMainWorkPlan().add( entity3 );

			backendMock.expectWorks( GeoPointOnTypeEntity.INDEX )
					.add( "1", b -> b
							.field( "homeLocation", GeoPoint.of(
									entity1.getHomeLatitude(), entity1.getHomeLongitude()
							) )
							.field( "workLocation", GeoPoint.of(
									entity1.getWorkLatitude(), entity1.getWorkLongitude()
							) )
					)
					.preparedThenExecuted();
			backendMock.expectWorks( GeoPointOnCoordinatesPropertyEntity.INDEX )
					.add( "2", b -> b
							.field( "coord", entity2.getCoord() )
							.field( "location", entity2.getCoord() )
					)
					.preparedThenExecuted();
			backendMock.expectWorks( GeoPointOnCustomCoordinatesPropertyEntity.INDEX )
					.add( "3", b -> b
							.field( "coord", GeoPoint.of(
									entity3.getCoord().getLat(), entity3.getCoord().getLon()
							) )
							.field( "location", GeoPoint.of(
									entity3.getCoord().getLat(), entity3.getCoord().getLon()
							) )
					)
					.preparedThenExecuted();
		}
	}

	public static final class GeoPointOnTypeEntity {

		public static final String INDEX = "GeoPointOnTypeEntity";

		private Integer id;

		private Double homeLatitude;

		private Double homeLongitude;

		private Double workLatitude;

		private Double workLongitude;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Double getHomeLatitude() {
			return homeLatitude;
		}

		public void setHomeLatitude(Double homeLatitude) {
			this.homeLatitude = homeLatitude;
		}

		public Double getHomeLongitude() {
			return homeLongitude;
		}

		public void setHomeLongitude(Double homeLongitude) {
			this.homeLongitude = homeLongitude;
		}

		public Double getWorkLatitude() {
			return workLatitude;
		}

		public void setWorkLatitude(Double workLatitude) {
			this.workLatitude = workLatitude;
		}

		public Double getWorkLongitude() {
			return workLongitude;
		}

		public void setWorkLongitude(Double workLongitude) {
			this.workLongitude = workLongitude;
		}

	}

	public static final class GeoPointOnCoordinatesPropertyEntity {

		public static final String INDEX = "GeoPointOnCoordinatesPropertyEntity";

		private Integer id;

		private GeoPoint coord;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public GeoPoint getCoord() {
			return coord;
		}

		public void setCoord(GeoPoint coord) {
			this.coord = coord;
		}

	}

	public static final class GeoPointOnCustomCoordinatesPropertyEntity {

		public static final String INDEX = "GeoPointOnCustomCoordinatesPropertyEntity";

		private Integer id;

		private CustomCoordinates coord;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public CustomCoordinates getCoord() {
			return coord;
		}

		public void setCoord(CustomCoordinates coord) {
			this.coord = coord;
		}

	}

	// Does not implement GeoPoint on purpose
	public static class CustomCoordinates {

		private final double lat;
		private final Double lon;

		public CustomCoordinates(double lat, Double lon) {
			this.lat = lat;
			this.lon = lon;
		}

		// Test primitive type
		public double getLat() {
			return lat;
		}

		// Test boxed type
		public Double getLon() {
			return lon;
		}
	}

}

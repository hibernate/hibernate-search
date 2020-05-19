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
import org.hibernate.search.mapper.javabean.mapping.SearchMapping;
import org.hibernate.search.mapper.pojo.bridge.builtin.programmatic.GeoPointBinder;
import org.hibernate.search.mapper.javabean.session.SearchSession;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.ProgrammaticMappingConfigurationContext;
import org.hibernate.search.integrationtest.mapper.pojo.testsupport.util.rule.JavaBeanMappingSetupHelper;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.mapper.pojo.mapping.definition.programmatic.TypeMappingStep;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;


public class ProgrammaticMappingGeoPointBindingIT {

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public JavaBeanMappingSetupHelper setupHelper = JavaBeanMappingSetupHelper.withBackendMock( MethodHandles.lookup(), backendMock );

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
				.withConfiguration( builder -> {
					builder.addEntityTypes( CollectionHelper.asSet(
							GeoPointOnTypeEntity.class,
							GeoPointOnCoordinatesPropertyEntity.class,
							GeoPointOnCustomCoordinatesPropertyEntity.class
					) );

					ProgrammaticMappingConfigurationContext mappingDefinition = builder.programmaticMapping();

					TypeMappingStep geoPointOntTypeEntityMapping = mappingDefinition.type( GeoPointOnTypeEntity.class );
					geoPointOntTypeEntityMapping.indexed( GeoPointOnTypeEntity.INDEX );
					geoPointOntTypeEntityMapping.binder( GeoPointBinder.create()
							.fieldName( "homeLocation" )
							.markerSet( "home" )
							.projectable( Projectable.YES )
							.sortable( Sortable.YES )
					);
					geoPointOntTypeEntityMapping.binder( GeoPointBinder.create()
							.fieldName( "workLocation" )
							.markerSet( "work" )
					);
					geoPointOntTypeEntityMapping.property( "id" ).documentId();
					geoPointOntTypeEntityMapping.property( "homeLatitude" )
							.marker( GeoPointBinder.latitude().markerSet( "home" ) );
					geoPointOntTypeEntityMapping.property( "homeLongitude" )
							.marker( GeoPointBinder.longitude().markerSet( "home" ) );
					geoPointOntTypeEntityMapping.property( "workLatitude" )
							.marker( GeoPointBinder.latitude().markerSet( "work" ) );
					geoPointOntTypeEntityMapping.property( "workLongitude" )
							.marker( GeoPointBinder.longitude().markerSet( "work" ) );

					TypeMappingStep geoPointOnCoordinatesPropertyEntityMapping =
							mappingDefinition.type( GeoPointOnCoordinatesPropertyEntity.class );
					geoPointOnCoordinatesPropertyEntityMapping.indexed( GeoPointOnCoordinatesPropertyEntity.INDEX );
					geoPointOnCoordinatesPropertyEntityMapping.property( "id" )
									.documentId();
					geoPointOnCoordinatesPropertyEntityMapping.property( "coord" )
									.genericField()
									.genericField( "location" ).projectable( Projectable.NO );

					TypeMappingStep geoPointOnCustomCoordinatesPropertyEntityMapping =
							mappingDefinition.type( GeoPointOnCustomCoordinatesPropertyEntity.class );
					geoPointOnCustomCoordinatesPropertyEntityMapping.indexed( GeoPointOnCustomCoordinatesPropertyEntity.INDEX );
					geoPointOnCustomCoordinatesPropertyEntityMapping.property( "id" ).documentId();
					geoPointOnCustomCoordinatesPropertyEntityMapping.property( "coord" )
							.binder( GeoPointBinder.create() )
							.binder( GeoPointBinder.create()
									.fieldName( "location" )
							);

					TypeMappingStep customCoordinatesMapping = mappingDefinition.type( CustomCoordinates.class );
					customCoordinatesMapping.property( "lat" )
							.marker( GeoPointBinder.latitude() );
					customCoordinatesMapping.property( "lon" )
							.marker( GeoPointBinder.longitude() );
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
				public double latitude() {
					return 2.1d;
				}
				@Override
				public double longitude() {
					return 2.2d;
				}
			} );
			GeoPointOnCustomCoordinatesPropertyEntity entity3 = new GeoPointOnCustomCoordinatesPropertyEntity();
			entity3.setId( 3 );
			entity3.setCoord( new CustomCoordinates( 3.1d, 3.2d ) );

			session.indexingPlan().add( entity1 );
			session.indexingPlan().add( entity2 );
			session.indexingPlan().add( entity3 );

			backendMock.expectWorks( GeoPointOnTypeEntity.INDEX )
					.add( "1", b -> b
							.field( "homeLocation", GeoPoint.of(
									entity1.getHomeLatitude(), entity1.getHomeLongitude()
							) )
							.field( "workLocation", GeoPoint.of(
									entity1.getWorkLatitude(), entity1.getWorkLongitude()
							) )
					)
					.processedThenExecuted();
			backendMock.expectWorks( GeoPointOnCoordinatesPropertyEntity.INDEX )
					.add( "2", b -> b
							.field( "coord", entity2.getCoord() )
							.field( "location", entity2.getCoord() )
					)
					.processedThenExecuted();
			backendMock.expectWorks( GeoPointOnCustomCoordinatesPropertyEntity.INDEX )
					.add( "3", b -> b
							.field( "coord", GeoPoint.of(
									entity3.getCoord().getLat(), entity3.getCoord().getLon()
							) )
							.field( "location", GeoPoint.of(
									entity3.getCoord().getLat(), entity3.getCoord().getLon()
							) )
					)
					.processedThenExecuted();
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

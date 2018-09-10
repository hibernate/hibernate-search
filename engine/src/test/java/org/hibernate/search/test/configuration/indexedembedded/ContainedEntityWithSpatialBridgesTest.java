/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration.indexedembedded;

import org.hibernate.search.annotations.ContainedIn;
import org.hibernate.search.annotations.DocumentId;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.hibernate.search.annotations.Latitude;
import org.hibernate.search.annotations.Longitude;
import org.hibernate.search.annotations.Spatial;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.testsupport.TestForIssue;
import org.hibernate.search.testsupport.junit.SearchITHelper;
import org.hibernate.search.testsupport.junit.SearchIntegratorResource;
import org.hibernate.search.testsupport.setup.SearchConfigurationForTest;

import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;

@TestForIssue(jiraKey = "HSEARCH-3339")
public class ContainedEntityWithSpatialBridgesTest {

	@Rule
	public final SearchIntegratorResource integratorResource = new SearchIntegratorResource();
	private SearchIntegrator integrator;
	private final SearchITHelper helper = new SearchITHelper( () -> this.integrator );

	@Test
	public void typeSpatialBridgeNotTriggeringBootstrapException() {
		SearchConfigurationForTest config = new SearchConfigurationForTest()
				.addClasses( ContainingForTypeSpatialBridge.class, ContainedForTypeSpatialBridge.class );

		// The following line used to fail with an exception before the fix
		integrator = integratorResource.create( config );

		// If we get here, the bug seems fixed, but let's check indexing works properly
		QueryBuilder builder = helper.queryBuilder( ContainingForTypeSpatialBridge.class );
		Query initialCoordinatesQuery = builder.spatial().onField( "contained.location" ).within( 100, Unit.KM )
				.ofLatitude( 12.0 ).andLongitude( 12.0 ).createQuery();
		Query updatedCoordinatesQuery = builder.spatial().onField( "contained.location" ).within( 100, Unit.KM )
				.ofLatitude( -12.0 ).andLongitude( -12.0 ).createQuery();

		ContainingForTypeSpatialBridge containing = new ContainingForTypeSpatialBridge();
		containing.id = 1;
		ContainedForTypeSpatialBridge contained = new ContainedForTypeSpatialBridge();
		contained.containing = containing;
		containing.contained = contained;
		contained.latitude = 12.0;
		contained.longitude = 12.0;

		helper.add( containing );

		helper.assertThat( initialCoordinatesQuery ).matchesExactlyIds( 1 );
		helper.assertThat( updatedCoordinatesQuery ).matchesNone();

		// ... and let's check @ContainedIn works properly
		contained.latitude = -12.0;
		contained.longitude = -12.0;
		helper.add( contained );

		helper.assertThat( initialCoordinatesQuery ).matchesNone();
		helper.assertThat( updatedCoordinatesQuery ).matchesExactlyIds( 1 );
	}

	@Test
	public void propertySpatialBridgeNotTriggeringBootstrapException() {
		SearchConfigurationForTest config = new SearchConfigurationForTest()
				.addClasses( ContainingForPropertySpatialBridge.class, ContainedForPropertySpatialBridge.class );

		// The following line used to fail with an exception before the fix
		integrator = integratorResource.create( config );

		// If we get here, the bug seems fixed, but let's check indexing works properly
		QueryBuilder builder = helper.queryBuilder( ContainingForPropertySpatialBridge.class );
		Query initialCoordinatesQuery = builder.spatial().onField( "contained.location" ).within( 100, Unit.KM )
				.ofLatitude( 12.0 ).andLongitude( 12.0 ).createQuery();
		Query updatedCoordinatesQuery = builder.spatial().onField( "contained.location" ).within( 100, Unit.KM )
				.ofLatitude( -12.0 ).andLongitude( -12.0 ).createQuery();

		ContainingForPropertySpatialBridge containing = new ContainingForPropertySpatialBridge();
		containing.id = 1;
		ContainedForPropertySpatialBridge contained = new ContainedForPropertySpatialBridge();
		contained.containing = containing;
		containing.contained = contained;
		contained.coordinates = Point.fromDegrees( 12.0, 12.0 );

		helper.add( containing );

		helper.assertThat( initialCoordinatesQuery ).matchesExactlyIds( 1 );
		helper.assertThat( updatedCoordinatesQuery ).matchesNone();

		// ... and let's check @ContainedIn works properly
		contained.coordinates = Point.fromDegrees( -12.0, -12.0 );
		helper.add( contained );

		helper.assertThat( initialCoordinatesQuery ).matchesNone();
		helper.assertThat( updatedCoordinatesQuery ).matchesExactlyIds( 1 );
	}

	@Indexed
	private static class ContainingForTypeSpatialBridge {
		@DocumentId
		private Integer id;

		@IndexedEmbedded
		private ContainedForTypeSpatialBridge contained;
	}

	@Spatial(name = "location")
	public static class ContainedForTypeSpatialBridge {
		@ContainedIn
		private ContainingForTypeSpatialBridge containing;

		private Double latitude;
		private Double longitude;

		@Latitude(of = "location")
		public Double getLatitude() {
			return latitude;
		}

		@Longitude(of = "location")
		public Double getLongitude() {
			return longitude;
		}
	}

	@Indexed
	private static class ContainingForPropertySpatialBridge {
		@DocumentId
		private Integer id;

		@IndexedEmbedded
		private ContainedForPropertySpatialBridge contained;
	}

	public static class ContainedForPropertySpatialBridge {
		@ContainedIn
		private ContainingForPropertySpatialBridge containing;

		@Spatial(name = "location")
		private Coordinates coordinates;

		public Coordinates getCoordinates() {
			return coordinates;
		}
	}

}

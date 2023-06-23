/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.dsl;

import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.query.dsl.Unit;
import org.hibernate.search.spatial.Coordinates;
import org.hibernate.search.spatial.impl.Point;
import org.hibernate.search.testsupport.junit.SearchFactoryHolder;
import org.hibernate.search.testsupport.junit.SearchITHelper;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
//DO NOT AUTO INDENT THIS FILE.
//MY DSL IS BEAUTIFUL, DUMB INDENTATION IS SCREWING IT UP
public class SpatialDSLTest {

	@Rule
	public final SearchFactoryHolder sfHolder = new SearchFactoryHolder( POI.class );

	private final SearchITHelper helper = new SearchITHelper( sfHolder );

	@Before
	public void setUp() throws Exception {
		indexTestData();
	}

	@Test
	public void testSpatialRangeQueries() {
		final QueryBuilder builder = helper.queryBuilder( POI.class );

		Coordinates coordinates = Point.fromDegrees( 24d, 31.5d );
		Query query = builder
				.spatial()
				.onField( "location" )
				.within( 51, Unit.KM )
				.ofCoordinates( coordinates )
				.createQuery();

		helper.assertThatQuery( query ).from( POI.class )
				.matchesExactlyIds( 2 );

		query = builder
				.spatial()
				.onField( "location" )
				.within( 500, Unit.KM )
				.ofLatitude( 48.858333d ).andLongitude( 2.294444d )
				.createQuery();

		helper.assertThatQuery( query ).from( POI.class )
				.matchesExactlyIds( 1 );
	}

	private void indexTestData() {
		POI poi = new POI( 1, "Tour Eiffel", 48.858333d, 2.294444d, "Monument" );
		helper.add( poi );
		poi = new POI( 2, "Bozo", 24d, 32d, "Monument" );
		helper.add( poi );
	}

}

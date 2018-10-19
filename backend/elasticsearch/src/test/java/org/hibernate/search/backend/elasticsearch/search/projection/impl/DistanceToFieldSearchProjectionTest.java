/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.junit.Test;

import com.google.gson.JsonObject;

public class DistanceToFieldSearchProjectionTest {

	private static final String FIELD = "myField";

	private static final GeoPoint LOCATION = GeoPoint.of( 43, 4 );

	@Test
	public void projection_script() {
		ElasticsearchSearchQueryElementCollector elementCollector = new ElasticsearchSearchQueryElementCollector();

		DistanceToFieldSearchProjectionImpl projection = new DistanceToFieldSearchProjectionImpl( FIELD,
				LOCATION, DistanceUnit.METERS );

		JsonObject requestBody = new JsonObject();

		projection.contributeRequest( requestBody, elementCollector );

		assertThat( elementCollector.getDistanceSortIndex( FIELD, LOCATION ) ).isNull();
		assertThat( requestBody.get( "script_fields" ) ).as( "script_fields" ).isNotNull();
	}

	@Test
	public void projection_sort() {
		ElasticsearchSearchQueryElementCollector elementCollector = new ElasticsearchSearchQueryElementCollector();
		elementCollector.collectSort( new JsonObject() );
		elementCollector.collectDistanceSort( new JsonObject(), FIELD, LOCATION );

		DistanceToFieldSearchProjectionImpl projection = new DistanceToFieldSearchProjectionImpl( FIELD,
				LOCATION, DistanceUnit.METERS );

		JsonObject requestBody = new JsonObject();

		projection.contributeRequest( requestBody, elementCollector );

		assertThat( elementCollector.getDistanceSortIndex( FIELD, LOCATION ) ).isEqualTo( 1 );
		assertThat( requestBody.get( "script_fields" ) ).as( "script_fields" ).isNull();
	}
}

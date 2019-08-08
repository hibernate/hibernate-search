/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext.DistanceSortKey;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.junit.Test;

import com.google.gson.JsonObject;
import org.easymock.EasyMockSupport;

public class DistanceToFieldSearchProjectionTest extends EasyMockSupport {

	private static final Set<String> INDEX_NAMES = Collections.singleton( "myIndexName" );
	private static final String FIELD = "myField";

	private static final GeoPoint LOCATION = GeoPoint.of( 43, 4 );

	@Test
	public void projection_script() {
		ElasticsearchDistanceToFieldProjection projection = new ElasticsearchDistanceToFieldProjection( INDEX_NAMES, FIELD, null,
				LOCATION, DistanceUnit.METERS );

		Map<DistanceSortKey, Integer> distanceSorts = Collections.emptyMap();
		SearchProjectionExtractContext extractContext = createExtractContext( distanceSorts );
		assertThat( extractContext.getDistanceSortIndex( FIELD, LOCATION ) ).isNull();

		JsonObject requestBody = new JsonObject();
		resetAll();
		replayAll();
		projection.contributeRequest( requestBody, extractContext );
		verifyAll();

		assertThat( requestBody.get( "script_fields" ) ).as( "script_fields" ).isNotNull();
	}

	@Test
	public void projection_sort() {
		ElasticsearchDistanceToFieldProjection projection = new ElasticsearchDistanceToFieldProjection( INDEX_NAMES, FIELD, null,
				LOCATION, DistanceUnit.METERS );

		Map<DistanceSortKey, Integer> distanceSorts = new HashMap<>();
		distanceSorts.put( new DistanceSortKey( FIELD, LOCATION ), 1 );
		SearchProjectionExtractContext extractContext = createExtractContext( distanceSorts );
		assertThat( extractContext.getDistanceSortIndex( FIELD, LOCATION ) ).isEqualTo( 1 );

		JsonObject requestBody = new JsonObject();

		resetAll();
		replayAll();
		projection.contributeRequest( requestBody, extractContext );
		verifyAll();

		assertThat( requestBody.get( "script_fields" ) ).as( "script_fields" ).isNull();
	}

	private SearchProjectionExtractContext createExtractContext(Map<DistanceSortKey, Integer> distanceSorts) {
		return new SearchProjectionExtractContext( distanceSorts );
	}
}

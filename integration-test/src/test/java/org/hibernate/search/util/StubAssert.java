/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

/**
 * @author Yoann Rodiere
 */
public final class StubAssert {

	private StubAssert() {
	}

	public static void assertRequest(Map<String, List<Request>> requestQueuesByIndex, String index, int workPositionInQueue,
			String host, String workType, String id, String body) throws JSONException {
		assertRequest( requestQueuesByIndex.get( index ).get( workPositionInQueue ),
				index, host, workType, id, body );
	}

	public static void assertRequest(Request request, String index, String host, String workType, String id, String body) throws JSONException {
		assertEquals( host, request.getHost() );
		assertEquals( workType, request.getWorkType() );

		Map<String, String> parameters = new HashMap<>();
		if ( index != null ) {
			parameters.put( "indexName", index );
		}
		if ( id != null ) {
			parameters.put( "id", id );
		}
		assertEquals( parameters, request.getParameters() );

		JSONAssert.assertEquals( body, request.getBody(), JSONCompareMode.STRICT );
	}
}

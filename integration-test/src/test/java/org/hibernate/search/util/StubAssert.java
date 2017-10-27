/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.client.impl.StubElasticsearchClient.Request;

import org.junit.Assert;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Yoann Rodiere
 */
public final class StubAssert {

	private StubAssert() {
	}

	public static void assertRequest(Map<String, List<Request>> requestQueuesByIndex, String indexName, int workPositionInQueue,
			String host, String workType, String id, String body) throws JSONException {
		assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, workPositionInQueue ),
				Arrays.asList( indexName ), host, workType, id, ignored -> { }, body );
	}

	private static Request getRequestInQueue(Map<String, List<Request>> requestQueuesByIndex,
			String indexName, int workPositionInQueue) {
		List<Request> queue = requestQueuesByIndex.get( indexName );
		Request request = null;
		if ( queue != null && queue.size() > workPositionInQueue ) {
			request = queue.get( workPositionInQueue );
		}
		assertNotNull( "No request found in queue for index '" + indexName + "' at position '" + workPositionInQueue + "'", queue );
		return request;
	}

	public static void assertRequest(Map<String, List<Request>> requestQueuesByIndex, Collection<String> indexNames, int workPositionInQueue,
			String host, String workType, String id, Consumer<BiConsumer<String, String>> otherParams, String body) throws JSONException {
		for ( String indexName : indexNames ) {
			assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, workPositionInQueue ),
					indexNames, host, workType, id, otherParams, body );
		}
	}

	public static void assertRequest(Request request, Collection<String> indexNames, String host, String workType, String id,
			Consumer<BiConsumer<String, String>> otherParams, String body) throws JSONException {
		assertEquals( host, request.getHost() );
		assertEquals( workType, request.getWorkType() );

		Map<String, Collection<String>> parameters = new HashMap<>();
		if ( indexNames != null ) {
			parameters.put( "indexName", indexNames );
		}
		if ( id != null ) {
			parameters.put( "id", Arrays.asList( id ) );
		}
		if ( otherParams != null ) {
			otherParams.accept( (key, value) ->
					parameters.computeIfAbsent( key, ignored -> new ArrayList<>() ).add( value )
			);
		}
		boolean sameParameters = true;
		if ( !parameters.keySet().equals( request.getParameters().keySet() ) ) {
			sameParameters = false;
		}
		else {
			for ( String key : parameters.keySet() ) {
				// Check that the expected parameters are there, in any order
				Collection<String> expected = new ArrayList<>( parameters.get( key ) );
				Collection<String> actual = new ArrayList<>( parameters.get( key ) );
				Iterator<String> actualIt = actual.iterator();
				while ( sameParameters && actualIt.hasNext() ) {
					String next = actualIt.next();
					actualIt.remove();
					sameParameters = expected.remove( next );
				}
				if ( !actual.isEmpty() ) {
					sameParameters = false;
				}
			}
		}
		if ( !sameParameters ) {
			Assert.fail( "expected parameters: <" + parameters + "> but was: <" + request.getParameters() + ">" );
		}

		JSONAssert.assertEquals( body, request.getBody(), JSONCompareMode.STRICT );
	}
}

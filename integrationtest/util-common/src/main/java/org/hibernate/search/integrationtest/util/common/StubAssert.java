/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.integrationtest.util.common.StubElasticsearchClient.Request;

import org.junit.Assert;

import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Yoann Rodiere
 */
public final class StubAssert {

	private StubAssert() {
	}

	public static void assertDropAndCreateIndexRequests(Map<String, List<Request>> requestQueuesByIndex, String indexName,
			String host, String mappingJson) throws JSONException {
		assertDropAndCreateIndexRequests( requestQueuesByIndex, indexName, host, host, mappingJson );
	}

	public static void assertDropAndCreateIndexRequests(Map<String, List<Request>> requestQueuesByIndex, String indexName,
			String dropHost, String createHost, String mappingJson) throws JSONException {
		assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, 0 ),
				Collections.singletonList( indexName ), dropHost, "DELETE", "", ignored -> { }, null );
		assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, 1 ),
				Collections.singletonList( indexName ), createHost, "PUT", "", ignored -> { },
				"{"
					+ "'mappings': {"
						+ "'typeName': " + mappingJson
					+ "}"
				+ "}" );
	}

	public static void assertIndexDocumentRequest(Map<String, List<Request>> requestQueuesByIndex, String indexName,
			int workPositionInQueue, String host, String id, String documentJson) throws JSONException {
		assertIndexDocumentRequest( requestQueuesByIndex, indexName, workPositionInQueue, host, id,
				c -> c.accept( "refresh", "true" ), documentJson );
	}

	public static void assertIndexDocumentRequest(Map<String, List<Request>> requestQueuesByIndex, String indexName,
			int workPositionInQueue, String host, String id, Consumer<BiConsumer<String, String>> params,
			String documentJson) throws JSONException {
		assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, workPositionInQueue ),
				Collections.singletonList( indexName ), host, "PUT",
				"/typeName/" + id, params, documentJson );
	}

	public static void assertRequest(Map<String, List<Request>> requestQueuesByIndex, String indexName,
			int workPositionInQueue, String host, String method, String pathAfterIndexPathComponent, String body) throws JSONException {
		assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, workPositionInQueue ),
				Arrays.asList( indexName ), host, method, pathAfterIndexPathComponent, ignored -> { }, body );
	}

	public static void assertRequest(Map<String, List<Request>> requestQueuesByIndex, String indexName,
			int workPositionInQueue, String host, String method, String pathAfterIndexPathComponent,
			Consumer<BiConsumer<String, String>> params, String body) throws JSONException {
		assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, workPositionInQueue ),
				Arrays.asList( indexName ), host, method, pathAfterIndexPathComponent, params, body );
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

	public static void assertRequest(Map<String, List<Request>> requestQueuesByIndex, Collection<String> indexNames,
			int workPositionInQueue, String host, String method, String pathAfterIndexPathComponent,
			Consumer<BiConsumer<String, String>> params, String body) throws JSONException {
		for ( String indexName : indexNames ) {
			assertRequest( getRequestInQueue( requestQueuesByIndex, indexName, workPositionInQueue ),
					indexNames, host, method, pathAfterIndexPathComponent, params, body );
		}
	}

	public static void assertRequest(Request request, Collection<String> indexNames, String host, String method,
			String pathAfterIndexPathComponent, Consumer<BiConsumer<String, String>> params, String body) throws JSONException {
		assertEquals( host, request.getHost() );
		assertThat( request.getIndexNames() ).containsOnly( indexNames.toArray() );
		assertEquals( method, request.getMethod() );
		assertEquals( pathAfterIndexPathComponent, request.getPathAfterIndexPathComponent() );

		Map<String, Collection<String>> expectedParameters = new HashMap<>();
		if ( params != null ) {
			params.accept( (key, value) ->
				expectedParameters.computeIfAbsent( key, ignored -> new ArrayList<>() ).add( value )
			);
		}
		boolean sameParameters = true;
		if ( !expectedParameters.keySet().equals( request.getParameters().keySet() ) ) {
			sameParameters = false;
		}
		else {
			for ( String key : expectedParameters.keySet() ) {
				// Check that the expected parameters are there, in any order
				Collection<String> expected = new ArrayList<>( expectedParameters.get( key ) );
				Collection<String> actual = new ArrayList<>( request.getSplitParameters().get( key ) );
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
			Assert.fail( "expected parameters: <" + expectedParameters + "> but was: <" + request.getSplitParameters() + ">" );
		}

		if ( body != null ) {
			assertThat( request.getBodyParts() ).hasSize( 1 );
			JSONAssert.assertEquals( body, request.getBodyParts().get( 0 ), JSONCompareMode.STRICT );
		}
		else {
			assertThat( request.getBodyParts() ).isEmpty();
		}
	}
}

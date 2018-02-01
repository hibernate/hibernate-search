/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.util.common;

import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonLogHelper;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLogCategories;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchClient implements ElasticsearchClientImplementor {

	private static final Log requestLog = LoggerFactory.make( Log.class, ElasticsearchLogCategories.REQUEST );

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private static final Deque<JsonObject> STUB_RESPONSE_BODIES = new LinkedList<>();

	private static final Deque<Request> REQUESTS = new LinkedList<>();

	public static void pushStubResponse(String jsonObject) {
		STUB_RESPONSE_BODIES.addLast( GSON.fromJson( jsonObject, JsonObject.class ) );
	}

	public static void pushStubResponse(JsonObject jsonObject) {
		STUB_RESPONSE_BODIES.addLast( jsonObject );
	}

	public static Map<String, List<Request>> drainRequestsByIndex() {
		Map<String, List<Request>> result = new HashMap<>();
		Request next = REQUESTS.pollFirst();
		while ( next != null ) {
			final Request request = next;
			request.getIndexNames().stream()
					.map( indexName -> result.computeIfAbsent( indexName, ignored -> new LinkedList<>() ) )
					.forEach( resultList -> resultList.add( request ) );

			next = REQUESTS.pollFirst();
		}
		return result;
	}

	public static class Request {
		private final String host;
		private final String method;
		private final String path;
		private final Map<String, String> parameters;
		private final List<String> bodyParts;

		private final Set<String> indexNames;
		private final String pathAfterIndexPathComponent;
		private final Map<String, Collection<String>> splitParameters;

		public Request(String host, String method, String path, Map<String, String> parameters, List<String> bodyParts) {
			this.host = host;
			this.method = method;
			this.path = path;
			this.parameters = parameters;
			this.bodyParts = bodyParts;

			int indexPathComponentStartIndex = 0;
			int indexPathComponentEndIndex = path.indexOf( '/', indexPathComponentStartIndex );
			if ( indexPathComponentEndIndex == 0 ) {
				indexPathComponentStartIndex = 1;
				indexPathComponentEndIndex = path.indexOf( '/', indexPathComponentStartIndex );
			}
			if ( indexPathComponentEndIndex < 0 ) {
				indexPathComponentEndIndex = path.length();
			}
			String indexPathComponent = path.substring( indexPathComponentStartIndex, indexPathComponentEndIndex );
			this.indexNames = new HashSet<>( Arrays.asList( indexPathComponent.split( "," ) ) );
			this.pathAfterIndexPathComponent = path.substring( indexPathComponentEndIndex, path.length() );

			this.splitParameters = new HashMap<>();
			parameters.forEach( (key, value) ->
					splitParameters.put( key, Arrays.asList( value.split( "," ) ) )
			);
		}

		public String getHost() {
			return host;
		}

		public String getMethod() {
			return method;
		}

		public String getPath() {
			return path;
		}

		public Map<String, String> getParameters() {
			return parameters;
		}

		public List<String> getBodyParts() {
			return bodyParts;
		}

		public Set<String> getIndexNames() {
			return indexNames;
		}

		public String getPathAfterIndexPathComponent() {
			return pathAfterIndexPathComponent;
		}

		public Map<String, Collection<String>> getSplitParameters() {
			return splitParameters;
		}
	}

	private final JsonLogHelper logHelper = JsonLogHelper.create( new GsonBuilder(), true );

	private final List<String> hosts;

	private final AtomicInteger nextHostIndex = new AtomicInteger( 0 );

	public StubElasticsearchClient(List<String> hosts) {
		this.hosts = hosts;
	}

	@Override
	public void init(GsonProvider gsonProvider) {
		// Ignored
	}

	@Override
	public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request) {
		List<String> bodyPartsAsString = request.getBodyParts().stream()
				.map( GSON::toJson )
				.collect( Collectors.toList() );
		int hostIndex = nextHostIndex.getAndUpdate( i -> ( i + 1 ) % hosts.size() );
		String host = hosts.get( hostIndex );

		REQUESTS.addLast( new Request( host, request.getMethod(), request.getPath(), request.getParameters(),
				bodyPartsAsString ) );

		JsonObject stubResponseBody = STUB_RESPONSE_BODIES.pollLast();
		if ( stubResponseBody == null ) {
			stubResponseBody = new JsonObject();
		}
		ElasticsearchResponse response = new ElasticsearchResponse( 200, "OK", stubResponseBody );

		requestLog.executedRequest( request.getMethod(), request.getPath(), request.getParameters(), 0,
				response.getStatusCode(), response.getStatusMessage(),
				logHelper.toString( request.getBodyParts() ),
				logHelper.toString( response.getBody() ) );

		return CompletableFuture.completedFuture( response );
	}

	@Override
	public void close() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + hosts + "]";
	}
}

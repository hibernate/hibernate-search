/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.lang.invoke.MethodHandles;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.spi.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class StubElasticsearchClient implements ElasticsearchClient {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private static final Deque<JsonObject> stubResponses = new LinkedList<>();

	private static final Deque<Request> requests = new LinkedList<>();

	public static void pushStubResponse(String jsonObject) {
		stubResponses.addLast( gson.fromJson( jsonObject, JsonObject.class ) );
	}

	public static void pushStubResponse(JsonObject jsonObject) {
		stubResponses.addLast( jsonObject );
	}

	public static Map<String, List<Request>> drainRequestsByIndex() {
		Map<String, List<Request>> result = new HashMap<>();
		Request next = requests.pollFirst();
		while ( next != null ) {
			final Request request = next;
			request.getParameters().get( "indexName" ).stream()
					.map( indexName -> result.computeIfAbsent( indexName, ignored -> new LinkedList<>() ) )
					.forEach( resultList -> resultList.add( request ) );

			next = requests.pollFirst();
		}
		return result;
	}

	public static class Request {
		private final String host;
		private final String workType;
		private final Map<String, List<String>> parameters;
		private final String body;

		public Request(String host, String workType, Map<String, List<String>> parameters, String body) {
			super();
			this.host = host;
			this.workType = workType;
			this.parameters = parameters;
			this.body = body;
		}

		public String getHost() {
			return host;
		}

		public String getWorkType() {
			return workType;
		}

		public Map<String, List<String>> getParameters() {
			return parameters;
		}

		public String getBody() {
			return body;
		}
	}

	private final List<String> hosts;

	private final AtomicInteger nextHostIndex = new AtomicInteger( 0 );

	public StubElasticsearchClient(List<String> hosts) {
		this.hosts = hosts;
	}

	public <T> CompletableFuture<T> execute(String workType, Map<String, List<String>> parameters, JsonObject body,
			Function<JsonObject, T> resultFunction) {
		String bodyAsString = body == null ? null : gson.toJson( body );
		int hostIndex = nextHostIndex.getAndUpdate( i -> ( i + 1 ) % hosts.size() );
		String host = hosts.get( hostIndex );
		log.executingWork( host, workType, parameters, bodyAsString );
		requests.addLast( new Request( host, workType, parameters, bodyAsString ) );
		JsonObject stubResponse = stubResponses.pollLast();
		if ( stubResponse == null ) {
			stubResponse = new JsonObject();
		}
		return CompletableFuture.completedFuture( resultFunction.apply( stubResponse ) );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + hosts + "]";
	}

}

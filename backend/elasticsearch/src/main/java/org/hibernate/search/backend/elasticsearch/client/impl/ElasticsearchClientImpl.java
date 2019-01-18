/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.GsonProvider;
import org.hibernate.search.backend.elasticsearch.gson.spi.JsonLogHelper;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLogCategories;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.util.impl.common.Closer;
import org.hibernate.search.util.impl.common.Executors;
import org.hibernate.search.util.impl.common.Futures;
import org.hibernate.search.util.impl.common.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchClientImpl implements ElasticsearchClientImplementor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Log requestLog = LoggerFactory.make( Log.class, ElasticsearchLogCategories.REQUEST );

	private final RestClient restClient;

	private final Sniffer sniffer;

	private final ScheduledExecutorService timeoutExecutorService;

	private final int requestTimeoutValue;
	private final TimeUnit requestTimeoutUnit;

	private volatile GsonProvider gsonProvider;

	public ElasticsearchClientImpl(RestClient restClient, Sniffer sniffer, int requestTimeoutValue, TimeUnit requestTimeoutUnit,
			GsonProvider initialGsonProvider) {
		this.restClient = restClient;
		this.sniffer = sniffer;
		this.timeoutExecutorService = Executors.newScheduledThreadPool( "Elasticsearch request timeout executor" );
		this.requestTimeoutValue = requestTimeoutValue;
		this.requestTimeoutUnit = requestTimeoutUnit;
		this.gsonProvider = initialGsonProvider;
	}

	@Override
	public void init(GsonProvider gsonProvider) {
		this.gsonProvider = gsonProvider;
	}

	@Override
	public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request) {
		CompletableFuture<ElasticsearchResponse> result = Futures.create( () -> send( request ) )
				.thenApply( response -> convertResponse( request, response ) );
		if ( requestLog.isDebugEnabled() ) {
			long startTime = System.nanoTime();
			result.thenAccept( response -> log( request, startTime, response ) );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clientClass) {
		if ( RestClient.class.isAssignableFrom( clientClass ) ) {
			return (T) restClient;
		}
		throw log.clientUnwrappingWithUnkownType( clientClass, RestClient.class );
	}

	private CompletableFuture<Response> send(ElasticsearchRequest elasticsearchRequest) {
		Gson gson = gsonProvider.getGson();

		CompletableFuture<Response> completableFuture = new CompletableFuture<>();

		HttpEntity entity;
		try {
			entity = ElasticsearchClientUtils.toEntity( gson, elasticsearchRequest );
		}
		catch (IOException | RuntimeException e) {
			completableFuture.completeExceptionally( e );
			return completableFuture;
		}

		restClient.performRequestAsync(
				toRequest( elasticsearchRequest, entity ),
				new ResponseListener() {
					@Override
					public void onSuccess(Response response) {
						completableFuture.complete( response );
					}
					@Override
					public void onFailure(Exception exception) {
						if ( exception instanceof ResponseException ) {
							requestLog.debug( "ES client issued a ResponseException - not necessarily a problem", exception );
							/*
							 * The client tries to guess what's an error and what's not, but it's too naive.
							 * A 404 on DELETE is not always important to us, for instance.
							 * Thus we ignore the exception and do our own checks afterwards.
							 */
							completableFuture.complete( ( (ResponseException) exception ).getResponse() );
						}
						else {
							completableFuture.completeExceptionally( exception );
						}
					}
				}
				);

		/*
		 * TODO maybe the callback should also cancel the request?
		 * In any case, the RestClient doesn't return the Future<?> from Apache HTTP client,
		 * so we can't do much until this changes.
		 */
		ScheduledFuture<?> timeout = timeoutExecutorService.schedule(
				() -> {
					if ( !completableFuture.isDone() ) {
						completableFuture.completeExceptionally( new TimeoutException() );
					}
				},
				requestTimeoutValue, requestTimeoutUnit
				);
		completableFuture.thenRun( () -> timeout.cancel( false ) );

		return completableFuture;
	}

	private static Request toRequest(ElasticsearchRequest elasticsearchRequest, HttpEntity entity) {
		Request request = new Request( elasticsearchRequest.getMethod(), elasticsearchRequest.getPath() );

		for ( Entry<String, String> parameter : elasticsearchRequest.getParameters().entrySet() ) {
			request.addParameter( parameter.getKey(), parameter.getValue() );
		}

		request.setEntity( entity );

		return request;
	}

	private ElasticsearchResponse convertResponse(ElasticsearchRequest request, Response response) {
		try {
			JsonObject body = parseBody( response );
			return new ElasticsearchResponse(
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(),
					body );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToParseElasticsearchResponse(
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(),
					e );
		}
	}

	private JsonObject parseBody(Response response) throws IOException {
		HttpEntity entity = response.getEntity();
		if ( entity == null ) {
			return null;
		}

		Gson gson = gsonProvider.getGson();
		Charset charset = getCharset( entity );
		try ( InputStream inputStream = entity.getContent();
				Reader reader = new InputStreamReader( inputStream, charset ) ) {
			return gson.fromJson( reader, JsonObject.class );
		}
	}

	private static Charset getCharset(HttpEntity entity) {
		ContentType contentType = ContentType.get( entity );
		Charset charset = contentType.getCharset();
		return charset != null ? charset : StandardCharsets.UTF_8;
	}

	private void log(ElasticsearchRequest request, long start, ElasticsearchResponse response) {
		long executionTimeNs = System.nanoTime() - start;
		long executionTimeMs = TimeUnit.NANOSECONDS.toMillis( executionTimeNs );
		if ( requestLog.isTraceEnabled() ) {
			JsonLogHelper logHelper = gsonProvider.getLogHelper();
			requestLog.executedRequest( request.getMethod(), request.getPath(), request.getParameters(),
					request.getBodyParts().size(), executionTimeMs,
					response.getStatusCode(), response.getStatusMessage(),
					logHelper.toString( request.getBodyParts() ),
					logHelper.toString( response.getBody() ) );
		}
		else {
			requestLog.executedRequest( request.getMethod(), request.getPath(), request.getParameters(),
					request.getBodyParts().size(), executionTimeMs,
					response.getStatusCode(), response.getStatusMessage() );
		}
	}

	@Override
	public void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			/*
			 * There's no point waiting for timeouts: we'll just cancel
			 * all timeouts and expect the RestClient to cancel all
			 * currently running requests when closing.
			 */
			closer.push( ExecutorService::shutdownNow, this.timeoutExecutorService );
			closer.push( Sniffer::close, this.sniffer );
			closer.push( RestClient::close, this.restClient );
		}
	}

}

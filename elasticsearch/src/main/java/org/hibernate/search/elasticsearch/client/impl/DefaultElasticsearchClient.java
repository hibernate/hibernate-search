/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.ElasticsearchLogCategories;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.util.impl.ElasticsearchClientUtils;
import org.hibernate.search.elasticsearch.util.impl.JsonLogHelper;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public class DefaultElasticsearchClient implements ElasticsearchClientImplementor {

	private static final Log log = LoggerFactory.make( Log.class );

	private static final Log requestLog = LoggerFactory.make( Log.class, ElasticsearchLogCategories.REQUEST );

	private final RestClient restClient;

	private final Sniffer sniffer;

	private final ScheduledExecutorService timeoutExecutorService;

	private final int requestTimeoutValue;
	private final TimeUnit requestTimeoutUnit;

	private volatile GsonProvider gsonProvider;

	public DefaultElasticsearchClient(RestClient restClient, Sniffer sniffer, int requestTimeoutValue, TimeUnit requestTimeoutUnit,
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

	private CompletableFuture<Response> send(ElasticsearchRequest request) {
		Gson gson = gsonProvider.getGson();
		HttpEntity entity = ElasticsearchClientUtils.toEntity( gson, request );
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();
		restClient.performRequestAsync(
				request.getMethod(),
				request.getPath(),
				request.getParameters(),
				entity,
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
			requestLog.executedRequest( request.getMethod(), request.getPath(), request.getParameters(), executionTimeMs,
					response.getStatusCode(), response.getStatusMessage(),
					logHelper.toString( request.getBodyParts() ),
					logHelper.toString( response.getBody() ) );
		}
		else {
			requestLog.executedRequest( request.getMethod(), request.getPath(), request.getParameters(), executionTimeMs,
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
			closer.push( this.timeoutExecutorService::shutdownNow );
			if ( this.sniffer != null ) {
				closer.push( this.sniffer::close );
			}
			closer.push( this.restClient::close );
		}
	}

}

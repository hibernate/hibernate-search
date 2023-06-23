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
import java.time.Duration;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.gson.spi.JsonLogHelper;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchLogCategories;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.entity.ContentType;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.sniff.Sniffer;

public class ElasticsearchClientImpl implements ElasticsearchClientImplementor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final Log requestLog = LoggerFactory.make( Log.class, ElasticsearchLogCategories.REQUEST );

	private final BeanHolder<? extends RestClient> restClientHolder;

	private final Sniffer sniffer;

	private final SimpleScheduledExecutor timeoutExecutorService;

	private final Optional<Integer> requestTimeoutMs;
	private final int connectionTimeoutMs;

	private final Gson gson;
	private final JsonLogHelper jsonLogHelper;

	ElasticsearchClientImpl(BeanHolder<? extends RestClient> restClientHolder, Sniffer sniffer,
			SimpleScheduledExecutor timeoutExecutorService,
			Optional<Integer> requestTimeoutMs, int connectionTimeoutMs,
			Gson gson, JsonLogHelper jsonLogHelper) {
		this.restClientHolder = restClientHolder;
		this.sniffer = sniffer;
		this.timeoutExecutorService = timeoutExecutorService;
		this.requestTimeoutMs = requestTimeoutMs;
		this.connectionTimeoutMs = connectionTimeoutMs;
		this.gson = gson;
		this.jsonLogHelper = jsonLogHelper;
	}

	@Override
	public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request) {
		CompletableFuture<ElasticsearchResponse> result = Futures.create( () -> send( request ) )
				.thenApply( this::convertResponse );
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
			return (T) restClientHolder.get();
		}
		throw log.clientUnwrappingWithUnkownType( clientClass, RestClient.class );
	}

	private CompletableFuture<Response> send(ElasticsearchRequest elasticsearchRequest) {
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();

		HttpEntity entity;
		try {
			entity = ElasticsearchClientUtils.toEntity( gson, elasticsearchRequest );
		}
		catch (IOException | RuntimeException e) {
			completableFuture.completeExceptionally( e );
			return completableFuture;
		}

		restClientHolder.get().performRequestAsync(
				toRequest( elasticsearchRequest, entity ),
				new ResponseListener() {
					@Override
					public void onSuccess(Response response) {
						completableFuture.complete( response );
					}

					@Override
					public void onFailure(Exception exception) {
						if ( exception instanceof ResponseException ) {
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

		Deadline deadline = elasticsearchRequest.deadline();
		if ( deadline == null && !requestTimeoutMs.isPresent() ) {
			// no need to schedule a client side timeout
			return completableFuture;
		}

		long currentTimeoutValue =
				deadline == null ? Long.valueOf( requestTimeoutMs.get() ) : deadline.checkRemainingTimeMillis();

		/*
		 * TODO HSEARCH-3590 maybe the callback should also cancel the request?
		 *  In any case, the RestClient doesn't return the Future<?> from Apache HTTP client,
		 *  so we can't do much until this changes.
		 */
		ScheduledFuture<?> timeout = timeoutExecutorService.schedule(
				() -> {
					if ( !completableFuture.isDone() ) {
						RuntimeException cause = log.requestTimedOut(
								Duration.ofNanos( TimeUnit.MILLISECONDS.toNanos( currentTimeoutValue ) ),
								elasticsearchRequest );
						completableFuture.completeExceptionally(
								deadline != null ? deadline.forceTimeoutAndCreateException( cause ) : cause
						);
					}
				},
				currentTimeoutValue, TimeUnit.MILLISECONDS
		);
		completableFuture.thenRun( () -> timeout.cancel( false ) );

		return completableFuture;
	}

	private Request toRequest(ElasticsearchRequest elasticsearchRequest, HttpEntity entity) {
		Request request = new Request( elasticsearchRequest.method(), elasticsearchRequest.path() );
		setPerRequestSocketTimeout( elasticsearchRequest, request );

		for ( Entry<String, String> parameter : elasticsearchRequest.parameters().entrySet() ) {
			request.addParameter( parameter.getKey(), parameter.getValue() );
		}

		request.setEntity( entity );

		return request;
	}

	private void setPerRequestSocketTimeout(ElasticsearchRequest elasticsearchRequest, Request request) {
		Deadline deadline = elasticsearchRequest.deadline();
		if ( deadline == null ) {
			return;
		}

		long timeToHardTimeout = deadline.checkRemainingTimeMillis();

		// set a per-request socket timeout
		int socketTimeoutMs = ( timeToHardTimeout <= Integer.MAX_VALUE ) ? Math.toIntExact( timeToHardTimeout ) : -1;
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout( 0 ) //Disable lease handling for the connection pool! See also HSEARCH-2681
				.setSocketTimeout( socketTimeoutMs )
				.setConnectTimeout( connectionTimeoutMs )
				.build();

		RequestOptions.Builder requestOptions = RequestOptions.DEFAULT.toBuilder()
				.setRequestConfig( requestConfig );

		request.setOptions( requestOptions );
	}

	private ElasticsearchResponse convertResponse(Response response) {
		try {
			JsonObject body = parseBody( response );
			return new ElasticsearchResponse(
					response.getHost(),
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(),
					body );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToParseElasticsearchResponse( response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(), e.getMessage(), e );
		}
	}

	private JsonObject parseBody(Response response) throws IOException {
		HttpEntity entity = response.getEntity();
		if ( entity == null ) {
			return null;
		}

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
		boolean successCode = ElasticsearchClientUtils.isSuccessCode( response.statusCode() );
		if ( !requestLog.isTraceEnabled() && successCode ) {
			return;
		}
		long executionTimeNs = System.nanoTime() - start;
		long executionTimeMs = TimeUnit.NANOSECONDS.toMillis( executionTimeNs );
		if ( successCode ) {
			requestLog.executedRequest( request.method(), response.host(), request.path(), request.parameters(),
					request.bodyParts().size(), executionTimeMs,
					response.statusCode(), response.statusMessage(),
					jsonLogHelper.toString( request.bodyParts() ),
					jsonLogHelper.toString( response.body() ) );
		}
		else {
			requestLog.executedRequestWithFailure( request.method(), response.host(), request.path(), request.parameters(),
					request.bodyParts().size(), executionTimeMs,
					response.statusCode(), response.statusMessage(),
					jsonLogHelper.toString( request.bodyParts() ),
					jsonLogHelper.toString( response.body() ) );
		}
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			/*
			 * There's no point waiting for timeouts: we'll just expect the RestClient to cancel all
			 * currently running requests when closing.
			 */
			closer.push( Sniffer::close, this.sniffer );
			// The BeanHolder is responsible for calling close() on the client if necessary.
			closer.push( BeanHolder::close, this.restClientHolder );
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToShutdownClient( e.getMessage(), e );
		}
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.hibernate.search.backend.elasticsearch.client.common.gson.spi.JsonLogHelper;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchRequestLog;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ClientJdkElasticsearchClient implements ElasticsearchClientImplementor {

	private static final Pattern CHARSET_PATTERN =
			Pattern.compile( "(?:;|^)\\s*charset\\s*=\\s*\"?([^\"\\s;]+)\"?", Pattern.CASE_INSENSITIVE );

	private final BeanHolder<? extends RestJdkClient> restClientHolder;

	private final SimpleScheduledExecutor timeoutExecutorService;

	private final Optional<Integer> requestTimeoutMs;

	private final Gson gson;
	private final JsonLogHelper jsonLogHelper;

	private final List<HttpRequestInterceptor> requestInterceptors;

	ClientJdkElasticsearchClient(BeanHolder<? extends RestJdkClient> restClientHolder,
			SimpleScheduledExecutor timeoutExecutorService,
			Optional<Integer> requestTimeoutMs,
			Gson gson, JsonLogHelper jsonLogHelper, List<HttpRequestInterceptor> requestInterceptors
	) {
		this.restClientHolder = restClientHolder;
		this.timeoutExecutorService = timeoutExecutorService;
		this.requestTimeoutMs = requestTimeoutMs;
		this.gson = gson;
		this.jsonLogHelper = jsonLogHelper;
		this.requestInterceptors = requestInterceptors;
	}

	@Override
	public CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request) {
		CompletableFuture<ElasticsearchResponse> result = Futures.create( () -> send( request ) )
				.thenApply( this::convertResponse );
		if ( ElasticsearchRequestLog.INSTANCE.isDebugEnabled() ) {
			long startTime = System.nanoTime();
			result.thenAccept( response -> log( request, startTime, response ) );
		}
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clientClass) {
		if ( HttpClient.class.isAssignableFrom( clientClass ) ) {
			return (T) restClientHolder.get();
		}
		throw ElasticsearchClientLog.INSTANCE.clientUnwrappingWithUnknownType( clientClass, HttpClient.class );
	}

	private ElasticsearchResponse convertResponse(HttpResponse<JsonObject> response) {
		try {
			return new ElasticsearchResponse(
					response.request().uri().getHost(),
					response.statusCode(),
					null,
					response.body() );
		}
		catch (RuntimeException e) {
			throw ElasticsearchClientLog.INSTANCE.failedToParseElasticsearchResponse( response.statusCode(),
					null, e.getMessage(), e );
		}
	}

	private static Charset getCharset(HttpHeaders headers) {
		Optional<String> contentType = headers.firstValue( "Content-Type" );
		if ( contentType.isPresent() ) {
			var matcher = CHARSET_PATTERN.matcher( contentType.get() );
			if ( matcher.find() ) {
				return Charset.forName( matcher.group( 1 ) );
			}
		}
		return StandardCharsets.UTF_8;
	}

	private CompletableFuture<HttpResponse<JsonObject>> send(ElasticsearchRequest elasticsearchRequest) {
		HttpRequest request;
		try {
			HttpRequest.BodyPublisher entity = ClientJdkGsonHttpEntity.toEntity( gson, elasticsearchRequest );
			request = toRequest( elasticsearchRequest, entity );
		}
		catch (IOException | RuntimeException e) {
			CompletableFuture<HttpResponse<JsonObject>> completableFuture = new CompletableFuture<>();
			completableFuture.completeExceptionally( e );
			return completableFuture;
		}

		CompletableFuture<HttpResponse<JsonObject>> completableFuture = restClientHolder.get().sendAsync(
				request,
				new JsonObjectBodyHandler()
		);

		Deadline deadline = elasticsearchRequest.deadline();
		if ( deadline == null && requestTimeoutMs.isEmpty() ) {
			// no need to schedule a client side timeout
			return completableFuture;
		}

		long currentTimeoutValue =
				deadline == null ? Long.valueOf( requestTimeoutMs.get() ) : deadline.checkRemainingTimeMillis();

		/*
		 * TODO HSEARCH-3590 maybe the callback should also cancel the request?
		 */
		ScheduledFuture<?> timeout = timeoutExecutorService.schedule(
				() -> {
					if ( !completableFuture.isDone() ) {
						RuntimeException cause = ElasticsearchClientLog.INSTANCE.requestTimedOut(
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

	private HttpRequest toRequest(ElasticsearchRequest elasticsearchRequest, HttpRequest.BodyPublisher bodyPublisher)
			throws IOException {
		URI uri = toUri( elasticsearchRequest );
		HttpRequest.Builder request = HttpRequest.newBuilder( uri )
				.method( elasticsearchRequest.method(), bodyPublisher );
		setPerRequestSocketTimeout( elasticsearchRequest, request );
		if ( !ClientJdkGsonHttpEntity.isNoBodyPublisher( bodyPublisher ) ) {
			request.header( "Content-Type", "application/json" );
		}

		HttpRequestInterceptorContext context = new HttpRequestInterceptorContext( elasticsearchRequest.method() );
		for ( HttpRequestInterceptor requestInterceptor : requestInterceptors ) {
			requestInterceptor.process( request, bodyPublisher, context );
		}

		return request.build();
	}

	private URI toUri(ElasticsearchRequest elasticsearchRequest) {
		return restClientHolder.get().nextNode().createRequestURI( elasticsearchRequest.path(),
				elasticsearchRequest.parameters() );
	}

	private void setPerRequestSocketTimeout(ElasticsearchRequest elasticsearchRequest, HttpRequest.Builder request) {
		Deadline deadline = elasticsearchRequest.deadline();
		if ( deadline == null ) {
			return;
		}

		long timeToHardTimeout = deadline.checkRemainingTimeMillis();

		// set a per-request socket timeout
		int generalRequestTimeoutMs = ( timeToHardTimeout <= Integer.MAX_VALUE ) ? Math.toIntExact( timeToHardTimeout ) : -1;
		request.timeout( Duration.of( generalRequestTimeoutMs, ChronoUnit.MILLIS ) );
	}

	private void log(ElasticsearchRequest request, long start, ElasticsearchResponse response) {
		boolean successCode = ElasticsearchClientUtils.isSuccessCode( response.statusCode() );
		if ( !ElasticsearchRequestLog.INSTANCE.isTraceEnabled() && successCode ) {
			return;
		}
		long executionTimeNs = System.nanoTime() - start;
		long executionTimeMs = TimeUnit.NANOSECONDS.toMillis( executionTimeNs );
		if ( successCode ) {
			ElasticsearchRequestLog.INSTANCE.executedRequest( request.method(), response.hostAndPort(), request.path(),
					request.parameters(),
					request.bodyParts().size(), executionTimeMs,
					response.statusCode(), response.statusMessage(),
					jsonLogHelper.toString( request.bodyParts() ),
					jsonLogHelper.toString( response.body() ) );
		}
		else {
			ElasticsearchRequestLog.INSTANCE.executedRequestWithFailure( request.method(), response.hostAndPort(),
					request.path(),
					request.parameters(),
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
			// The BeanHolder is responsible for calling close() on the client if necessary.
			closer.push( BeanHolder::close, this.restClientHolder );
		}
		catch (RuntimeException | IOException e) {
			throw ElasticsearchClientLog.INSTANCE.unableToShutdownClient( e.getMessage(), e );
		}
	}

	private class GsonJsonMapper implements Function<InputStream, JsonObject> {
		private final Charset charset;
		private final int statusCode;

		public GsonJsonMapper(Charset charset, int statusCode) {
			this.charset = charset;
			this.statusCode = statusCode;
		}

		@Override
		public JsonObject apply(InputStream inputStream) {
			try ( inputStream; Reader reader = new InputStreamReader( inputStream, charset ) ) {
				return gson.fromJson( reader, JsonObject.class );
			}
			catch (IOException e) {
				throw ElasticsearchClientLog.INSTANCE.failedToParseElasticsearchResponse( statusCode, null, e.getMessage(), e );
			}
		}
	}

	private class JsonObjectBodyHandler implements HttpResponse.BodyHandler<JsonObject> {

		@Override
		public HttpResponse.BodySubscriber<JsonObject> apply(HttpResponse.ResponseInfo responseInfo) {
			Charset charset = getCharset( responseInfo.headers() );

			return HttpResponse.BodySubscribers.mapping(
					HttpResponse.BodySubscribers.ofInputStream(),
					new GsonJsonMapper( charset, responseInfo.statusCode() )
			);
		}
	}
}

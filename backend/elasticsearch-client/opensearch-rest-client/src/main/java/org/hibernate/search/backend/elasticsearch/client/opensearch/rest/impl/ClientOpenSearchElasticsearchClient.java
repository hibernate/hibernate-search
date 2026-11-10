package org.hibernate.search.backend.elasticsearch.client.opensearch.rest.impl;

import java.io.IOException;
import java.time.Duration;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonHttpEntityContentProvider;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonResponseConsumerScope;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonTransportSupport;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.opensearch.client.Request;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.Response;
import org.opensearch.client.ResponseException;
import org.opensearch.client.ResponseListener;
import org.opensearch.client.RestClient;
import org.opensearch.client.sniff.Sniffer;

@SuppressWarnings("deprecation")
public class ClientOpenSearchElasticsearchClient implements ElasticsearchClientImplementor {

	private final BeanHolder<? extends RestClient> restClientHolder;

	private final Sniffer sniffer;

	private final SimpleScheduledExecutor timeoutExecutorService;

	private final Optional<Integer> requestTimeoutMs;


	ClientOpenSearchElasticsearchClient(BeanHolder<? extends RestClient> restClientHolder, Sniffer sniffer,
			SimpleScheduledExecutor timeoutExecutorService,
			Optional<Integer> requestTimeoutMs) {
		this.restClientHolder = restClientHolder;
		this.sniffer = sniffer;
		this.timeoutExecutorService = timeoutExecutorService;
		this.requestTimeoutMs = requestTimeoutMs;
	}

	@Override
	public <T> CompletableFuture<ElasticsearchResponse<T>> submit(ElasticsearchRequest request,
			JsonResponseConsumerFactory<T> responseFactory) {
		CompletableFuture<Response> sent;
		try {
			sent = send( request, responseFactory );
		}
		catch (RuntimeException e) {
			return CompletableFuture.failedFuture( e );
		}
		CompletableFuture<ElasticsearchResponse<T>> result =
				sent.thenApply( this::convertResponse );
		result.whenComplete( (response, failure) -> {
			if ( result.isCancelled() ) {
				sent.cancel( true );
			}
		} );
		return result;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T unwrap(Class<T> clientClass) {
		if ( RestClient.class.isAssignableFrom( clientClass ) ) {
			return (T) restClientHolder.get();
		}
		throw ElasticsearchClientLog.INSTANCE.clientUnwrappingWithUnknownType( clientClass, RestClient.class );
	}

	private <T> CompletableFuture<Response> send(ElasticsearchRequest elasticsearchRequest,
			JsonResponseConsumerFactory<T> responseFactory) {
		CompletableFuture<Response> completableFuture = new CompletableFuture<>();
		JsonResponseConsumerScope responseScope = new JsonResponseConsumerScope();

		HttpEntity entity;
		try {
			entity = ClientOpenSearchJsonHttpEntity.toEntity( elasticsearchRequest );
		}
		catch (IOException | RuntimeException e) {
			completableFuture.completeExceptionally( e );
			return completableFuture;
		}

		var cancellation = restClientHolder.get().performRequestAsync(
				toRequest( elasticsearchRequest, entity, responseScope, responseFactory ),
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

		completableFuture.whenComplete( (response, failure) -> {
			if ( failure != null ) {
				if ( entity instanceof JsonHttpEntityContentProvider provider ) {
					provider.cancel();
				}
				responseScope.cancel();
				cancellation.cancel();
			}
		} );

		Deadline deadline = elasticsearchRequest.deadline();
		if ( deadline == null && !requestTimeoutMs.isPresent() ) {
			// no need to schedule a client side timeout
			return completableFuture;
		}

		try {
			long currentTimeoutValue =
					deadline == null ? Long.valueOf( requestTimeoutMs.get() ) : deadline.checkRemainingTimeMillis();

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
			completableFuture.whenComplete( (response, failure) -> timeout.cancel( false ) );
		}
		catch (RuntimeException e) {
			// A deadline or executor failure after submission must still cancel the active transport.
			completableFuture.completeExceptionally( e );
		}

		return completableFuture;
	}

	private <T> Request toRequest(ElasticsearchRequest elasticsearchRequest, HttpEntity entity,
			JsonResponseConsumerScope responseScope,
			JsonResponseConsumerFactory<T> responseFactory) {
		Request request = new Request( elasticsearchRequest.method(), elasticsearchRequest.path() );
		setPerRequestSocketTimeout( elasticsearchRequest, request );
		RequestOptions.Builder options = request.getOptions().toBuilder();
		options.setHttpAsyncResponseConsumerFactory(
				() -> new ClientOpenSearchJsonResponseConsumer<T>( JsonTransportSupport.DEFAULT_LIMIT, responseScope,
						responseFactory ) );
		request.setOptions( options );

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
		int generalRequestTimeoutMs = ( timeToHardTimeout <= Integer.MAX_VALUE ) ? Math.toIntExact( timeToHardTimeout ) : -1;
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectionRequestTimeout( Timeout.DISABLED ) //Disable lease handling for the connection pool! See also HSEARCH-2681
				.setResponseTimeout( generalRequestTimeoutMs, TimeUnit.MILLISECONDS )
				.build();

		RequestOptions.Builder requestOptions = RequestOptions.DEFAULT.toBuilder()
				.setRequestConfig( requestConfig );

		request.setOptions( requestOptions );
	}

	private <T> ElasticsearchResponse<T> convertResponse(Response response) {
		try {
			T body = parseBody( response );
			return new ElasticsearchResponse<>(
					response.getHost().toHostString(),
					response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(),
					body );
		}
		catch (IOException | RuntimeException e) {
			throw ElasticsearchClientLog.INSTANCE.failedToParseElasticsearchResponse( response.getStatusLine().getStatusCode(),
					response.getStatusLine().getReasonPhrase(), e.getMessage(), e );
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T parseBody(Response response) throws IOException {
		HttpEntity entity = response.getEntity();
		if ( entity == null ) {
			return null;
		}

		return ( (ClientOpenSearchJsonResponseConsumer.Entity<T>) entity ).body().result();
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
			throw ElasticsearchClientLog.INSTANCE.unableToShutdownClient( e.getMessage(), e );
		}
	}

}

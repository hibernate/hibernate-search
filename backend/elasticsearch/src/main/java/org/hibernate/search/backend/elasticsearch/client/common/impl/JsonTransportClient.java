package org.hibernate.search.backend.elasticsearch.client.common.impl;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody;
import org.hibernate.search.backend.elasticsearch.client.common.json.impl.JsonResponseBody;
import org.hibernate.search.backend.elasticsearch.client.common.json.impl.MappingJsonResponseConsumer;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonLogHelper;
import org.hibernate.search.backend.elasticsearch.client.common.json.internal.spi.JsonProvider;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.ElasticsearchClientUtils;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchRequestLog;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonResponseConsumer;
import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/** Adapts internal trees to the library-neutral HTTP transport boundary. */
public final class JsonTransportClient implements ElasticsearchJsonClientImplementor {
	private final org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor transport;
	private final JsonCodecService json;
	private final JsonLogHelper jsonLogHelper;
	private final JsonResponseConsumerFactory<ElasticsearchJsonResponseBody> responseConsumerFactory;
	private final Function<JsonNode, ElasticsearchJsonResponseBody> responseDecoder;
	private final Function<
			org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse<ElasticsearchJsonResponseBody>,
			ElasticsearchJsonResponse> responseMapper;

	public JsonTransportClient(
			org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchClientImplementor transport,
			JsonProvider provider) {
		this.transport = transport;
		this.json = provider.getCodec();
		this.jsonLogHelper = provider.getLogHelper();
		this.responseConsumerFactory = this::createResponseConsumer;
		this.responseDecoder = this::decodeResponse;
		this.responseMapper = this::toResponse;
	}

	@Override
	public CompletableFuture<ElasticsearchJsonResponse> submit(ElasticsearchJsonRequest unscoped) {
		long start = System.nanoTime();
		ElasticsearchJsonRequest request = unscoped.withValidation( json.validation() );
		CompletableFuture<org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse<
				ElasticsearchJsonResponseBody>> sent;
		try {
			JsonBody body = request.bodyPartsJson().isEmpty()
					? null
					: ElasticsearchJsonBody.ndjson( request.bodyPartsJson(), json.validation() );
			var wire = new org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest(
					request.method(), request.path(), request.parameters(), body, request.deadline() );
			sent = transport.submit( wire, responseConsumerFactory );
		}
		catch (RuntimeException e) {
			return CompletableFuture.failedFuture( e );
		}
		CompletableFuture<ElasticsearchJsonResponse> result = sent.thenApply( responseMapper );
		result.whenComplete( (response, failure) -> {
			if ( result.isCancelled() ) {
				sent.cancel( true );
			}
		} );
		if ( ElasticsearchRequestLog.INSTANCE.isDebugEnabled() ) {
			result.thenAccept( response -> log( request, start, response ) );
		}
		return result;
	}

	private JsonResponseConsumer<ElasticsearchJsonResponseBody> createResponseConsumer(Charset charset, long limit) {
		return new MappingJsonResponseConsumer<>( new JsonResponseBody( charset, limit, json.validation() ), responseDecoder );
	}

	private ElasticsearchJsonResponseBody decodeResponse(JsonNode tree) {
		return new ElasticsearchJsonResponseBody( json.decode( tree, ObjectNode.class ), json.validation() );
	}

	private ElasticsearchJsonResponse toResponse(
			org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchResponse<
					ElasticsearchJsonResponseBody> response) {
		return new ElasticsearchJsonResponse( response, json.validation() );
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		return transport.unwrap( type );
	}

	@Override
	public void close() {
		transport.close();
	}

	private void log(ElasticsearchJsonRequest request, long start, ElasticsearchJsonResponse response) {
		boolean successCode = ElasticsearchClientUtils.isSuccessCode( response.statusCode() );
		if ( !ElasticsearchRequestLog.INSTANCE.isTraceEnabled() && successCode ) {
			return;
		}
		long executionTimeNs = System.nanoTime() - start;
		long executionTimeMs = TimeUnit.NANOSECONDS.toMillis( executionTimeNs );
		if ( successCode ) {
			ElasticsearchRequestLog.INSTANCE.executedRequest( request.method(), response.hostAndPort(), request.path(),
					request.parameters(),
					request.bodyPartsJson().size(), executionTimeMs,
					response.statusCode(), response.statusMessage(),
					jsonLogHelper.toString( request.bodyPartsJson() ),
					jsonLogHelper.toString( response.bodyJson() ) );
		}
		else {
			ElasticsearchRequestLog.INSTANCE.executedRequestWithFailure( request.method(), response.hostAndPort(),
					request.path(),
					request.parameters(),
					request.bodyPartsJson().size(), executionTimeMs,
					response.statusCode(), response.statusMessage(),
					jsonLogHelper.toString( request.bodyPartsJson() ),
					jsonLogHelper.toString( response.bodyJson() ) );
		}
	}

}

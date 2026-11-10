package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonHandlerBridge;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformer;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchRequestTransformerContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Contracts;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import com.google.gson.JsonObject;

import tools.jackson.databind.node.ObjectNode;

final class ElasticsearchSearchRequestTransformerContextImpl
		implements ElasticsearchSearchRequestTransformerContext {

	static Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> createTransformerFunction(
			ElasticsearchSearchRequestTransformer transformer) {
		return createTransformerFunction( transformer, JsonValidation.systemDefault() );
	}

	static Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> createTransformerFunction(
			ElasticsearchSearchRequestTransformer transformer, JsonValidation validation) {
		if ( transformer == null ) {
			return null;
		}
		return request -> new ElasticsearchSearchRequestTransformerContextImpl( request, validation ).apply( transformer );
	}

	private final ElasticsearchJsonRequest originalRequest;
	private final ObjectNode originalBody;
	private final JsonValidation validation;

	private String path;
	private ObjectNode potentiallyTransformedBody;
	private JsonObject legacyBody;
	private boolean typedBodyEditing;
	private Map<String, String> potentiallyTransformedParametersMap;

	private ElasticsearchSearchRequestTransformerContextImpl(ElasticsearchJsonRequest originalRequest,
			JsonValidation validation) {
		this.originalRequest = originalRequest;
		this.validation = validation;
		List<ObjectNode> originalBodyParts = originalRequest.bodyPartsJson();
		if ( originalBodyParts.size() != 1 ) {
			throw new AssertionFailure(
					"Request transformation was applied to a request with no body part or more than one body parts."
			);
		}
		this.originalBody = originalBodyParts.get( 0 );
		this.path = originalRequest.path();
	}

	@Override
	public String path() {
		return path;
	}

	@Override
	public void path(String newPath) {
		Contracts.assertNotNullNorEmpty( newPath, "newPath" );
		this.path = newPath;
	}

	@Override
	public Map<String, String> parametersMap() {
		// Avoid side-effects on the original request
		if ( potentiallyTransformedParametersMap == null ) {
			potentiallyTransformedParametersMap = new LinkedHashMap<>( originalRequest.parameters() );
		}
		return potentiallyTransformedParametersMap;
	}

	@Override
	@Deprecated
	public JsonObject body() {
		if ( typedBodyEditing ) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( "request body editing",
					new IllegalStateException( "Cannot mix body() and transformBody() in one context" ) );
		}
		if ( legacyBody == null ) {
			legacyBody =
					JsonHandlerBridge.exportObject( GsonJsonHandlers.object(), originalBody, "legacy request body editing",
							validation );
		}
		return legacyBody;
	}

	@Override
	public <T> void transformBody(JsonHandler<T> handler, UnaryOperator<T> transform) {
		if ( legacyBody != null ) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( "request body editing",
					new IllegalStateException( "Cannot mix body() and transformBody() in one context" ) );
		}
		typedBodyEditing = true;
		try {
			Objects.requireNonNull( transform );
			ObjectNode current = potentiallyTransformedBody != null ? potentiallyTransformedBody : originalBody;
			ObjectNode input = JsonHandlerBridge.isTrusted( handler ) ? current.deepCopy() : current;
			T representation = JsonHandlerBridge.exportObject( handler, input, "request body transformation", validation );
			T returned;
			try {
				returned = transform.apply( representation );
			}
			catch (RuntimeException e) {
				throw QueryLog.INSTANCE.jsonHandlerOperationFailed( "request body transformation", e );
			}
			ObjectNode replacement = JsonHandlerBridge.importObject( handler, returned, "request body transformation" );
			potentiallyTransformedBody = replacement;
		}
		catch (SearchException e) {
			throw e;
		}
		catch (RuntimeException e) {
			throw QueryLog.INSTANCE.jsonHandlerOperationFailed( "request body transformation", e );
		}
	}

	public ElasticsearchJsonRequest apply(ElasticsearchSearchRequestTransformer transformer) {
		transformer.transform( this );

		ElasticsearchJsonRequest.Builder builder = ElasticsearchJsonRequest.builder( originalRequest.method() );

		builder.wholeEncodedPath( path );

		Map<String, String> parameters = potentiallyTransformedParametersMap != null
				? potentiallyTransformedParametersMap
				: originalRequest.parameters();
		parameters.forEach( builder::param );

		ObjectNode body = legacyBody != null
				? JsonHandlerBridge.importObject( GsonJsonHandlers.object(), legacyBody, "legacy request body editing" )
				: potentiallyTransformedBody != null ? potentiallyTransformedBody
				: originalBody;
		if ( body != null ) {
			builder.bodyJsonFromInternal( body );
		}

		return builder.build().withValidation( validation );
	}
}

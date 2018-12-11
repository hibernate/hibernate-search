/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.document.converter.FromDocumentFieldValueConverter;
import org.hibernate.search.engine.backend.document.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.query.spi.LoadingResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ElasticsearchFieldProjection<F, T> implements ElasticsearchSearchProjection<T, T> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asObject();

	private final String absoluteFieldPath;
	private final UnknownTypeJsonAccessor hitFieldValueAccessor;

	private final FromDocumentFieldValueConverter<? super F, T> converter;
	private final ElasticsearchFieldCodec<F> codec;

	ElasticsearchFieldProjection(String absoluteFieldPath,
			FromDocumentFieldValueConverter<? super F, T> converter,
			ElasticsearchFieldCodec<F> codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.hitFieldValueAccessor = HIT_SOURCE_ACCESSOR.path( absoluteFieldPath );
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public void contributeRequest(JsonObject requestBody, SearchProjectionExecutionContext searchProjectionExecutionContext) {
		JsonArray source = REQUEST_SOURCE_ACCESSOR.get( requestBody )
				.orElseGet( () -> {
					JsonArray newSource = new JsonArray();
					REQUEST_SOURCE_ACCESSOR.set( requestBody, newSource );
					return newSource;
				} );
		JsonPrimitive fieldPathJson = new JsonPrimitive( absoluteFieldPath );
		if ( !source.contains( fieldPathJson ) ) {
			source.add( fieldPathJson );
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject responseBody, JsonObject hit,
			SearchProjectionExecutionContext searchProjectionExecutionContext) {
		Optional<JsonElement> fieldValue = hitFieldValueAccessor.get( hit );
		FromDocumentFieldValueConvertContext context = searchProjectionExecutionContext.getFromDocumentFieldValueConvertContext();
		if ( fieldValue.isPresent() ) {
			F rawValue = codec.decode( fieldValue.get() );
			return converter.convert( rawValue, context );
		}
		else {
			return converter.convert( null, context );
		}
	}

	@Override
	public T transform(LoadingResult<?> loadingResult, T extractedData) {
		return extractedData;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( "]" );
		return sb.toString();
	}
}

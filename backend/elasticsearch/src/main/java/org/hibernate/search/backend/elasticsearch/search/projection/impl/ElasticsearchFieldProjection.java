/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnknownTypeJsonAccessor;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

class ElasticsearchFieldProjection<F, V> implements ElasticsearchSearchProjection<F, V> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asObject();

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final UnknownTypeJsonAccessor hitFieldValueAccessor;

	private final ProjectionConverter<? super F, V> converter;
	private final ElasticsearchFieldCodec<F> codec;

	ElasticsearchFieldProjection(Set<String> indexNames, String absoluteFieldPath,
			ProjectionConverter<? super F, V> converter,
			ElasticsearchFieldCodec<F> codec) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.hitFieldValueAccessor = HIT_SOURCE_ACCESSOR.path( absoluteFieldPath );
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		JsonArray source = REQUEST_SOURCE_ACCESSOR.getOrCreate( requestBody, JsonArray::new );
		JsonPrimitive fieldPathJson = new JsonPrimitive( absoluteFieldPath );
		if ( !source.contains( fieldPathJson ) ) {
			source.add( fieldPathJson );
		}
	}

	@Override
	public F extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		Optional<JsonElement> fieldValue = hitFieldValueAccessor.get( hit );
		if ( fieldValue.isPresent() ) {
			return codec.decode( fieldValue.get() );
		}
		else {
			return null;
		}
	}

	@Override
	public V transform(LoadingResult<?> loadingResult, F extractedData, SearchProjectionTransformContext context) {
		FromDocumentFieldValueConvertContext convertContext = context.getFromDocumentFieldValueConvertContext();
		return converter.convert( extractedData, convertContext );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
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

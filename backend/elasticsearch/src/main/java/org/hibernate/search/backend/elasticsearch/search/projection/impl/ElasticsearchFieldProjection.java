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
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * A projection on the values of an index field.
 *
 * @param <E> The type of aggregated values extracted from the backend response (before conversion).
 * @param <P> The type of aggregated values returned by the projection (after conversion).
 * @param <F> The type of individual field values obtained from the backend (before conversion).
 * @param <V> The type of individual field values after conversion.
 */
class ElasticsearchFieldProjection<E, P, F, V> implements ElasticsearchSearchProjection<E, P> {

	private static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asArray();
	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR = JsonAccessor.root().property( "_source" ).asObject();

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final UnknownTypeJsonAccessor hitFieldValueAccessor;

	private final ElasticsearchFieldCodec<F> codec;
	private final ProjectionConverter<? super F, V> converter;
	private final ProjectionAccumulator<F, V, E, P> accumulator;

	ElasticsearchFieldProjection(Set<String> indexNames, String absoluteFieldPath,
			ElasticsearchFieldCodec<F> codec, ProjectionConverter<? super F, V> converter,
			ProjectionAccumulator<F, V, E, P> accumulator) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.hitFieldValueAccessor = HIT_SOURCE_ACCESSOR.path( absoluteFieldPath );
		this.accumulator = accumulator;
		this.converter = converter;
		this.codec = codec;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", accumulator=" + accumulator
				+ "]";
	}

	@Override
	public void request(JsonObject requestBody, SearchProjectionRequestContext context) {
		JsonPrimitive fieldPathJson = new JsonPrimitive( absoluteFieldPath );
		REQUEST_SOURCE_ACCESSOR.addElementIfAbsent( requestBody, fieldPathJson );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> projectionHitMapper, JsonObject hit,
			SearchProjectionExtractContext context) {
		E extracted = accumulator.createInitial();
		Optional<JsonElement> fieldValue = hitFieldValueAccessor.get( hit );
		if ( fieldValue.isPresent() ) {
			F decoded = codec.decode( fieldValue.get() );
			extracted = accumulator.accumulate( extracted, decoded );
		}
		return extracted;
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, E extractedData, SearchProjectionTransformContext context) {
		FromDocumentFieldValueConvertContext convertContext = context.getFromDocumentFieldValueConvertContext();
		return accumulator.finish( extractedData, converter, convertContext );
	}

	@Override
	public Set<String> getIndexNames() {
		return indexNames;
	}
}

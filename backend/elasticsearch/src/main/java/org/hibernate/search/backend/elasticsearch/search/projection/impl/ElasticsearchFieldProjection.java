/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnexpectedJsonElementTypeException;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
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
	private final String[] absoluteFieldPathComponents;

	private final Function<JsonElement, F> decodeFunction;
	private final ProjectionConverter<? super F, V> converter;
	private final ProjectionAccumulator<F, V, E, P> accumulator;

	ElasticsearchFieldProjection(Set<String> indexNames, String absoluteFieldPath, String[] absoluteFieldPathComponents,
			Function<JsonElement, F> decodeFunction, ProjectionConverter<? super F, V> converter,
			ProjectionAccumulator<F, V, E, P> accumulator) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.absoluteFieldPathComponents = absoluteFieldPathComponents;
		this.decodeFunction = decodeFunction;
		this.converter = converter;
		this.accumulator = accumulator;
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
		JsonObject source = HIT_SOURCE_ACCESSOR.get( hit ).get();
		extracted = collect( source, extracted, 0 );
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

	private E collect(JsonObject parent, E accumulated, int currentPathComponentIndex) {
		JsonElement child = parent.get( absoluteFieldPathComponents[currentPathComponentIndex] );

		if ( currentPathComponentIndex == (absoluteFieldPathComponents.length - 1) ) {
			// We reached the field we want to collect.
			if ( child == null ) {
				// Not present
				return accumulated;
			}
			else if ( child.isJsonNull() ) {
				// Present, but null
				return accumulator.accumulate( accumulated, null );
			}
			else if ( child.isJsonArray() ) {
				for ( JsonElement childElement : child.getAsJsonArray() ) {
					F decoded = decodeFunction.apply( childElement );
					accumulated = accumulator.accumulate( accumulated, decoded );
				}
				return accumulated;
			}
			else {
				F decoded = decodeFunction.apply( child );
				return accumulator.accumulate( accumulated, decoded );
			}
		}
		else {
			// We just reached an intermediary object field leading to the field we want to collect.
			if ( child == null || child.isJsonNull() ) {
				// Not present
				return accumulated;
			}
			else if ( child.isJsonArray() ) {
				for ( JsonElement childElement : child.getAsJsonArray() ) {
					JsonObject childElementAsObject = toJsonObject( childElement, currentPathComponentIndex );
					accumulated = collect( childElementAsObject, accumulated, currentPathComponentIndex + 1 );
				}
				return accumulated;
			}
			else {
				JsonObject childAsObject = toJsonObject( child, currentPathComponentIndex );
				return collect( childAsObject, accumulated, currentPathComponentIndex + 1 );
			}
		}
	}

	private JsonObject toJsonObject(JsonElement childElement, int currentPathComponentIndex) {
		if ( !JsonElementTypes.OBJECT.isInstance( childElement ) ) {
			throw new UnexpectedJsonElementTypeException(
					Arrays.stream( absoluteFieldPathComponents, 0, currentPathComponentIndex + 1 )
							.collect( Collectors.joining( "." ) ),
					JsonElementTypes.OBJECT, childElement
			);
		}
		return childElement.getAsJsonObject();
	}
}

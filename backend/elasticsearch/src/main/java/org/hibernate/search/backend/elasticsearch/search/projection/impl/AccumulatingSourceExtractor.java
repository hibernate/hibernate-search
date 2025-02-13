/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.gson.impl.UnexpectedJsonElementTypeException;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.ProjectionCollector;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

abstract class AccumulatingSourceExtractor<E, V, A, P>
		implements ElasticsearchSearchProjection.Extractor<A, P> {

	static final JsonArrayAccessor REQUEST_SOURCE_ACCESSOR =
			JsonAccessor.root().property( "_source" ).asArray();

	private final String[] fieldPathComponents;
	final ProjectionCollector<E, V, A, P> collector;

	public AccumulatingSourceExtractor(String[] fieldPathComponents,
			ProjectionCollector<E, V, A, P> collector) {
		this.fieldPathComponents = fieldPathComponents;
		this.collector = collector;
	}

	@Override
	public final A extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
			JsonObject source, ProjectionExtractContext context) {
		A accumulated = collector.createInitial();
		accumulated = collect( projectionHitMapper, hit, source, context, accumulated, 0 );
		return accumulated;
	}

	private A collect(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
			JsonObject parent, ProjectionExtractContext context,
			A accumulated, int currentPathComponentIndex) {
		if ( parent == null ) {
			return accumulated;
		}

		JsonElement child = parent.get( fieldPathComponents[currentPathComponentIndex] );

		if ( currentPathComponentIndex == ( fieldPathComponents.length - 1 ) ) {
			// We reached the field we want to collect.
			return collectTargetField( projectionHitMapper, hit, child, context, accumulated );
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
					accumulated = collect( projectionHitMapper, hit, childElementAsObject, context,
							accumulated, currentPathComponentIndex + 1 );
				}
				return accumulated;
			}
			else {
				JsonObject childAsObject = toJsonObject( child, currentPathComponentIndex );
				return collect( projectionHitMapper, hit, childAsObject, context,
						accumulated, currentPathComponentIndex + 1 );
			}
		}
	}

	private A collectTargetField(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit, JsonElement fieldValue,
			ProjectionExtractContext context, A accumulated) {
		if ( fieldValue == null ) {
			// Not present
			return accumulated;
		}
		else if ( fieldValue.isJsonNull() ) {
			// Present, but null
			return collector.accumulate( accumulated, extract( projectionHitMapper, hit, fieldValue, context ) );
		}
		else if ( !canDecodeArrays() && fieldValue.isJsonArray() ) {
			for ( JsonElement childElement : fieldValue.getAsJsonArray() ) {
				accumulated = collector.accumulate( accumulated,
						extract( projectionHitMapper, hit, childElement, context ) );
			}
			return accumulated;
		}
		else {
			return collector.accumulate( accumulated, extract( projectionHitMapper, hit, fieldValue, context ) );
		}
	}

	protected abstract E extract(ProjectionHitMapper<?> projectionHitMapper, JsonObject hit,
			JsonElement sourceElement, ProjectionExtractContext context);

	protected abstract boolean canDecodeArrays();

	private JsonObject toJsonObject(JsonElement childElement, int currentPathComponentIndex) {
		if ( childElement == null || childElement.isJsonNull() ) {
			return null;
		}
		if ( !JsonElementTypes.OBJECT.isInstance( childElement ) ) {
			throw new UnexpectedJsonElementTypeException(
					Arrays.stream( fieldPathComponents, 0, currentPathComponentIndex + 1 )
							.collect( Collectors.joining( "." ) ),
					JsonElementTypes.OBJECT, childElement
			);
		}
		return childElement.getAsJsonObject();
	}
}

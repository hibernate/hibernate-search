package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonElementTypes;
import org.hibernate.search.backend.elasticsearch.json.impl.UnexpectedJsonElementTypeException;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.ProjectionCollector;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

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
	public final A extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			ObjectNode source, ProjectionExtractContext context) {
		A accumulated = collector.createInitial();
		accumulated = collect( projectionHitMapper, hit, source, context, accumulated, 0 );
		return accumulated;
	}

	private A collect(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			ObjectNode parent, ProjectionExtractContext context,
			A accumulated, int currentPathComponentIndex) {
		if ( parent == null ) {
			return accumulated;
		}

		JsonNode child = parent.get( fieldPathComponents[currentPathComponentIndex] );

		if ( currentPathComponentIndex == ( fieldPathComponents.length - 1 ) ) {
			// We reached the field we want to collect.
			return collectTargetField( projectionHitMapper, hit, child, context, accumulated );
		}
		else {
			// We just reached an intermediary object field leading to the field we want to collect.
			if ( child == null || child.isNull() ) {
				// Not present
				return accumulated;
			}
			else if ( child.isArray() ) {
				for ( JsonNode childElement : ( (ArrayNode) child ) ) {
					ObjectNode childElementAsObject = toJsonObject( childElement, currentPathComponentIndex );
					accumulated = collect( projectionHitMapper, hit, childElementAsObject, context,
							accumulated, currentPathComponentIndex + 1 );
				}
				return accumulated;
			}
			else {
				ObjectNode childAsObject = toJsonObject( child, currentPathComponentIndex );
				return collect( projectionHitMapper, hit, childAsObject, context,
						accumulated, currentPathComponentIndex + 1 );
			}
		}
	}

	private A collectTargetField(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit, JsonNode fieldValue,
			ProjectionExtractContext context, A accumulated) {
		if ( fieldValue == null ) {
			// Not present
			return accumulated;
		}
		else if ( fieldValue.isNull() ) {
			// Present, but null
			return collector.accumulate( accumulated, extract( projectionHitMapper, hit, fieldValue, context ) );
		}
		else if ( fieldValue.isArray() && !canDecodeArrays( hit, context ) ) {
			for ( JsonNode childElement : ( (ArrayNode) fieldValue ) ) {
				accumulated = collector.accumulate( accumulated,
						extract( projectionHitMapper, hit, childElement, context ) );
			}
			return accumulated;
		}
		else {
			return collector.accumulate( accumulated, extract( projectionHitMapper, hit, fieldValue, context ) );
		}
	}

	protected abstract E extract(ProjectionHitMapper<?> projectionHitMapper, ObjectNode hit,
			JsonNode sourceElement, ProjectionExtractContext context);

	protected abstract boolean canDecodeArrays(ObjectNode hit, ProjectionExtractContext context);

	private ObjectNode toJsonObject(JsonNode childElement, int currentPathComponentIndex) {
		if ( childElement == null || childElement.isNull() ) {
			return null;
		}
		if ( !JsonElementTypes.OBJECT.isInstance( childElement ) ) {
			throw new UnexpectedJsonElementTypeException(
					Arrays.stream( fieldPathComponents, 0, currentPathComponentIndex + 1 )
							.collect( Collectors.joining( "." ) ),
					JsonElementTypes.OBJECT, childElement
			);
		}
		return ( (ObjectNode) childElement );
	}
}

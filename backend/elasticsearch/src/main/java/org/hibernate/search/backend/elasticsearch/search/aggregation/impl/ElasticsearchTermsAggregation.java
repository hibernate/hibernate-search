package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import static org.hibernate.search.backend.elasticsearch.search.aggregation.impl.AggregationRequestBuildingContextContext.buildingContextKey;
import static org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchCompositeAggregation.compositeKeyFor;

import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicate;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;
import org.hibernate.search.engine.search.aggregation.spi.TermsAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.impl.CollectionHelper;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class ElasticsearchTermsAggregation<F, K, T, V>
		extends AbstractElasticsearchBucketAggregation<K, V> {

	private final String absoluteFieldPath;

	private final ProjectionConverter<T, ? extends K> fromFieldValueConverter;
	private final BiFunction<JsonNode, JsonNode, T> decodeFunction;

	private final ElasticsearchSearchAggregation<V> aggregation;

	private final ObjectNode order;
	private final int size;
	private final int minDocCount;

	private ElasticsearchTermsAggregation(Builder<F, K, T, V> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.fromFieldValueConverter = builder.fromFieldValueConverter;
		this.decodeFunction = builder.decodeFunction;
		this.order = builder.order;
		this.size = builder.size;
		this.minDocCount = builder.minDocCount;
		this.aggregation = builder.aggregation;
	}

	@Override
	protected void doRequest(ObjectNode outerObject, ObjectNode innerObject, AggregationRequestBuildingContextContext context) {
		ObjectNode termsDefinition = JsonNodeFactory.instance.objectNode();
		outerObject.set( "terms", termsDefinition );
		termsDefinition.put( "field", absoluteFieldPath );
		if ( order != null ) {
			termsDefinition.set( "order", order );
		}
		termsDefinition.put( "size", size );
		termsDefinition.put( "min_doc_count", minDocCount );

		context.add( buildingContextKey( INNER_EXTRACTOR ),
				aggregation.request( context, compositeKeyFor( isNested() ), innerObject ) );

	}

	@Override
	protected Extractor<Map<K, V>> extractor(AggregationKey<?> key, AggregationRequestBuildingContextContext context) {
		Extractor<V> innerExtractor = context.get( buildingContextKey( INNER_EXTRACTOR ) );
		return new TermsBucketExtractor( key, nestedPathHierarchy, filter, innerExtractor );
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<TermsAggregationBuilder.TypeSelector, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TermsAggregationBuilder.TypeSelector create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F> implements TermsAggregationBuilder.TypeSelector {
		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> Builder<F, T, ?, Long> type(Class<T> expectedType, ValueModel valueModel) {
			if ( ValueModel.RAW.equals( valueModel ) ) {
				return new CountBuilder<>(
						scope, field,
						(key, string) -> string != null && !string.isNull() ? string : key,
						// unchecked cast to make eclipse-compiler happy
						// we know that Elasticsearch projection converters work with the String
						( (ProjectionConverter<JsonNode, ?>) field.type().rawProjectionConverter() )
								.withConvertedType( expectedType, field )
				);
			}
			else {
				return new CountBuilder<>( scope, field, codec::decodeAggregationKey,
						field.type().projectionConverter( valueModel ).withConvertedType( expectedType, field ) );
			}
		}
	}

	protected class TermsBucketExtractor extends AbstractBucketExtractor<K, V> {
		private final Extractor<V> innerExtractor;

		protected TermsBucketExtractor(AggregationKey<?> key, List<String> nestedPathHierarchy,
				ElasticsearchSearchPredicate filter, Extractor<V> innerExtractor) {
			super( key, nestedPathHierarchy, filter );
			this.innerExtractor = innerExtractor;
		}

		@Override
		protected Map<K, V> doExtract(AggregationExtractContext context, JsonNode buckets) {
			ArrayNode bucketArray = ( (ArrayNode) buckets );
			Map<K, V> result = CollectionHelper.newLinkedHashMap( bucketArray.size() );
			FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
			for ( JsonNode bucketElement : bucketArray ) {
				ObjectNode bucket = ( (ObjectNode) bucketElement );
				JsonNode keyJson = bucket.get( "key" );
				JsonNode keyAsStringJson = bucket.get( "key_as_string" );
				K key = fromFieldValueConverter.fromDocumentValue(
						decodeFunction.apply( keyJson, keyAsStringJson ),
						convertContext
				);
				result.put( key, innerExtractor.extract( bucket, context ) );
			}
			return result;
		}
	}

	private static class CountBuilder<F, K, T> extends Builder<F, K, T, Long> {

		protected CountBuilder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				BiFunction<JsonNode, JsonNode, T> decodeFunction,
				ProjectionConverter<T, ? extends K> fromFieldValueConverter) {
			super( scope, field, decodeFunction, fromFieldValueConverter,
					ElasticsearchSearchAggregation.from( scope,
							ElasticsearchCountDocumentAggregation.factory().create( scope, field ).builder().build() ) );
		}
	}

	private static class Builder<F, K, T, V> extends AbstractBuilder<K, V>
			implements TermsAggregationBuilder<K, V> {

		private final BiFunction<JsonNode, JsonNode, T> decodeFunction;
		private final ProjectionConverter<T, ? extends K> fromFieldValueConverter;
		private final ElasticsearchSearchAggregation<V> aggregation;

		private ObjectNode order;
		private int minDocCount;
		private int size;

		private Builder(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field,
				BiFunction<JsonNode, JsonNode, T> decodeFunction,
				ProjectionConverter<T, ? extends K> fromFieldValueConverter,
				ElasticsearchSearchAggregation<V> aggregation) {
			this( scope, field, decodeFunction, fromFieldValueConverter, aggregation, null, 1, 100 );
		}

		public Builder(ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<?> field,
				BiFunction<JsonNode, JsonNode, T> decodeFunction,
				ProjectionConverter<T, ? extends K> fromFieldValueConverter,
				ElasticsearchSearchAggregation<V> aggregation,
				ObjectNode order, int minDocCount, int size) {
			super( scope, field );
			this.order = order;
			this.decodeFunction = decodeFunction;
			this.fromFieldValueConverter = fromFieldValueConverter;
			this.aggregation = aggregation;
			this.minDocCount = minDocCount;
			this.size = size;
		}

		@Override
		public void orderByCountDescending() {
			order( "_count", "desc" );
		}

		@Override
		public void orderByCountAscending() {
			order( "_count", "asc" );
		}

		@Override
		public void orderByTermAscending() {
			order( scope.searchSyntax().getTermAggregationOrderByTermToken(), "asc" );
		}

		@Override
		public void orderByTermDescending() {
			order( scope.searchSyntax().getTermAggregationOrderByTermToken(), "desc" );
		}

		@Override
		public void minDocumentCount(int minDocumentCount) {
			this.minDocCount = minDocumentCount;
		}

		@Override
		public void maxTermCount(int maxTermCount) {
			this.size = maxTermCount;
		}

		@Override
		public <R> TermsAggregationBuilder<K, R> withValue(SearchAggregation<R> aggregation) {
			return new Builder<>( scope, field, decodeFunction, fromFieldValueConverter,
					ElasticsearchSearchAggregation.from( scope, aggregation ), order, minDocCount, size );
		}

		@Override
		public ElasticsearchTermsAggregation<F, K, T, V> build() {
			return new ElasticsearchTermsAggregation<>( this );
		}

		protected final void order(String key, String order) {
			ObjectNode orderObject = JsonNodeFactory.instance.objectNode();
			orderObject.put( key, order );
			this.order = orderObject;
		}
	}
}

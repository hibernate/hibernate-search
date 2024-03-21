/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.parameter;
import static org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider.simple;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.DslConverter;
import org.hibernate.search.engine.search.aggregation.spi.RangeAggregationBuilder;
import org.hibernate.search.engine.search.common.ValueConvert;
import org.hibernate.search.engine.search.query.spi.QueryParametersContext;
import org.hibernate.search.engine.search.query.spi.QueryParametersValueProvider;
import org.hibernate.search.util.common.data.Range;
import org.hibernate.search.util.common.data.RangeBoundInclusion;
import org.hibernate.search.util.common.impl.CollectionHelper;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @param <F> The type of field values.
 * @param <K> The type of keys in the returned map. It can be {@code F}
 * or a different type if value converters are used.
 */
public class ElasticsearchRangeAggregation<F, K>
		extends AbstractElasticsearchBucketAggregation<Range<K>, Long> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String absoluteFieldPath;

	private final List<QueryParametersValueProvider<Range<K>>> rangeProvidersInOrder;
	private final List<QueryParametersValueProvider<JsonObject>> rangeJsonProvidersInOrder;
	private List<Range<K>> rangesInOrder;
	private JsonArray rangesJson;

	private ElasticsearchRangeAggregation(Builder<F, K> builder) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.rangeProvidersInOrder = builder.rangeProvidersInOrder;
		this.rangeJsonProvidersInOrder = builder.rangeJsonProvidersInOrder;
	}

	@Override
	protected void doRequest(AggregationRequestContext context, JsonObject outerObject, JsonObject innerObject) {
		produceValuesFromProviders( context.getRootPredicateContext().toQueryParametersContext() );
		outerObject.add( "range", innerObject );
		innerObject.addProperty( "field", absoluteFieldPath );
		innerObject.addProperty( "keyed", true );
		innerObject.add( "ranges", rangesJson );
	}

	private void produceValuesFromProviders(QueryParametersContext parametersContext) {
		this.rangesInOrder = new ArrayList<>();
		for ( QueryParametersValueProvider<Range<K>> provider : rangeProvidersInOrder ) {
			rangesInOrder.add( provider.provide( parametersContext ) );
		}
		this.rangesJson = new JsonArray();
		for ( QueryParametersValueProvider<JsonObject> provider : rangeJsonProvidersInOrder ) {
			rangesJson.add( provider.provide( parametersContext ) );
		}
	}

	@Override
	protected Map<Range<K>, Long> doExtract(AggregationExtractContext context, JsonElement buckets) {
		JsonObject bucketMap = buckets.getAsJsonObject();
		Map<Range<K>, Long> result = CollectionHelper.newLinkedHashMap( rangesJson.size() );
		for ( int i = 0; i < rangesJson.size(); i++ ) {
			JsonObject bucket = bucketMap.get( String.valueOf( i ) ).getAsJsonObject();
			Range<K> range = rangesInOrder.get( i );
			long documentCount = getBucketDocCount( bucket );
			result.put( range, documentCount );
		}
		return result;
	}

	public static class Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<RangeAggregationBuilder.TypeSelector, F> {
		public Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public TypeSelector<?> create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new TypeSelector<>( codec, scope, field );
		}
	}

	private static class TypeSelector<F> implements RangeAggregationBuilder.TypeSelector {
		private final ElasticsearchFieldCodec<F> codec;
		private final ElasticsearchSearchIndexScope<?> scope;
		private final ElasticsearchSearchIndexValueFieldContext<F> field;

		private TypeSelector(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexScope<?> scope, ElasticsearchSearchIndexValueFieldContext<F> field) {
			this.codec = codec;
			this.scope = scope;
			this.field = field;
		}

		@Override
		public <T> Builder<F, T> type(Class<T> expectedType, ValueConvert convert) {
			return new Builder<>( codec, scope, field,
					field.type().dslConverter( convert ).withInputType( expectedType, field ) );
		}
	}

	private static class Builder<F, K> extends AbstractBuilder<Range<K>, Long>
			implements RangeAggregationBuilder<K> {

		private final ElasticsearchFieldCodec<F> codec;
		private final DslConverter<? super K, F> toFieldValueConverter;

		private final List<QueryParametersValueProvider<Range<K>>> rangeProvidersInOrder = new ArrayList<>();
		private final List<QueryParametersValueProvider<JsonObject>> rangeJsonProvidersInOrder = new ArrayList<>();

		private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field, DslConverter<? super K, F> toFieldValueConverter) {
			super( scope, field );
			this.codec = codec;
			this.toFieldValueConverter = toFieldValueConverter;
		}

		@Override
		public void range(Range<? extends K> range) {
			Range<K> mappedRange = range.map( Function.identity() );
			rangeProvidersInOrder.add( simple( mappedRange ) );

			int position = rangeJsonProvidersInOrder.size();
			JsonObject jsonObject = rangeJson( codec, field, scope, toFieldValueConverter, mappedRange, position );
			rangeJsonProvidersInOrder.add( simple( jsonObject ) );
		}

		@Override
		@SuppressWarnings("unchecked")
		public void param(String parameterName) {
			rangeProvidersInOrder.add( parameter( parameterName, Range.class, r -> r ) );

			int position = rangeJsonProvidersInOrder.size();
			QueryParametersValueProvider<JsonObject> provider = parameter( parameterName, Range.class,
					r -> rangeJson( codec, field, scope, toFieldValueConverter, (Range<K>) r, position )
			);
			rangeJsonProvidersInOrder.add( provider );
		}

		@Override
		public ElasticsearchRangeAggregation<F, K> build() {
			return new ElasticsearchRangeAggregation<>( this );
		}

		private static <F, T> JsonObject rangeJson(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexValueFieldContext<?> field, ElasticsearchSearchIndexScope<?> scope,
				DslConverter<? super T, F> toFieldValueConverter, Range<? extends T> range, int position) {
			JsonObject rangeJson = new JsonObject();
			Optional<? extends T> lowerBoundValue = range.lowerBoundValue();
			if ( lowerBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.INCLUDED.equals( range.lowerBoundInclusion() ) ) {
					throw log.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "from",
						convertToFieldValue( codec, field, scope, toFieldValueConverter, lowerBoundValue.get() ) );
			}
			Optional<? extends T> upperBoundValue = range.upperBoundValue();
			if ( upperBoundValue.isPresent() ) {
				if ( !RangeBoundInclusion.EXCLUDED.equals( range.upperBoundInclusion() ) ) {
					throw log.elasticsearchRangeAggregationRequiresCanonicalFormForRanges( range );
				}
				rangeJson.add( "to", convertToFieldValue( codec, field, scope, toFieldValueConverter, upperBoundValue.get() ) );
			}
			// We need to request a keyed response,
			// because ranges are not always returned in the order they are submitted
			rangeJson.addProperty( "key", String.valueOf( position ) );
			return rangeJson;
		}

		private static <F, K> JsonElement convertToFieldValue(ElasticsearchFieldCodec<F> codec,
				ElasticsearchSearchIndexValueFieldContext<?> field, ElasticsearchSearchIndexScope<?> scope,
				DslConverter<? super K, F> toFieldValueConverter, K value) {
			try {
				F converted = toFieldValueConverter.toDocumentValue( value, scope.toDocumentValueConvertContext() );
				return codec.encodeForAggregation( scope.searchSyntax(), converted );
			}
			catch (RuntimeException e) {
				throw log.cannotConvertDslParameter( e.getMessage(), e, field.eventContext() );
			}
		}
	}
}

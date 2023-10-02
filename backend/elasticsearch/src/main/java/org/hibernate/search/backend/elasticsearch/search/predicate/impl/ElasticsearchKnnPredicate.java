/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonArrayAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.search.common.impl.AbstractElasticsearchCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexValueFieldContext;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchFieldCodec;
import org.hibernate.search.backend.elasticsearch.types.codec.impl.ElasticsearchVectorFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public abstract class ElasticsearchKnnPredicate extends AbstractElasticsearchSingleFieldPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final ElasticsearchSearchPredicate filter;
	protected final int k;
	protected final JsonArray vector;
	protected final Float similarity;

	private ElasticsearchKnnPredicate(AbstractKnnBuilder<?> builder) {
		super( builder );
		this.filter = builder.filter;
		this.k = builder.k;
		this.vector = builder.vector;
		this.similarity = builder.similarity;
		builder.filter = null;
		builder.vector = null;
	}

	protected JsonObject prepareFilter(PredicateRequestContext context) {
		JsonObject mainFilter = filter == null ? null : filter.toJsonQuery( context );
		JsonArray filters = context.tenantAndRoutingFilters();
		if ( context.getNestedPath() == null ) {
			return Queries.boolCombineMust( mainFilter, filters );
		}
		else if ( !filters.isEmpty() ) {
			log.knnUsedInNestedContextRequiresFilters();
		}
		return mainFilter;
	}

	public static class Elasticsearch812Factory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<KnnPredicateBuilder, F> {
		public Elasticsearch812Factory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public KnnPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new Elasticsearch812Impl.Builder<>( codec, scope, field );
		}
	}

	public static class OpenSearchFactory<F>
			extends AbstractElasticsearchCodecAwareSearchQueryElementFactory<KnnPredicateBuilder, F> {
		public OpenSearchFactory(ElasticsearchFieldCodec<F> codec) {
			super( codec );
		}

		@Override
		public KnnPredicateBuilder create(ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			return new OpenSearchImpl.Builder<>( codec, scope, field );
		}
	}

	private abstract static class AbstractKnnBuilder<F> extends AbstractBuilder implements KnnPredicateBuilder {

		private final Class<?> vectorElementsType;
		private final int indexedVectorsDimension;
		private int k;
		private JsonArray vector;
		private ElasticsearchSearchPredicate filter;
		protected Float similarity;

		private AbstractKnnBuilder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
				ElasticsearchSearchIndexValueFieldContext<F> field) {
			super( scope, field );
			if ( codec instanceof ElasticsearchVectorFieldCodec ) {
				ElasticsearchVectorFieldCodec<F> vectorCodec = (ElasticsearchVectorFieldCodec<F>) codec;
				vectorElementsType = vectorCodec.vectorElementsType();
				indexedVectorsDimension = vectorCodec.getConfiguredDimensions();
			}
			else {
				// shouldn't really happen as if someone tries this it should fail on `queryElementFactory` lookup.
				throw new AssertionFailure( "Attempting to use a knn predicate on a non-vector field." );
			}
		}

		@Override
		public void k(int k) {
			this.k = k;
		}

		@Override
		public void vector(Object vector) {
			if ( !vector.getClass().isArray() ) {
				throw new IllegalArgumentException( "Vector can only be either a float or a byte array (float[], byte[])." );
			}
			if ( !vectorElementsType.equals( vector.getClass().getComponentType() ) ) {
				throw log.vectorKnnMatchVectorTypeDiffersFromField( absoluteFieldPath, vectorElementsType,
						vector.getClass().getComponentType() );
			}
			if ( Array.getLength( vector ) != indexedVectorsDimension ) {
				throw log.vectorKnnMatchVectorDimensionDiffersFromField( absoluteFieldPath, indexedVectorsDimension,
						Array.getLength( vector )
				);
			}
			this.vector = vectorToJsonArray( vector, vectorElementsType );
		}

		@Override
		public void filter(SearchPredicate filter) {
			ElasticsearchSearchPredicate elasticsearchFilter = ElasticsearchSearchPredicate.from( scope, filter );
			elasticsearchFilter.checkNestableWithin( PredicateNestingContext.simple() );
			this.filter = elasticsearchFilter;
		}

	}

	private static JsonArray vectorToJsonArray(Object vector, Class<?> vectorElementsType) {
		// we know it is an array since we've checked it when we got the vector
		int length = Array.getLength( vector );
		JsonArray array = new JsonArray( length );
		for ( int i = 0; i < length; i++ ) {
			if ( byte.class.equals( vectorElementsType ) ) {
				array.add( Array.getByte( vector, i ) );
			}
			else {
				array.add( Array.getFloat( vector, i ) );
			}
		}
		return array;
	}

	private static class Elasticsearch812Impl extends ElasticsearchKnnPredicate {

		private static final JsonObjectAccessor KNN_ACCESSOR = JsonAccessor.root().property( "knn" ).asObject();
		private static final JsonAccessor<String> FIELD_ACCESSOR = JsonAccessor.root().property( "field" ).asString();
		private static final JsonArrayAccessor VECTOR_ACCESSOR = JsonAccessor.root().property( "query_vector" ).asArray();
		private static final JsonObjectAccessor FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();
		private static final JsonAccessor<Integer> NUM_CANDIDATES_ACCESSOR =
				JsonAccessor.root().property( "num_candidates" ).asInteger();
		private static final JsonAccessor<Float> SIMILARITY_ACCESSOR = JsonAccessor.root().property( "similarity" ).asFloat();


		private Elasticsearch812Impl(Builder<?> builder) {
			super( builder );
		}

		@Override
		protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
			KNN_ACCESSOR.set( outerObject, innerObject );

			FIELD_ACCESSOR.set( innerObject, absoluteFieldPath );
			NUM_CANDIDATES_ACCESSOR.set( innerObject, k );
			VECTOR_ACCESSOR.set( innerObject, vector );

			JsonObject filter = prepareFilter( context );
			if ( filter != null ) {
				FILTER_ACCESSOR.set( innerObject, filter );
			}
			if ( similarity != null ) {
				SIMILARITY_ACCESSOR.set( innerObject, similarity );
			}

			return outerObject;
		}

		private static class Builder<F> extends AbstractKnnBuilder<F> {

			private Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
					ElasticsearchSearchIndexValueFieldContext<F> field) {
				super( codec, scope, field );
			}

			@Override
			public void requiredMinimumSimilarity(float similarity) {
				this.similarity = similarity;
			}

			@Override
			public SearchPredicate build() {
				return new Elasticsearch812Impl( this );
			}
		}
	}

	private static class OpenSearchImpl extends ElasticsearchKnnPredicate {

		private static final JsonObjectAccessor KNN_ACCESSOR = JsonAccessor.root().property( "knn" ).asObject();
		private static final JsonArrayAccessor VECTOR_ACCESSOR = JsonAccessor.root().property( "vector" ).asArray();
		private static final JsonAccessor<Integer> K_ACCESSOR = JsonAccessor.root().property( "k" ).asInteger();

		private static final JsonObjectAccessor FILTER_ACCESSOR = JsonAccessor.root().property( "filter" ).asObject();

		private OpenSearchImpl(Builder<?> builder) {
			super( builder );
		}

		@Override
		protected JsonObject doToJsonQuery(PredicateRequestContext context, JsonObject outerObject, JsonObject innerObject) {
			JsonObject field = new JsonObject();
			KNN_ACCESSOR.set( outerObject, field );

			field.add( absoluteFieldPath, innerObject );
			JsonObject filter = prepareFilter( context );
			if ( filter != null ) {
				FILTER_ACCESSOR.set( innerObject, filter );
			}
			K_ACCESSOR.set( innerObject, k );
			VECTOR_ACCESSOR.set( innerObject, vector );

			return outerObject;
		}

		protected static class Builder<F> extends AbstractKnnBuilder<F> {
			protected Builder(ElasticsearchFieldCodec<F> codec, ElasticsearchSearchIndexScope<?> scope,
					ElasticsearchSearchIndexValueFieldContext<F> field) {
				super( codec, scope, field );
			}

			@Override
			public void requiredMinimumSimilarity(float similarity) {
				throw log.knnRequiredMinimumSimilarityUnsupportedOption();
			}

			@Override
			public SearchPredicate build() {
				return new OpenSearchImpl( this );
			}
		}
	}
}

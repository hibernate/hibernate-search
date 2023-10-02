/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.predicate.impl;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Array;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.VectorSimilarityFilterQuery;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneValueFieldSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneVectorFieldCodec;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.spi.KnnPredicateBuilder;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.VectorSimilarityFunction;
import org.apache.lucene.search.KnnByteVectorQuery;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.Query;

public abstract class LuceneKnnPredicate<T> extends AbstractLuceneSingleFieldPredicate implements LuceneSearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	protected final int k;
	protected final T vector;
	protected final Float similarity;
	protected final VectorSimilarityFunction similarityFunction;
	private final LuceneSearchPredicate filter;

	private LuceneKnnPredicate(Builder<T> builder) {
		super( builder );
		this.k = builder.k;
		this.vector = builder.vector;
		this.filter = builder.filter;
		this.similarity = builder.similarity;
		this.similarityFunction = builder.similarityFunction;
	}

	protected Query prepareFilter(PredicateRequestContext context) {
		return context.appendTenantAndRoutingFilters( filter == null ? null : filter.toQuery( context ) );
	}

	private abstract static class Builder<F> extends AbstractBuilder implements KnnPredicateBuilder {
		private final Class<?> vectorElementsType;
		private final int indexedVectorsDimension;
		private final VectorSimilarityFunction similarityFunction;
		private final LuceneVectorFieldCodec<F> vectorCodec;
		private int k;
		private F vector;
		private LuceneSearchPredicate filter;
		private Float similarity;

		protected Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			super( scope, field );

			LuceneFieldCodec<F> codec = field.type().codec();
			if ( codec instanceof LuceneVectorFieldCodec ) {
				vectorCodec = (LuceneVectorFieldCodec<F>) codec;
				vectorElementsType = vectorCodec.vectorElementsType();
				indexedVectorsDimension = vectorCodec.getConfiguredDimensions();
				similarityFunction = vectorCodec.getVectorSimilarity();
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
		@SuppressWarnings("unchecked")
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

			// we just checked the array type above, so we do the cast:
			this.vector = vectorCodec.encode( (F) vector );
		}

		@Override
		public void filter(SearchPredicate filter) {
			this.filter = LuceneSearchPredicate.from( scope, filter );
		}

		@Override
		public void requiredMinimumSimilarity(float similarity) {
			this.similarity = similarity;
		}
	}

	public static class FloatFactory extends AbstractLuceneValueFieldSearchQueryElementFactory<KnnPredicateBuilder, float[]> {
		@Override
		public KnnPredicateBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<float[]> field) {
			return new LuceneFloatKnnPredicate.FloatBuilder( scope, field );
		}
	}

	public static class ByteFactory
			extends
			AbstractLuceneValueFieldSearchQueryElementFactory<KnnPredicateBuilder, byte[]> {
		@Override
		public KnnPredicateBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<byte[]> field) {
			return new LuceneByteKnnPredicate.ByteBuilder( scope, field );
		}
	}

	private static class LuceneByteKnnPredicate extends LuceneKnnPredicate<byte[]> {

		private LuceneByteKnnPredicate(ByteBuilder builder) {
			super( builder );
		}

		@Override
		protected Query doToQuery(PredicateRequestContext context) {
			KnnByteVectorQuery query = new KnnByteVectorQuery( absoluteFieldPath, vector, k, prepareFilter( context ) );
			return similarity == null
					? query
					: VectorSimilarityFilterQuery.create( query, similarity, vector.length, similarityFunction );
		}

		private static class ByteBuilder extends Builder<byte[]> {
			protected ByteBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<byte[]> field) {
				super( scope, field );
			}

			@Override
			public SearchPredicate build() {
				return new LuceneByteKnnPredicate( this );
			}
		}
	}

	private static class LuceneFloatKnnPredicate extends LuceneKnnPredicate<float[]> {

		private LuceneFloatKnnPredicate(FloatBuilder builder) {
			super( builder );
		}

		@Override
		protected Query doToQuery(PredicateRequestContext context) {
			KnnFloatVectorQuery query = new KnnFloatVectorQuery( absoluteFieldPath, vector, k, prepareFilter( context ) );
			return similarity == null ? query : VectorSimilarityFilterQuery.create( query, similarity, similarityFunction );
		}

		private static class FloatBuilder extends Builder<float[]> {
			protected FloatBuilder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<float[]> field) {
				super( scope, field );
			}

			@Override
			public SearchPredicate build() {
				return new LuceneFloatKnnPredicate( this );
			}
		}
	}
}

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

public class LuceneKnnPredicate extends AbstractLuceneSingleFieldPredicate implements LuceneSearchPredicate {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final int k;
	private final Object vector;
	private final LuceneSearchPredicate filter;
	private final Float similarity;
	private final VectorSimilarityFunction similarityFunction;

	private LuceneKnnPredicate(Builder<?> builder) {
		super( builder );
		this.k = builder.k;
		this.vector = builder.vector;
		this.filter = builder.filter;
		this.similarity = builder.similarity;
		this.similarityFunction = builder.similarityFunction;
	}

	@Override
	protected Query doToQuery(PredicateRequestContext context) {

		if ( vector instanceof byte[] ) {
			byte[] byteVector = (byte[]) vector;
			KnnByteVectorQuery query = new KnnByteVectorQuery( absoluteFieldPath, byteVector, k, prepareFilter( context ) );
			return similarity == null
					? query
					: VectorSimilarityFilterQuery.create( query, similarity, byteVector.length, similarityFunction );
		}
		if ( vector instanceof float[] ) {
			KnnFloatVectorQuery query =
					new KnnFloatVectorQuery( absoluteFieldPath, (float[]) vector, k, prepareFilter( context ) );
			return similarity == null ? query : VectorSimilarityFilterQuery.create( query, similarity, similarityFunction );
		}

		throw new UnsupportedOperationException(
				"Unknown vector type " + vector.getClass() + ". only byte[] and float[] vectors are supported." );
	}

	private Query prepareFilter(PredicateRequestContext context) {
		return context.appendTenantAndRoutingFilters( filter == null ? null : filter.toQuery( context ) );
	}

	public static class DefaultFactory<F>
			extends AbstractLuceneValueFieldSearchQueryElementFactory<KnnPredicateBuilder, F> {
		@Override
		public KnnPredicateBuilder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
			return new Builder<>( scope, field );
		}
	}

	private static class Builder<F> extends AbstractBuilder implements KnnPredicateBuilder {
		private final Class<?> vectorElementsType;
		private final int indexedVectorsDimension;
		private final VectorSimilarityFunction similarityFunction;
		private final LuceneVectorFieldCodec<F> vectorCodec;
		private int k;
		private Object vector;
		private LuceneSearchPredicate filter;
		private Float similarity;

		private Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<F> field) {
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

		@Override
		public SearchPredicate build() {
			return new LuceneKnnPredicate( this );
		}
	}
}

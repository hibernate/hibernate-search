/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.Arrays;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

import org.apache.lucene.index.LeafReaderContext;

/**
 * A projection that composes the result of multiple inner projections into a single value.
 * <p>
 * Not to be confused with {@link LuceneObjectProjection}.
 *
 * @param <E> The type of the temporary storage for component values.
 * @param <V> The type of a single composed value.
 * @param <A> The type of the temporary storage for accumulated values, before and after being composed.
 * @param <P> The type of the final projection result representing an accumulation of composed values of type {@code V}.
 */
class LuceneCompositeProjection<E, V, A, P>
		extends AbstractLuceneProjection<P> {

	private final LuceneSearchProjection<?>[] inners;
	private final ResultsCompositor<E, V> compositor;
	private final ProjectionCollector<E, V, A, P> collector;

	public LuceneCompositeProjection(Builder builder, LuceneSearchProjection<?>[] inners,
			ResultsCompositor<E, V> compositor, ProjectionCollector<E, V, A, P> collector) {
		super( builder.scope );
		this.inners = inners;
		this.compositor = compositor;
		this.collector = collector;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + Arrays.toString( inners )
				+ ", compositor=" + compositor
				+ ", collector=" + collector
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		Extractor<?, ?>[] innerExtractors = new Extractor[inners.length];
		for ( int i = 0; i < inners.length; i++ ) {
			innerExtractors[i] = inners[i].request( context );
		}
		return new CompositeExtractor( innerExtractors );
	}

	private class CompositeExtractor implements Extractor<A, P> {
		private final Extractor<?, ?>[] inners;

		private CompositeExtractor(Extractor<?, ?>[] inners) {
			this.inners = inners;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "inners=" + Arrays.toString( inners )
					+ ", compositor=" + compositor
					+ ", collector=" + collector
					+ "]";
		}

		@Override
		public Values<A> values(ProjectionExtractContext context) {
			Values<?>[] innerValues = new Values<?>[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				innerValues[i] = inners[i].values( context );
			}
			return new CompositeValues( innerValues );
		}

		private class CompositeValues implements Values<A> {
			private final Values<?>[] inners;

			private CompositeValues(Values<?>[] inners) {
				this.inners = inners;
			}

			@Override
			public void context(LeafReaderContext context) throws IOException {
				for ( Values<?> inner : inners ) {
					inner.context( context );
				}
			}

			@Override
			public A get(int doc) throws IOException {
				A accumulated = collector.createInitial();

				E components = compositor.createInitial();
				for ( int i = 0; i < inners.length; i++ ) {
					Object extractedDataForInner = inners[i].get( doc );
					components = compositor.set( components, i, extractedDataForInner );
				}
				accumulated = collector.accumulate( accumulated, components );

				return accumulated;
			}
		}

		@Override
		public final P transform(LoadingResult<?> loadingResult, A accumulated,
				ProjectionTransformContext context) {
			for ( int i = 0; i < collector.size( accumulated ); i++ ) {
				E transformedData = collector.get( accumulated, i );
				// Transform in-place
				for ( int j = 0; j < inners.length; j++ ) {
					Object extractedDataForInner = compositor.get( transformedData, j );
					Object transformedDataForInner = Extractor.transformUnsafe( inners[j], loadingResult,
							extractedDataForInner, context );
					transformedData = compositor.set( transformedData, j, transformedDataForInner );
				}
				accumulated = collector.transform( accumulated, i, compositor.finish( transformedData ) );
			}
			return collector.finish( accumulated );
		}
	}

	static class Builder implements CompositeProjectionBuilder {

		private final LuceneSearchIndexScope<?> scope;

		Builder(LuceneSearchIndexScope<?> scope) {
			this.scope = scope;
		}

		@Override
		public <E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ResultsCompositor<E, V> compositor,
				ProjectionCollector.Provider<V, P> collectorProvider) {
			LuceneSearchProjection<?>[] typedInners =
					new LuceneSearchProjection<?>[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				typedInners[i] = LuceneSearchProjection.from( scope, inners[i] );
			}
			return new LuceneCompositeProjection<>( this, typedInners,
					compositor, collectorProvider.get() );
		}
	}
}

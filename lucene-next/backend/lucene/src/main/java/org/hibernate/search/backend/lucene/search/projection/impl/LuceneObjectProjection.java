/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.Arrays;

import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.reporting.impl.LuceneSearchHints;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.predicate.impl.LuceneSearchPredicate;
import org.hibernate.search.backend.lucene.search.predicate.impl.PredicateRequestContext;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.predicate.spi.PredicateTypeKeys;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionTypeKeys;
import org.hibernate.search.engine.search.spi.ResultsCompositor;
import org.hibernate.search.util.common.SearchException;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.join.QueryBitSetProducer;
import org.apache.lucene.util.BitSet;

/**
 * A projection that yields one composite value per object in a given object field.
 * <p>
 * Not to be confused with {@link LuceneCompositeProjection}.
 *
 * @param <E> The type of the temporary storage for component values.
 * @param <V> The type of a single composed value.
 * @param <P> The type of the final projection result representing an accumulation of composed values of type {@code V}.
 */
public class LuceneObjectProjection<E, V, P>
		extends AbstractLuceneProjection<P> {

	private final String absoluteFieldPath;
	private final boolean nested;
	private final Query filter;
	private final String nestedDocumentPath;
	private final String requiredContextAbsoluteFieldPath;
	private final LuceneSearchProjection<?>[] inners;
	private final ResultsCompositor<E, V> compositor;
	private final ProjectionCollector.Provider<V, P> collectorProvider;

	public LuceneObjectProjection(Builder builder, LuceneSearchProjection<?>[] inners,
			ResultsCompositor<E, V> compositor, ProjectionCollector.Provider<V, P> collectorProvider) {
		super( builder.scope );
		this.absoluteFieldPath = builder.objectField.absolutePath();
		this.nested = builder.objectField.type().nested();
		this.filter = builder.filter;
		this.nestedDocumentPath = builder.objectField.nestedDocumentPath();
		this.requiredContextAbsoluteFieldPath = collectorProvider.isSingleValued()
				? builder.objectField.closestMultiValuedParentAbsolutePath()
				: null;
		this.inners = inners;
		this.compositor = compositor;
		this.collectorProvider = collectorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + Arrays.toString( inners )
				+ ", compositor=" + compositor
				+ ", collectorProvider=" + collectorProvider
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		ProjectionRequestContext innerContext = context.forField( absoluteFieldPath, nested );
		if ( !context.projectionCardinalityCorrectlyAddressed( requiredContextAbsoluteFieldPath ) ) {
			throw QueryLog.INSTANCE.invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(
					absoluteFieldPath, requiredContextAbsoluteFieldPath );
		}
		Extractor<?, ?>[] innerExtractors = new Extractor[inners.length];
		for ( int i = 0; i < inners.length; i++ ) {
			innerExtractors[i] = inners[i].request( innerContext );
		}
		return new ObjectFieldExtractor<>( context.absoluteCurrentNestedFieldPath(), innerExtractors,
				collectorProvider.get() );
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being composed.
	 */
	private class ObjectFieldExtractor<A> implements Extractor<A, P> {
		private final String contextAbsoluteFieldPath;
		private final Extractor<?, ?>[] inners;
		private final ProjectionCollector<E, V, A, P> collector;

		private ObjectFieldExtractor(String contextAbsoluteFieldPath,
				Extractor<?, ?>[] inners, ProjectionCollector<E, V, A, P> collector) {
			this.contextAbsoluteFieldPath = contextAbsoluteFieldPath;
			this.inners = inners;
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
		public Values<A> values(ProjectionExtractContext context) {
			Values<?>[] innerValues = new Values<?>[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				innerValues[i] = inners[i].values( context );
			}
			return new ObjectFieldValues( context.collectorExecutionContext(), innerValues );
		}

		private class ObjectFieldValues extends AbstractNestingAwareAccumulatingValues<E, A> {
			private final Values<?>[] inners;
			private final QueryBitSetProducer filterBitSetProducer;

			private BitSet filterMatchedBitSet;

			private ObjectFieldValues(TopDocsDataCollectorExecutionContext context, Values<?>[] inners) {
				super( contextAbsoluteFieldPath, nestedDocumentPath, ObjectFieldExtractor.this.collector, context );
				this.inners = inners;
				this.filterBitSetProducer = filter == null ? null : new QueryBitSetProducer( filter );

			}

			@Override
			public void context(LeafReaderContext context) throws IOException {
				super.context( context );
				if ( filterBitSetProducer != null ) {
					filterMatchedBitSet = filterBitSetProducer.getBitSet( context );
				}
				for ( Values<?> inner : inners ) {
					inner.context( context );
				}
			}

			@Override
			protected A accumulate(A accumulated, int docId) throws IOException {
				if ( filterBitSetProducer != null && ( filterMatchedBitSet == null || !filterMatchedBitSet.get( docId ) ) ) {
					// The object didn't match the given filter: act as if it didn't exist.
					// Note that filters are used to detect flattened objects that were null upon indexing.
					return accumulated;
				}
				E components = compositor.createInitial();
				for ( int i = 0; i < inners.length; i++ ) {
					Object extractedDataForInner = inners[i].get( docId );
					components = compositor.set( components, i, extractedDataForInner );
				}
				return collector.accumulate( accumulated, components );
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

	public static class Factory
			extends AbstractLuceneCompositeNodeSearchQueryElementFactory<Builder> {
		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext node) {
			Query filter = null;
			if ( !node.type().nested() ) {
				if ( node.multiValued() ) {
					throw node.cannotUseQueryElement( ProjectionTypeKeys.OBJECT,
							LuceneSearchHints.INSTANCE.missingSupportHintForObjectProjectionOnMultiValuedFlattenedObjectNode(),
							null );
				}
				try {
					filter = LuceneSearchPredicate.from( scope, node.queryElement( PredicateTypeKeys.EXISTS, scope ).build() )
							// We are creating an exists predicate that does not need any session info,
							//  hence it should be safe here to use the context without session:
							.toQuery( PredicateRequestContext.withoutSession() );
				}
				catch (SearchException e) {
					throw node.cannotUseQueryElement( ProjectionTypeKeys.OBJECT, e.getMessage(), e );
				}
			}
			if ( node.multiValued() && !node.type().nested() ) {
				throw node.cannotUseQueryElement( ProjectionTypeKeys.OBJECT,
						LuceneSearchHints.INSTANCE.missingSupportHintForObjectProjectionOnMultiValuedFlattenedObjectNode(),
						null );
			}
			return new Builder( scope, node, filter );
		}
	}

	static class Builder implements CompositeProjectionBuilder {

		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexCompositeNodeContext objectField;
		private final Query filter;

		Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext objectField, Query filter) {
			this.scope = scope;
			this.objectField = objectField;
			this.filter = filter;
		}

		@Override
		public <E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ResultsCompositor<E, V> compositor,
				ProjectionCollector.Provider<V, P> collectorProvider) {
			if ( collectorProvider.isSingleValued() && objectField.multiValued() ) {
				throw QueryLog.INSTANCE.invalidSingleValuedProjectionOnMultiValuedField( objectField.absolutePath(),
						objectField.eventContext() );
			}
			LuceneSearchProjection<?>[] typedInners =
					new LuceneSearchProjection<?>[inners.length];
			for ( int i = 0; i < inners.length; i++ ) {
				typedInners[i] = LuceneSearchProjection.from( scope, inners[i] );
			}
			return new LuceneObjectProjection<>( this, typedInners,
					compositor, collectorProvider );
		}
	}
}

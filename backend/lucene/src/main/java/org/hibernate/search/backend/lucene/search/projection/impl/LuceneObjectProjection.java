/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.util.Arrays;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCompositeNodeSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexCompositeNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.CompositeProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;

import org.apache.lucene.index.LeafReaderContext;

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
	private final String nestedDocumentPath;
	private final String requiredContextAbsoluteFieldPath;
	private final LuceneSearchProjection<?>[] inners;
	private final ProjectionCompositor<E, V> compositor;
	private final ProjectionAccumulator.Provider<V, P> accumulatorProvider;

	public LuceneObjectProjection(Builder builder, LuceneSearchProjection<?>[] inners,
			ProjectionCompositor<E, V> compositor, ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
		super( builder.scope );
		this.absoluteFieldPath = builder.objectField.absolutePath();
		this.nestedDocumentPath = builder.objectField.nestedDocumentPath();
		this.requiredContextAbsoluteFieldPath = accumulatorProvider.isSingleValued()
				? builder.objectField.closestMultiValuedParentAbsolutePath() : null;
		this.inners = inners;
		this.compositor = compositor;
		this.accumulatorProvider = accumulatorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "inners=" + Arrays.toString( inners )
				+ ", compositor=" + compositor
				+ ", accumulatorProvider=" + accumulatorProvider
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		ProjectionRequestContext innerContext = context.forField( absoluteFieldPath );
		if ( requiredContextAbsoluteFieldPath != null
				&& !requiredContextAbsoluteFieldPath.equals( context.absoluteCurrentFieldPath() ) ) {
			throw log.invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(
					absoluteFieldPath, requiredContextAbsoluteFieldPath );
		}
		Extractor<?, ?>[] innerExtractors = new Extractor[inners.length];
		for ( int i = 0; i < inners.length; i++ ) {
			innerExtractors[i] = inners[i].request( innerContext );
		}
		return new ObjectFieldExtractor<>( context.absoluteCurrentFieldPath(), innerExtractors,
				accumulatorProvider.get() );
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being composed.
	 */
	private class ObjectFieldExtractor<A> implements Extractor<A, P> {
		private final String contextAbsoluteFieldPath;
		private final Extractor<?, ?>[] inners;
		private final ProjectionAccumulator<E, V, A, P> accumulator;

		private ObjectFieldExtractor(String contextAbsoluteFieldPath,
				Extractor<?, ?>[] inners, ProjectionAccumulator<E, V, A, P> accumulator) {
			this.contextAbsoluteFieldPath = contextAbsoluteFieldPath;
			this.inners = inners;
			this.accumulator = accumulator;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "inners=" + Arrays.toString( inners )
					+ ", compositor=" + compositor
					+ ", accumulator=" + accumulator
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

			private ObjectFieldValues(TopDocsDataCollectorExecutionContext context, Values<?>[] inners) {
				super( contextAbsoluteFieldPath, nestedDocumentPath, ObjectFieldExtractor.this.accumulator, context );
				this.inners = inners;
			}

			@Override
			public void context(LeafReaderContext context) throws IOException {
				super.context( context );
				for ( Values<?> inner : inners ) {
					inner.context( context );
				}
			}

			@Override
			protected A accumulate(A accumulated, int docId) throws IOException {
				E components = compositor.createInitial();
				for ( int i = 0; i < inners.length; i++ ) {
					Object extractedDataForInner = inners[i].get( docId );
					components = compositor.set( components, i, extractedDataForInner );
				}
				return accumulator.accumulate( accumulated, components );
			}
		}

		@Override
		public final P transform(LoadingResult<?, ?> loadingResult, A accumulated,
				ProjectionTransformContext context) {
			for ( int i = 0; i < accumulator.size( accumulated ); i++ ) {
				E transformedData = accumulator.get( accumulated, i );
				// Transform in-place
				for ( int j = 0; j < inners.length; j++ ) {
					Object extractedDataForInner = compositor.get( transformedData, j );
					Object transformedDataForInner = Extractor.transformUnsafe( inners[j], loadingResult,
							extractedDataForInner, context );
					transformedData = compositor.set( transformedData, j, transformedDataForInner );
				}
				accumulated = accumulator.transform( accumulated, i, compositor.finish( transformedData ) );
			}
			return accumulator.finish( accumulated );
		}
	}

	public static class Factory
			extends AbstractLuceneCompositeNodeSearchQueryElementFactory<Builder> {
		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext node) {
			return new Builder( scope, node );
		}
	}

	static class Builder implements CompositeProjectionBuilder {

		private final LuceneSearchIndexScope<?> scope;
		private final LuceneSearchIndexCompositeNodeContext objectField;

		Builder(LuceneSearchIndexScope<?> scope, LuceneSearchIndexCompositeNodeContext objectField) {
			this.scope = scope;
			this.objectField = objectField;
		}

		@Override
		public <E, V, P> SearchProjection<P> build(SearchProjection<?>[] inners, ProjectionCompositor<E, V> compositor,
				ProjectionAccumulator.Provider<V, P> accumulatorProvider) {
			if ( accumulatorProvider.isSingleValued() && objectField.multiValued() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( objectField.absolutePath(),
						objectField.eventContext() );
			}
			LuceneSearchProjection<?>[] typedInners =
					new LuceneSearchProjection<?>[ inners.length ];
			for ( int i = 0; i < inners.length; i++ ) {
				typedInners[i] = LuceneSearchProjection.from( scope, inners[i] );
			}
			return new LuceneObjectProjection<>( this, typedInners,
					compositor, accumulatorProvider );
		}
	}
}

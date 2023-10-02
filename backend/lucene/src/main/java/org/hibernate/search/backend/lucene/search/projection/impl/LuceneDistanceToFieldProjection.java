/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.Values;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.GeoPointDistanceDocValues;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.DocValues;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.SloppyMath;

/**
 * A projection on the distance from a given center to the GeoPoint defined in an index field.
 *
 * @param <P> The type of the final projection result representing accumulated distance values.
 */
public class LuceneDistanceToFieldProjection<P> extends AbstractLuceneProjection<P> {

	private static final ProjectionConverter<Double, Double> NO_OP_DOUBLE_CONVERTER =
			ProjectionConverter.passThrough( Double.class );

	private final String absoluteFieldPath;
	private final String nestedDocumentPath;
	private final String requiredContextAbsoluteFieldPath;

	private final LuceneFieldCodec<GeoPoint> codec;

	private final GeoPoint center;
	private final DistanceUnit unit;

	private final ProjectionAccumulator.Provider<Double, P> accumulatorProvider;

	private final LuceneFieldProjection<Double, Double, P> fieldProjection;

	private LuceneDistanceToFieldProjection(Builder builder,
			ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.nestedDocumentPath = builder.field.nestedDocumentPath();
		this.requiredContextAbsoluteFieldPath = accumulatorProvider.isSingleValued()
				? builder.field.closestMultiValuedParentAbsolutePath()
				: null;
		this.codec = builder.codec;
		this.center = builder.center;
		this.unit = builder.unit;
		this.accumulatorProvider = accumulatorProvider;
		if ( builder.field.multiValued() ) {
			// For multi-valued fields, use a field projection, because we need order to be preserved.
			this.fieldProjection = new LuceneFieldProjection<>(
					builder.scope, builder.field,
					this::computeDistanceWithUnit, NO_OP_DOUBLE_CONVERTER, accumulatorProvider
			);
		}
		else {
			// For single-valued fields, we can use the docvalues.
			this.fieldProjection = null;
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "absoluteFieldPath=" + absoluteFieldPath
				+ ", center=" + center
				+ ", accumulatorProvider=" + accumulatorProvider
				+ "]";
	}

	@Override
	public Extractor<?, P> request(ProjectionRequestContext context) {
		if ( fieldProjection != null ) {
			return fieldProjection.request( context );
		}
		else {
			context.checkValidField( absoluteFieldPath );
			if ( requiredContextAbsoluteFieldPath != null
					&& !requiredContextAbsoluteFieldPath.equals( context.absoluteCurrentNestedFieldPath() ) ) {
				throw log.invalidSingleValuedProjectionOnValueFieldInMultiValuedObjectField(
						absoluteFieldPath, requiredContextAbsoluteFieldPath );
			}
			return new DocValuesBasedDistanceExtractor<>( accumulatorProvider.get(),
					context.absoluteCurrentNestedFieldPath() );
		}
	}

	/**
	 * @param <A> The type of the temporary storage for accumulated values, before and after being transformed.
	 */
	private class DocValuesBasedDistanceExtractor<A> implements Extractor<A, P> {
		private final ProjectionAccumulator<Double, Double, A, P> accumulator;
		private final String contextAbsoluteFieldPath;

		private DocValuesBasedDistanceExtractor(ProjectionAccumulator<Double, Double, A, P> accumulator,
				String contextAbsoluteFieldPath) {
			this.accumulator = accumulator;
			this.contextAbsoluteFieldPath = contextAbsoluteFieldPath;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "["
					+ "absoluteFieldPath=" + absoluteFieldPath
					+ ", center=" + center
					+ ", accumulator=" + accumulator
					+ "]";
		}

		@Override
		public Values<A> values(ProjectionExtractContext context) {
			// Note that if this method gets called, we're dealing with a single-valued projection,
			// so we don't care that the order of doc values is not the same
			// as the order of values in the original document.
			return new DocValuesBasedDistanceValues( context.collectorExecutionContext() );
		}

		private class DocValuesBasedDistanceValues
				extends AbstractNestingAwareAccumulatingValues<Double, A> {
			private GeoPointDistanceDocValues currentLeafValues;

			public DocValuesBasedDistanceValues(TopDocsDataCollectorExecutionContext context) {
				super( contextAbsoluteFieldPath, nestedDocumentPath,
						DocValuesBasedDistanceExtractor.this.accumulator, context );
			}

			@Override
			protected DocIdSetIterator doContext(LeafReaderContext context) throws IOException {
				currentLeafValues = new GeoPointDistanceDocValues(
						DocValues.getSortedNumeric( context.reader(), absoluteFieldPath ), center );
				return currentLeafValues;
			}

			@Override
			protected A accumulate(A accumulated, int docId) throws IOException {
				if ( currentLeafValues.advanceExact( docId ) ) {
					for ( int i = 0; i < currentLeafValues.docValueCount(); i++ ) {
						Double distanceOrNull = currentLeafValues.nextValue();
						accumulated = accumulator.accumulate( accumulated, unit.fromMeters( distanceOrNull ) );
					}
				}
				return accumulated;
			}
		}

		@Override
		public P transform(LoadingResult<?> loadingResult, A extractedData,
				ProjectionTransformContext context) {
			// Nothing to transform: we take the values as they are.
			return accumulator.finish( extractedData );
		}
	}

	private Double computeDistanceWithUnit(IndexableField field) {
		GeoPoint decoded = codec.decode( field );
		if ( decoded == null ) {
			return null;
		}
		double distanceInMeters = SloppyMath.haversinMeters(
				center.latitude(), center.longitude(),
				decoded.latitude(), decoded.longitude()
		);
		return unit.fromMeters( distanceInMeters );
	}

	public static class Factory
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<DistanceToFieldProjectionBuilder,
					GeoPoint,
					LuceneFieldCodec<GeoPoint>> {
		public Factory(LuceneFieldCodec<GeoPoint> codec) {
			super( codec );
		}

		@Override
		public Builder create(LuceneSearchIndexScope<?> scope, LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			// Fail early if the nested structure differs in the case of multi-index search.
			field.nestedPathHierarchy();
			return new Builder( codec, scope, field );
		}
	}

	public static class Builder extends AbstractLuceneProjection.AbstractBuilder<Double>
			implements DistanceToFieldProjectionBuilder {

		private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

		private final LuceneFieldCodec<GeoPoint> codec;

		private final LuceneSearchIndexValueFieldContext<GeoPoint> field;

		private GeoPoint center;
		private DistanceUnit unit = DistanceUnit.METERS;

		private Builder(LuceneFieldCodec<GeoPoint> codec, LuceneSearchIndexScope<?> scope,
				LuceneSearchIndexValueFieldContext<GeoPoint> field) {
			super( scope );
			this.codec = codec;
			this.field = field;
		}

		@Override
		public void center(GeoPoint center) {
			this.center = center;
		}

		@Override
		public void unit(DistanceUnit unit) {
			this.unit = unit;
		}

		@Override
		public <P> SearchProjection<P> build(ProjectionAccumulator.Provider<Double, P> accumulatorProvider) {
			if ( accumulatorProvider.isSingleValued() && field.multiValued() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
			}
			return new LuceneDistanceToFieldProjection<>( this, accumulatorProvider );
		}
	}
}

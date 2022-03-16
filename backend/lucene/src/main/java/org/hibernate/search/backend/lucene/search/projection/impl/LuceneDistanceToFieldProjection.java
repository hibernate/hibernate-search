/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.GeoPointDistanceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.common.impl.AbstractLuceneCodecAwareSearchQueryElementFactory;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexValueFieldContext;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.spi.DistanceToFieldProjectionBuilder;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.SloppyMath;

/**
 * A projection on the distance from a given center to the GeoPoint defined in an index field.
 *
 * @param <E> The type of aggregated values extracted from the backend response (before conversion).
 * @param <P> The type of aggregated values returned by the projection (after conversion).
 */
public class LuceneDistanceToFieldProjection<E, P> extends AbstractLuceneProjection<E, P>
		implements CollectorFactory<GeoPointDistanceCollector> {

	private static final ProjectionConverter<Double, Double> NO_OP_DOUBLE_CONVERTER =
			ProjectionConverter.passThrough( Double.class );

	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final LuceneFieldCodec<GeoPoint> codec;

	private final GeoPoint center;
	private final DistanceUnit unit;

	private final ProjectionAccumulator<Double, Double, E, P> accumulator;

	private final DistanceCollectorKey collectorKey;
	private final LuceneFieldProjection<E, P, Double, Double> fieldProjection;

	private LuceneDistanceToFieldProjection(Builder builder, boolean singleValued,
			ProjectionAccumulator<Double, Double, E, P> accumulator) {
		super( builder );
		this.absoluteFieldPath = builder.field.absolutePath();
		this.nestedDocumentPath = builder.field.nestedDocumentPath();
		this.codec = builder.codec;
		this.center = builder.center;
		this.unit = builder.unit;
		this.accumulator = accumulator;
		if ( singleValued ) {
			// For single-valued fields, we can use the docvalues.
			this.collectorKey = new DistanceCollectorKey( absoluteFieldPath, center );
			this.fieldProjection = null;
		}
		else {
			// For multi-valued fields, use a field projection, because we need order to be preserved.
			this.collectorKey = null;
			this.fieldProjection = new LuceneFieldProjection<>(
					builder.scope, builder.field,
					this::computeDistanceWithUnit, NO_OP_DOUBLE_CONVERTER, accumulator
			);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( ", center=" ).append( center )
				.append( ", accumulator=" ).append( accumulator )
				.append( "]" );
		return sb.toString();
	}

	@Override
	public void request(ProjectionRequestContext context) {
		if ( collectorKey != null ) {
			context.requireCollector( this );
		}
		else {
			fieldProjection.request( context );
		}
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			ProjectionExtractContext context) {
		if ( collectorKey != null ) {
			E accumulated = accumulator.createInitial();
			GeoPointDistanceCollector distanceCollector = context.getCollector( collectorKey );
			Double distanceOrNull = distanceCollector.getDistance( documentResult.getDocId() );
			if ( distanceOrNull != null ) {
				accumulated = accumulator.accumulate( accumulated, unit.fromMeters( distanceOrNull ) );
			}
			return accumulated;
		}
		else {
			return fieldProjection.extract( mapper, documentResult, context );
		}
	}

	@Override
	public P transform(LoadingResult<?, ?> loadingResult, E extractedData,
			ProjectionTransformContext context) {
		FromDocumentValueConvertContext convertContext = context.fromDocumentValueConvertContext();
		return accumulator.finish( extractedData, NO_OP_DOUBLE_CONVERTER, convertContext );
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

	@Override
	public GeoPointDistanceCollector createCollector(CollectorExecutionContext context) {
		return new GeoPointDistanceCollector(
				absoluteFieldPath,
				nestedDocumentPath == null ? null : context.createNestedDocsProvider( nestedDocumentPath ),
				center, context.getMaxDocs()
		);
	}

	@Override
	public CollectorKey<GeoPointDistanceCollector> getCollectorKey() {
		return collectorKey;
	}

	/**
	 * Necessary in order to share a single collector if there are multiple similar projections.
	 * See {@link #createCollector(CollectorExecutionContext)}, {@link #request(ProjectionRequestContext)}.
	 */
	private static final class DistanceCollectorKey implements CollectorKey<GeoPointDistanceCollector> {

		private final String absoluteFieldPath;
		private final GeoPoint center;

		private DistanceCollectorKey(String absoluteFieldPath, GeoPoint center) {
			this.absoluteFieldPath = absoluteFieldPath;
			this.center = center;
		}

		@Override
		public boolean equals(Object obj) {
			if ( obj == this ) {
				return true;
			}
			if ( obj == null || !obj.getClass().equals( getClass() ) ) {
				return false;
			}
			DistanceCollectorKey other = (DistanceCollectorKey) obj;
			return absoluteFieldPath.equals( other.absoluteFieldPath ) && center.equals( other.center );
		}

		@Override
		public int hashCode() {
			return Objects.hash( absoluteFieldPath, center );
		}
	}

	public static class Factory
			extends
			AbstractLuceneCodecAwareSearchQueryElementFactory<DistanceToFieldProjectionBuilder, GeoPoint, LuceneFieldCodec<GeoPoint>> {
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
			if ( accumulatorProvider.isSingleValued() && field.multiValuedInRoot() ) {
				throw log.invalidSingleValuedProjectionOnMultiValuedField( field.absolutePath(), field.eventContext() );
			}
			return new LuceneDistanceToFieldProjection<>( this, accumulatorProvider.isSingleValued(),
					accumulatorProvider.get() );
		}
	}
}

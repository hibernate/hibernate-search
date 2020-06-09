/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Objects;
import java.util.Set;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorKey;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.GeoPointDistanceCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorExecutionContext;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.CollectorFactory;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentFieldValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.spi.ProjectionConverter;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A projection on the distance from a given center to the GeoPoint defined in an index field.
 *
 * @param <E> The type of aggregated values extracted from the backend response (before conversion).
 * @param <P> The type of aggregated values returned by the projection (after conversion).
 */
class LuceneDistanceToFieldProjection<E, P>
		implements LuceneSearchProjection<E, P>, CollectorFactory<GeoPointDistanceCollector> {

	private static final ProjectionConverter<Double, Double> NO_OP_DOUBLE_CONVERTER = new ProjectionConverter<>(
			Double.class,
			(value, context) -> value
	);

	private final Set<String> indexNames;
	private final String absoluteFieldPath;
	private final String nestedDocumentPath;

	private final GeoPoint center;
	private final DistanceUnit unit;

	private final ProjectionAccumulator<Double, Double, E, P> accumulator;

	private final DistanceCollectorKey collectorKey;

	LuceneDistanceToFieldProjection(Set<String> indexNames, String absoluteFieldPath, String nestedDocumentPath,
			GeoPoint center, DistanceUnit unit,
			ProjectionAccumulator<Double, Double, E, P> accumulator) {
		this.indexNames = indexNames;
		this.absoluteFieldPath = absoluteFieldPath;
		this.nestedDocumentPath = nestedDocumentPath;
		this.center = center;
		this.unit = unit;
		this.accumulator = accumulator;
		this.collectorKey = new DistanceCollectorKey( absoluteFieldPath, center );
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
	public Set<String> getIndexNames() {
		return indexNames;
	}

	@Override
	public void request(SearchProjectionRequestContext context) {
		context.requireCollector( this );
	}

	@Override
	public E extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {
		GeoPointDistanceCollector distanceCollector = context.getCollector( collectorKey );
		E accumulated = accumulator.createInitial();
		Double distanceOrNull = distanceCollector.getDistance( documentResult.getDocId() );
		if ( distanceOrNull != null ) {
			accumulated = accumulator.accumulate( accumulated, unit.fromMeters( distanceOrNull ) );
		}
		return accumulated;
	}

	@Override
	public P transform(LoadingResult<?> loadingResult, E extractedData,
			SearchProjectionTransformContext context) {
		FromDocumentFieldValueConvertContext convertContext = context.getFromDocumentFieldValueConvertContext();
		return accumulator.finish( extractedData, NO_OP_DOUBLE_CONVERTER, convertContext );
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
	 * See {@link #createCollector(CollectorExecutionContext)}, {@link #request(SearchProjectionRequestContext)}.
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

}

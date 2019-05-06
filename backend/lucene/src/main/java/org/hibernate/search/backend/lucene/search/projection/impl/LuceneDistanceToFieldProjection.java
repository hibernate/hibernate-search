/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import org.hibernate.search.backend.lucene.search.extraction.impl.DistanceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class LuceneDistanceToFieldProjection implements LuceneSearchProjection<Double, Double> {

	private final String absoluteFieldPath;

	private final GeoPoint center;

	private final DistanceUnit unit;

	LuceneDistanceToFieldProjection(String absoluteFieldPath, GeoPoint center, DistanceUnit unit) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.center = center;
		this.unit = unit;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.addDistanceCollector( absoluteFieldPath, center );
	}

	@Override
	public void contributeFields(LuceneDocumentStoredFieldVisitorBuilder builder) {
		builder.add( absoluteFieldPath );
	}

	@Override
	public Double extract(ProjectionHitMapper<?, ?> mapper, LuceneResult documentResult,
			SearchProjectionExtractContext context) {

		DistanceCollector distanceCollector = context.getDistanceCollector( absoluteFieldPath, center );
		return unit.fromMeters( distanceCollector.getDistance( documentResult.getDocId() ) );
	}

	@Override
	public Double transform(LoadingResult<?> loadingResult, Double extractedData,
			SearchProjectionTransformContext context) {
		return extractedData;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( ", center=" ).append( center )
				.append( "]" );
		return sb.toString();
	}
}

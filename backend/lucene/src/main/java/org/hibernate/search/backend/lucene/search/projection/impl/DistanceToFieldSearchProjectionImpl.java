/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.search.extraction.impl.DistanceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceToFieldSearchProjectionImpl implements LuceneSearchProjection<Double> {

	private final String absoluteFieldPath;

	private final GeoPoint center;

	private final DistanceUnit unit;

	private DistanceCollector distanceCollector;

	DistanceToFieldSearchProjectionImpl(String absoluteFieldPath, GeoPoint center, DistanceUnit unit) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.center = center;
		this.unit = unit;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		this.distanceCollector = luceneCollectorBuilder.addDistanceCollector( absoluteFieldPath, center );
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.add( absoluteFieldPath );
	}

	@Override
	public void extract(ProjectionHitCollector collector, LuceneResult documentResult) {
		collector.collectProjection( unit.fromMeters( distanceCollector.getDistance( documentResult.getDocId() ) ) );
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

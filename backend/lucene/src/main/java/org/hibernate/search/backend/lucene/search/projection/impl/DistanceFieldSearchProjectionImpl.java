/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.search.extraction.impl.DistanceCollector;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.engine.spatial.GeoPoint;

class DistanceFieldSearchProjectionImpl implements LuceneSearchProjection<Double> {

	private final LuceneIndexSchemaFieldNode<GeoPoint> schemaNode;

	private final GeoPoint center;

	private final DistanceUnit unit;

	private DistanceCollector distanceCollector;

	DistanceFieldSearchProjectionImpl(LuceneIndexSchemaFieldNode<GeoPoint> schemaNode, GeoPoint center,
			DistanceUnit unit) {
		this.schemaNode = schemaNode;
		this.center = center;
		this.unit = unit;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		this.distanceCollector = luceneCollectorBuilder.addDistanceCollector( schemaNode.getAbsoluteFieldPath(), center );
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.add( schemaNode.getAbsoluteFieldPath() );
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document, int docId, Float score) {
		collector.collectProjection( unit.fromMeters( distanceCollector.getDistance( docId ) ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( schemaNode.getAbsoluteFieldPath() )
				.append( ", center=" ).append( center )
				.append( "]" );
		return sb.toString();
	}
}

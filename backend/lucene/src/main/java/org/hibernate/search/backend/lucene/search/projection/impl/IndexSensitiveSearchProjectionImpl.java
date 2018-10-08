/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class IndexSensitiveSearchProjectionImpl<T> implements LuceneSearchProjection<T> {

	private final Map<String, LuceneSearchProjection<T>> projectionsByIndex;

	IndexSensitiveSearchProjectionImpl(Map<String, LuceneSearchProjection<T>> projectionsByIndex) {
		this.projectionsByIndex = projectionsByIndex;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		for ( LuceneSearchProjection<T> projection : projectionsByIndex.values() ) {
			projection.contributeCollectors( luceneCollectorBuilder );
		}
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		absoluteFieldPaths.add( LuceneFields.indexFieldName() );
		for ( LuceneSearchProjection<T> projection : projectionsByIndex.values() ) {
			projection.contributeFields( absoluteFieldPaths );
		}
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document, int docId, Float score) {
		projectionsByIndex.get( document.get( LuceneFields.indexFieldName() ) )
				.extract( collector, document, docId, score );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( projectionsByIndex )
				.append( "]" );
		return sb.toString();
	}
}

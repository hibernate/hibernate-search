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
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorsBuilder;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

class FieldSearchProjectionImpl<T> implements LuceneSearchProjection<T> {

	private final LuceneIndexSchemaFieldNode<T> schemaNode;

	FieldSearchProjectionImpl(LuceneIndexSchemaFieldNode<T> schemaNode) {
		this.schemaNode = schemaNode;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		if ( schemaNode.getCodec().getOverriddenStoredFields().isEmpty() ) {
			absoluteFieldPaths.add( schemaNode.getAbsoluteFieldPath() );
		}
		else {
			absoluteFieldPaths.addAll( schemaNode.getCodec().getOverriddenStoredFields() );
		}
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document, int docId, Float score) {
		T rawValue = schemaNode.getCodec().decode( document, schemaNode.getAbsoluteFieldPath() );
		collector.collectProjection( schemaNode.getConverter().convertFromProjection( rawValue ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( schemaNode.getAbsoluteFieldPath() )
				.append( "]" );
		return sb.toString();
	}
}

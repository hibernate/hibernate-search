/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.HitExtractor;
import org.hibernate.search.backend.elasticsearch.search.extraction.impl.SourceHitExtractor;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class FieldSearchProjectionImpl<T> implements ElasticsearchSearchProjection<T> {

	private final String absoluteFieldPath;

	FieldSearchProjectionImpl(String absoluteFieldPath, Class<T> type) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public Optional<HitExtractor<? super ProjectionHitCollector>> getHitExtractor(SearchBackendContext searchBackendContext,
			ElasticsearchIndexModel indexModel) {
		ElasticsearchIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

		if ( schemaNode == null ) {
			return Optional.empty();
		}

		return Optional.of( new SourceHitExtractor<T>( absoluteFieldPath, schemaNode.getConverter() ) );
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "absoluteFieldPath=" ).append( absoluteFieldPath )
				.append( "]" );
		return sb.toString();
	}
}

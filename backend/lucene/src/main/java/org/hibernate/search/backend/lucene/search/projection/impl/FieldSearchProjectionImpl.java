/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.projection.impl;

import java.util.Optional;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.search.query.impl.FieldProjectionHitExtractor;
import org.hibernate.search.backend.lucene.search.query.impl.HitExtractor;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

public class FieldSearchProjectionImpl<T> implements LuceneSearchProjection<T> {

	private final String absoluteFieldPath;

	FieldSearchProjectionImpl(String absoluteFieldPath, Class<T> type) {
		this.absoluteFieldPath = absoluteFieldPath;
	}

	@Override
	public Optional<HitExtractor<? super ProjectionHitCollector>> getHitExtractor(LuceneIndexModel indexModel) {
		LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( absoluteFieldPath );

		if ( schemaNode == null ) {
			return Optional.empty();
		}

		return Optional.of( new FieldProjectionHitExtractor<>( absoluteFieldPath, schemaNode ) );
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

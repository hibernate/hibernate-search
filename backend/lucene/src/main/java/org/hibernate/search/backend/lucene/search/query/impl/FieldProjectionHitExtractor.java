/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.Set;

import org.apache.lucene.document.Document;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneFieldCodec;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;

class FieldProjectionHitExtractor implements HitExtractor<ProjectionHitCollector> {

	private final String absoluteFieldPath;

	private final LuceneFieldCodec<?> codec;

	FieldProjectionHitExtractor(String absoluteFieldPath, LuceneFieldCodec<?> codec) {
		this.absoluteFieldPath = absoluteFieldPath;
		this.codec = codec;
	}

	@Override
	public void contributeCollectors(LuceneCollectorsBuilder luceneCollectorBuilder) {
		luceneCollectorBuilder.requireTopDocsCollector();
	}

	@Override
	public void contributeFields(Set<String> absoluteFieldPaths) {
		if ( codec.getOverriddenStoredFields().isEmpty() ) {
			absoluteFieldPaths.add( absoluteFieldPath );
		}
		else {
			absoluteFieldPaths.addAll( codec.getOverriddenStoredFields() );
		}
	}

	@Override
	public void extract(ProjectionHitCollector collector, Document document) {
		collector.collectProjection( codec.decode( document, absoluteFieldPath ) );
	}
}

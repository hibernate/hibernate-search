/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.common.spi.FieldPaths.RelativizedPath;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public abstract class AbstractLuceneIndexSchemaFieldTemplate<N> {

	private final SimpleGlobPattern absolutePathGlob;
	private final boolean multiValued;

	AbstractLuceneIndexSchemaFieldTemplate(SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		this.absolutePathGlob = absolutePathGlob;
		this.multiValued = multiValued;
	}

	N createNodeIfMatching(LuceneIndexModel model, String absolutePath) {
		if ( !absolutePathGlob.matches( absolutePath ) ) {
			return null;
		}

		RelativizedPath relativizedPath = FieldPaths.relativize( absolutePath );
		LuceneIndexSchemaObjectNode parent =
				relativizedPath.parentPath.map( model::getObjectNode ).orElseGet( model::getRootNode );

		return createNode( parent, relativizedPath.relativePath, multiValued );
	}

	protected abstract N createNode(LuceneIndexSchemaObjectNode parent, String relativePath, boolean multiValued);
}

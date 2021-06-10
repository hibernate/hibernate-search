/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.common.spi.FieldPaths.RelativizedPath;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public abstract class AbstractLuceneIndexSchemaFieldTemplate<N extends LuceneIndexField> {

	private final IndexFieldInclusion inclusion;

	private final SimpleGlobPattern absolutePathGlob;
	private final boolean multiValued;

	AbstractLuceneIndexSchemaFieldTemplate(LuceneIndexCompositeNode declaringParent, IndexFieldInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued) {
		this.inclusion = declaringParent.inclusion().compose( inclusion );
		this.absolutePathGlob = absolutePathGlob;
		this.multiValued = multiValued;
	}

	N createNodeIfMatching(LuceneIndexModel model, String absolutePath) {
		if ( !absolutePathGlob.matches( absolutePath ) ) {
			return null;
		}

		RelativizedPath relativizedPath = FieldPaths.relativize( absolutePath );
		LuceneIndexCompositeNode parent =
				relativizedPath.parentPath
						.<LuceneIndexCompositeNode>map( path -> model.fieldOrNull( path, IndexFieldFilter.ALL ).toObjectField() )
						.orElseGet( model::root );

		return createNode( parent, relativizedPath.relativePath, inclusion, multiValued );
	}

	protected abstract N createNode(LuceneIndexCompositeNode parent, String relativePath,
			IndexFieldInclusion inclusion, boolean multiValued);
}

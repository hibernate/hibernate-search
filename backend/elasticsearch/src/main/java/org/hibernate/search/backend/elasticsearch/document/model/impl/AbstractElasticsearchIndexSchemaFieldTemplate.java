/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.document.model.impl;

import org.hibernate.search.engine.backend.common.spi.FieldPaths;
import org.hibernate.search.engine.backend.common.spi.FieldPaths.RelativizedPath;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldFilter;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public abstract class AbstractElasticsearchIndexSchemaFieldTemplate<N extends AbstractElasticsearchIndexField> {

	private final SimpleGlobPattern absolutePathGlob;
	private final IndexFieldInclusion inclusion;
	private final boolean multiValued;

	AbstractElasticsearchIndexSchemaFieldTemplate(ElasticsearchIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, IndexFieldInclusion inclusion,
			boolean multiValued) {
		this.absolutePathGlob = absolutePathGlob;
		this.inclusion = declaringParent.inclusion().compose( inclusion );
		this.multiValued = multiValued;
	}

	public IndexFieldInclusion getInclusion() {
		return inclusion;
	}

	N createNodeIfMatching(ElasticsearchIndexModel model, String absolutePath) {
		if ( !absolutePathGlob.matches( absolutePath ) ) {
			return null;
		}

		RelativizedPath relativizedPath = FieldPaths.relativize( absolutePath );
		ElasticsearchIndexCompositeNode parent =
				relativizedPath.parentPath
						.<ElasticsearchIndexCompositeNode>map( path ->
								model.fieldOrNull( path, IndexFieldFilter.ALL ).toObjectField() )
						.orElseGet( model::root );

		return createNode( parent, relativizedPath.relativePath, inclusion, multiValued );
	}

	protected abstract N createNode(ElasticsearchIndexCompositeNode parent, String relativePath,
			IndexFieldInclusion inclusion, boolean multiValued);
}

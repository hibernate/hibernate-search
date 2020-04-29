/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class LuceneIndexSchemaFieldTemplate
		extends AbstractLuceneIndexSchemaFieldTemplate<LuceneIndexSchemaFieldNode<?>> {

	private final LuceneIndexFieldType<?> type;

	public LuceneIndexSchemaFieldTemplate(LuceneIndexSchemaObjectNode declaringParent, IndexFieldInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued,
			LuceneIndexFieldType<?> type) {
		super( declaringParent, inclusion, absolutePathGlob, multiValued );
		this.type = type;
	}

	@Override
	protected LuceneIndexSchemaFieldNode<?> createNode(LuceneIndexSchemaObjectNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new LuceneIndexSchemaFieldNode<>( parent, relativePath, inclusion, multiValued, type );
	}
}

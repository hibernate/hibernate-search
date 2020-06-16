/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collections;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class LuceneIndexSchemaObjectFieldTemplate
		extends AbstractLuceneIndexSchemaFieldTemplate<LuceneIndexSchemaObjectFieldNode> {

	private final ObjectStructure structure;

	public LuceneIndexSchemaObjectFieldTemplate(LuceneIndexSchemaObjectNode declaringParent, IndexFieldInclusion inclusion,
			SimpleGlobPattern absolutePathGlob, boolean multiValued,
			ObjectStructure structure) {
		super( declaringParent, inclusion, absolutePathGlob, multiValued );
		this.structure = structure;
	}

	@Override
	protected LuceneIndexSchemaObjectFieldNode createNode(LuceneIndexSchemaObjectNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new LuceneIndexSchemaObjectFieldNode(
				parent, relativePath, inclusion, structure, multiValued,
				// TODO HSEARCH-3905 we don't know the children, so we should find another way to implement the exists predicate
				Collections.emptyList()
		);
	}
}

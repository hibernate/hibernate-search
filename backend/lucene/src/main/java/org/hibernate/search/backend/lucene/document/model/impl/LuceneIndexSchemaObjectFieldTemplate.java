/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collections;

import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class LuceneIndexSchemaObjectFieldTemplate
		extends AbstractLuceneIndexSchemaFieldTemplate<LuceneIndexSchemaObjectNode> {

	private final ObjectFieldStorage storage;

	public LuceneIndexSchemaObjectFieldTemplate(SimpleGlobPattern absolutePathGlob, boolean multiValued,
			ObjectFieldStorage storage) {
		super( absolutePathGlob, multiValued );
		this.storage = storage;
	}

	@Override
	protected LuceneIndexSchemaObjectNode createNode(LuceneIndexSchemaObjectNode parent,
			String relativePath, boolean multiValued) {
		return new LuceneIndexSchemaObjectNode(
				parent, relativePath, storage, multiValued,
				// TODO HSEARCH-3273 we don't know the children, so we should find another way to implement the exists predicate
				Collections.emptyList()
		);
	}
}

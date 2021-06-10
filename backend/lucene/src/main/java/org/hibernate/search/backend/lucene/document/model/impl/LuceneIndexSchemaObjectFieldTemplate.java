/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Collections;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class LuceneIndexSchemaObjectFieldTemplate
		extends AbstractLuceneIndexSchemaFieldTemplate<LuceneIndexObjectField> {

	private final LuceneIndexCompositeNodeType type;

	public LuceneIndexSchemaObjectFieldTemplate(LuceneIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, LuceneIndexCompositeNodeType type, IndexFieldInclusion inclusion,
			boolean multiValued) {
		super( declaringParent, inclusion, absolutePathGlob, multiValued );
		this.type = type;
	}

	@Override
	protected LuceneIndexObjectField createNode(LuceneIndexCompositeNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new LuceneIndexObjectField( parent, relativePath, type, inclusion, multiValued,
				Collections.emptyMap(), true );
	}

	public LuceneIndexCompositeNodeType type() {
		return type;
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.backend.document.model.spi.IndexFieldInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;


public class LuceneIndexValueFieldTemplate
		extends AbstractLuceneIndexFieldTemplate<LuceneIndexValueField<?>> {

	private final LuceneIndexValueFieldType<?> type;

	public LuceneIndexValueFieldTemplate(LuceneIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, LuceneIndexValueFieldType<?> type, IndexFieldInclusion inclusion,
			boolean multiValued) {
		super( declaringParent, inclusion, absolutePathGlob, multiValued );
		this.type = type;
	}

	@Override
	protected LuceneIndexValueField<?> createNode(LuceneIndexCompositeNode parent,
			String relativePath, IndexFieldInclusion inclusion, boolean multiValued) {
		return new LuceneIndexValueField<>( parent, relativePath, type, inclusion, multiValued, true );
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.backend.lucene.types.impl.LuceneIndexValueFieldType;
import org.hibernate.search.engine.common.tree.spi.TreeNodeInclusion;
import org.hibernate.search.util.common.pattern.spi.SimpleGlobPattern;

public class LuceneIndexValueFieldTemplate
		extends AbstractLuceneIndexFieldTemplate<LuceneIndexValueFieldType<?>> {

	public LuceneIndexValueFieldTemplate(LuceneIndexCompositeNode declaringParent,
			SimpleGlobPattern absolutePathGlob, LuceneIndexValueFieldType<?> type, TreeNodeInclusion inclusion,
			boolean multiValued) {
		super( declaringParent, absolutePathGlob, type, inclusion, multiValued );
	}

	@Override
	protected LuceneIndexField createNode(LuceneIndexCompositeNode parent, String relativePath,
			LuceneIndexValueFieldType<?> type, TreeNodeInclusion inclusion, boolean multiValued) {
		return new LuceneIndexValueField<>( parent, relativePath, type, inclusion, multiValued, true );
	}
}

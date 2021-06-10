/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexNodeContext;
import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.engine.backend.document.model.spi.IndexNode;

public interface LuceneIndexNode
		extends IndexNode<LuceneSearchIndexScope>, LuceneSearchIndexNodeContext {

	@Override
	LuceneIndexCompositeNode toComposite();

	@Override
	LuceneIndexObjectField toObjectField();

	@Override
	LuceneIndexValueField<?> toValueField();

	boolean dynamic();

}

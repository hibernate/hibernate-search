/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.document.model.impl;

import java.util.Map;

import org.hibernate.search.backend.lucene.search.common.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.types.impl.LuceneIndexCompositeNodeType;
import org.hibernate.search.engine.backend.document.model.spi.AbstractIndexRoot;
import org.hibernate.search.engine.search.common.spi.SearchIndexSchemaElementContextHelper;

public final class LuceneIndexRoot
		extends AbstractIndexRoot<
				LuceneIndexRoot,
				LuceneSearchIndexScope<?>,
				LuceneIndexCompositeNodeType,
				LuceneIndexField>
		implements LuceneIndexCompositeNode {

	public LuceneIndexRoot(LuceneIndexCompositeNodeType type,
			Map<String, LuceneIndexField> notYetInitializedStaticChildren) {
		super( type, notYetInitializedStaticChildren );
	}

	@Override
	protected LuceneIndexRoot self() {
		return this;
	}

	@Override
	public LuceneIndexObjectField toObjectField() {
		return SearchIndexSchemaElementContextHelper.throwingToObjectField( this );
	}

	@Override
	public LuceneIndexValueField<?> toValueField() {
		return SearchIndexSchemaElementContextHelper.throwingToValueField( this );
	}

	@Override
	public boolean dynamic() {
		return false;
	}
}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.common.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexCompositeNodeContext;

public final class LuceneMultiIndexSearchIndexCompositeNodeContext
		extends AbstractMultiIndexSearchIndexCompositeNodeContext<
				LuceneSearchIndexCompositeNodeContext,
				LuceneSearchIndexScope<?>,
				LuceneSearchIndexCompositeNodeTypeContext,
				LuceneSearchIndexNodeContext>
		implements LuceneSearchIndexCompositeNodeContext, LuceneSearchIndexCompositeNodeTypeContext {

	public LuceneMultiIndexSearchIndexCompositeNodeContext(LuceneSearchIndexScope<?> scope,
			String absolutePath, List<? extends LuceneSearchIndexCompositeNodeContext> nodeForEachIndex) {
		super( scope, absolutePath, nodeForEachIndex );
	}

	@Override
	protected LuceneSearchIndexCompositeNodeContext self() {
		return this;
	}

	@Override
	protected LuceneSearchIndexCompositeNodeTypeContext selfAsNodeType() {
		return this;
	}

	@Override
	protected LuceneSearchIndexCompositeNodeTypeContext typeOf(LuceneSearchIndexCompositeNodeContext indexElement) {
		return indexElement.type();
	}

	@Override
	protected LuceneSearchIndexNodeContext childInScope(String childRelativeName) {
		return scope.child( this, childRelativeName );
	}
}

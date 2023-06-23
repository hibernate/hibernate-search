/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.AbstractMultiIndexSearchIndexCompositeNodeContext;

public final class StubMultiIndexSearchIndexCompositeNodeContext
		extends AbstractMultiIndexSearchIndexCompositeNodeContext<
				StubSearchIndexCompositeNodeContext,
				StubSearchIndexScope,
				StubSearchIndexCompositeNodeTypeContext,
				StubSearchIndexNodeContext>
		implements StubSearchIndexCompositeNodeContext, StubSearchIndexCompositeNodeTypeContext {

	public StubMultiIndexSearchIndexCompositeNodeContext(StubSearchIndexScope scope,
			String absolutePath, List<? extends StubSearchIndexCompositeNodeContext> nodeForEachIndex) {
		super( scope, absolutePath, nodeForEachIndex );
	}

	@Override
	protected StubSearchIndexCompositeNodeContext self() {
		return this;
	}

	@Override
	protected StubSearchIndexCompositeNodeTypeContext selfAsNodeType() {
		return this;
	}

	@Override
	protected StubSearchIndexCompositeNodeTypeContext typeOf(
			StubSearchIndexCompositeNodeContext indexElement) {
		return indexElement.type();
	}

	@Override
	public StubSearchIndexValueFieldContext<?> toValueField() {
		return (StubSearchIndexValueFieldContext<?>) super.toValueField();
	}

	@Override
	protected StubSearchIndexNodeContext childInScope(String childRelativeName) {
		return scope.child( this, childRelativeName );
	}
}

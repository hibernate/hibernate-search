/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;

public final class StubMultiIndexSearchIndexCompositeNodeContext
		extends AbstractStubMultiIndexSearchIndexNodeContext<StubSearchIndexCompositeNodeContext>
		implements StubSearchIndexCompositeNodeContext {

	public StubMultiIndexSearchIndexCompositeNodeContext(StubSearchIndexScope scope,
			String absolutePath,
			List<StubSearchIndexCompositeNodeContext> elementForEachIndex) {
		super( scope, absolutePath, elementForEachIndex );
	}

	@Override
	public boolean isValueField() {
		return false;
	}

	@Override
	public StubSearchIndexValueFieldContext<?> toValueField() {
		throw new SearchException( "This is not a value field, but an object field" );
	}

	@Override
	public boolean nested() {
		return getFromElementIfCompatible(
				StubSearchIndexCompositeNodeContext::nested, Object::equals, "nested" );
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, StubSearchIndexScope scope) {
		return queryElementFactory( key ).create( scope, this );
	}

	@Override
	protected <T> AbstractStubSearchQueryElementFactory<T> queryElementFactory(
			StubSearchIndexCompositeNodeContext indexElement, SearchQueryElementTypeKey<T> key) {
		return indexElement.queryElementFactory( key );
	}

}

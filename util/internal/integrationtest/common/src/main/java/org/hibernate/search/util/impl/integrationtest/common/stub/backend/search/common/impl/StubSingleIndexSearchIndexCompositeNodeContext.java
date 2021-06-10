/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubSearchQueryElementFactories;

public class StubSingleIndexSearchIndexCompositeNodeContext
		implements StubSearchIndexCompositeNodeContext {

	private final String absolutePath;
	private final ObjectStructure objectStructure;

	public StubSingleIndexSearchIndexCompositeNodeContext(String absolutePath, ObjectStructure objectStructure) {
		this.absolutePath = absolutePath;
		this.objectStructure = objectStructure;
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
	public EventContext eventContext() {
		return absolutePath() == null ? EventContexts.indexSchemaRoot()
				: EventContexts.fromIndexFieldAbsolutePath( absolutePath() );
	}

	@Override
	public String absolutePath() {
		return absolutePath;
	}

	@Override
	public boolean nested() {
		return objectStructure == ObjectStructure.NESTED;
	}

	@Override
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, StubSearchIndexScope scope) {
		return queryElementFactory( key ).create( scope, this );
	}

	@Override
	public <T> AbstractStubSearchQueryElementFactory<T> queryElementFactory(SearchQueryElementTypeKey<T> key) {
		return StubSearchQueryElementFactories.get( key );
	}

}

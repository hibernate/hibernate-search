/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.types.impl.StubIndexValueFieldType;

public class StubSingleIndexSearchValueFieldContext<F>
		implements StubSearchValueFieldContext<F> {

	private final String absolutePath;
	private final StubIndexValueFieldType<F> type;

	public StubSingleIndexSearchValueFieldContext(String absolutePath, StubIndexValueFieldType<F> type) {
		this.absolutePath = absolutePath;
		this.type = type;
	}

	@Override
	public boolean isValueField() {
		return true;
	}

	@Override
	public StubSearchValueFieldContext<?> toValueField() {
		return this;
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
	public <T> T queryElement(SearchQueryElementTypeKey<T> key, StubSearchIndexScope scope) {
		return type.queryElementFactory( key ).create( scope, this );
	}

	@Override
	public StubSearchValueFieldTypeContext<F> type() {
		return type;
	}
}

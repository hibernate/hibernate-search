/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.common.impl;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

public interface StubSearchIndexSchemaElementContext extends EventContextProvider {

	boolean isValueField();

	StubSearchValueFieldContext<?> toValueField();

	String absolutePath();

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, StubSearchIndexScope scope);
}

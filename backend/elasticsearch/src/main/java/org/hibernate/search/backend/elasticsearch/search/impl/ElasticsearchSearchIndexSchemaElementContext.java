/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.List;

import org.hibernate.search.engine.search.common.spi.SearchQueryElementTypeKey;
import org.hibernate.search.util.common.reporting.spi.EventContextProvider;

/**
 * Information about an index element targeted by search,
 * be it the index root, a value field or an object field.
 * <p>
 * This is used in predicates, projections, sorts, ...
 */
public interface ElasticsearchSearchIndexSchemaElementContext extends EventContextProvider {

	boolean isObjectField();

	ElasticsearchSearchCompositeIndexSchemaElementContext toObjectField();

	String absolutePath();

	List<String> nestedPathHierarchy();

	// Query elements: predicates, sorts, projections, aggregations, ...

	<T> T queryElement(SearchQueryElementTypeKey<T> key, ElasticsearchSearchIndexScope scope);

}

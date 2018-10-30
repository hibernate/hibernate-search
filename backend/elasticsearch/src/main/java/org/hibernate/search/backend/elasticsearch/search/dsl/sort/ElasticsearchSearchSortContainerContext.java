/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.sort;

import org.hibernate.search.engine.search.dsl.sort.NonEmptySortContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;

/**
 * A DSL context allowing to specify the sort order, with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchSortContainerContext extends SearchSortContainerContext {

	NonEmptySortContext fromJsonString(String jsonString);

}

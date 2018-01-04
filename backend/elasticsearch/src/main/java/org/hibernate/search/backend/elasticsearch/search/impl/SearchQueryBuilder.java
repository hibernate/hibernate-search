/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.function.Function;

import org.hibernate.search.engine.search.SearchQuery;

/**
 * @author Yoann Rodiere
 */
public interface SearchQueryBuilder<T, C> {

	C getPredicateCollector();

	void addRoutingKey(String routingKey);

	// TODO add more arguments, such as faceting options

	<Q> Q build(Function<SearchQuery<T>, Q> searchQueryWrapperFactory);

}

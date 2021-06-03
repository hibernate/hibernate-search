/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

/**
 * A factory for query elements (predicates, sorts, projections, aggregations, ...) targeting index schema elements
 * (root, value fields, object fields).
 *
 * @param <T> The type returned by {@link #create(ElasticsearchSearchIndexScope, Object)}.
 * Can be the type of the query element, or an intermediary builder type.
 * @param <E> The type representing the targeted index schema element.
 */
public interface ElasticsearchSearchQueryElementFactory<T, E> {

	/**
	 * @param scope The search context, i.e. information regarding the targeted indexes.
	 * @param element The targeted index schema element.
	 * @return The query element, or an intermediary builder (depending on the factory type).
	 */
	T create(ElasticsearchSearchIndexScope scope, E element);

	/**
	 * Checks whether this factory and the given factory can be used interchangeably.
	 * @param other Another factory.
	 * @throws org.hibernate.search.util.common.SearchException if the two factories cannot be used interchangeably.
	 */
	void checkCompatibleWith(ElasticsearchSearchQueryElementFactory<?, ?> other);

}

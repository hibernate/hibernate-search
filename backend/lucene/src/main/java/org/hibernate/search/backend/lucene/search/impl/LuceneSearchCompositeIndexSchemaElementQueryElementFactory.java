/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

/**
 * A factory for query elements (predicates, sorts, projections, aggregations, ...)
 * targeting composite index elements (either the root or an object field).
 *
 * @param <T> The type returned by {@link #create(LuceneSearchIndexScope, LuceneSearchCompositeIndexSchemaElementContext)}.
 * Can be the type of the query element, or an intermediary builder type.
 */
public interface LuceneSearchCompositeIndexSchemaElementQueryElementFactory<T> {

	/**
	 * @param scope The search context, i.e. information regarding the targeted indexes.
	 * @param field The targeted field.
	 * @return The query element, or an intermediary builder (depending on the factory type).
	 */
	T create(LuceneSearchIndexScope scope, LuceneSearchCompositeIndexSchemaElementContext field);

	/**
	 * Checks whether this factory and the given factory can be used interchangeably.
	 * @param other Another factory.
	 * @throws org.hibernate.search.util.common.SearchException if the two factories cannot be used interchangeably.
	 */
	void checkCompatibleWith(LuceneSearchCompositeIndexSchemaElementQueryElementFactory<?> other);

}

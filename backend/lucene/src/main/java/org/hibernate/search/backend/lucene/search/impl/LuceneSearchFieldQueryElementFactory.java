/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.impl;

/**
 * A factory for query elements (predicates, sorts, projections, aggregations, ...) targeting fields of a given type.
 *
 * @param <T> The type returned by {@link #create(LuceneSearchContext, LuceneSearchFieldContext)}.
 * Can be the type of the query element, or an intermediary builder type.
 * @param <F> The type of values for the targeted field.
 */
public interface LuceneSearchFieldQueryElementFactory<T, F> {

	/**
	 * @param searchContext The search context, i.e. information regarding the targeted indexes.
	 * @param field The targeted field.
	 * @return The query element, or an intermediary builder (depending on the factory type).
	 */
	T create(LuceneSearchContext searchContext, LuceneSearchFieldContext<F> field);

	/**
	 * Determines whether this factory and the given factory can be used interchangeably.
	 * @param other Another factory.
	 * @return {@code true} if the two factories can be used interchangeably, {@code false} otherwise.
	 */
	boolean isCompatibleWith(LuceneSearchFieldQueryElementFactory<?, ?> other);

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.apache.lucene.search.SortField;
import org.hibernate.search.exception.SearchException;

/**
 * A context from which one may add another sort definition to the sort list.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortAdditionalSortFieldContext {

	/**
	 * Order elements by their relevance score.
	 *
	 * <p>The default order is <strong>descending</strong>, i.e. higher scores come first.
	 *
	 * @see SortContext#byScore()
	 */
	SortScoreContext andByScore();

	/**
	 * Order elements by their internal index order.
	 *
	 * @see SortContext#byIndexOrder()
	 */
	SortOrderTermination andByIndexOrder();

	/**
	 * Order elements by the value of a specific field.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @throws SearchException If the sort field type could not be automatically determined.
	 * @see SortContext#byField(String)
	 */
	SortFieldContext andByField(String fieldName);

	/**
	 * Order elements by distance.
	 *
	 * <p>The default order is <strong>ascending</strong>, i.e. shorter distances
	 * come first.
	 * <p>The distance is computed between the value of a field carrying coordinates
	 * (to be provided in the {@link SortDistanceNoFieldContext next context})
	 * and reference coordinates.
	 *
	 * @see SortContext#byDistance()
	 */
	SortDistanceNoFieldContext andByDistance();

	/**
	 * Order element using the native backend API for Lucene.
	 *
	 * <p>The sort order (ascending/descending) is defined in <code>sortField</code>
	 *
	 * @param sortField The sort field to be added to the sort list.
	 * @see SortContext#byNative(SortField)
	 */
	SortNativeContext andByNative(SortField sortField);

}

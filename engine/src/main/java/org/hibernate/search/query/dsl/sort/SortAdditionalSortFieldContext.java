/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.apache.lucene.search.SortField;
import org.hibernate.search.annotations.Spatial;
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
	 * <p>The sort field type will be determined automatically if possible, and an exception will be thrown
	 * if guessing is not possible. Guessing is impossible in particular if a custom field bridge is
	 * defined on the given field.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @throws SearchException If the sort field type could not be guessed.
	 * @see SortContext#byField(String)
	 * @see {@link #andByField(String, org.apache.lucene.search.SortField.Type)} for fields with
	 * custom field bridges.
	 */
	SortFieldContext andByField(String fieldName);

	/**
	 * Order elements by value of a specific field, with the sort field type provided.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 * <p><strong>Note:</strong> using this method is only required when sorting on a
	 * field on which a custom field bridge is defined. Otherwise, one may simply use
	 * {@link #byField(String)}.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @param sortFieldType The sort field type
	 * @see #andByField(String, org.apache.lucene.search.SortField.Type)
	 */
	SortFieldContext andByField(String fieldName, SortField.Type sortFieldType);

	/**
	 * Order elements by distance to a specific point.
	 * <p>The distance is computed between the value of the given field (which must be
	 * a {@link Spatial} field) and reference coordinates, to be provided in the
	 * {@link SortDistanceFromContext next context}.
	 *
	 * @param fieldName The name of the index field containing the coordinates
	 * @see SortContext#byDistance(String)
	 */
	SortDistanceFromContext andByDistance(String fieldName);

	/**
	 * Order element using the native backend API for Lucene.
	 *
	 * <p>The sort order (ascending/descending) is defined in <code>field</code>
	 *
	 * @param field The sort field to be added to the sort list.
	 * @see SortContext#byNative(SortField)
	 */
	SortNativeContext andByNative(SortField field);

	/**
	 * Order element using the native backend API for Elasticsearch.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @param nativeSortFieldDescription The sort field description, as valid JSON. This may be a
	 * simple quoted string (e.g. "'asc'") or a JSON map (e.g. "{'key': 'value'}"). See
	 * Elasticsearch's documentation for information about the exact syntax.
	 * @see SortContext#byNative(String, String)
	 */
	SortNativeContext andByNative(String fieldName, String nativeSortFieldDescription);
}

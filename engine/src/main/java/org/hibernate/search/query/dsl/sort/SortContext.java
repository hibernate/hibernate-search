/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query.dsl.sort;

import org.apache.lucene.search.SortField;
import org.hibernate.search.bridge.MetadataProvidingFieldBridge;
import org.hibernate.search.exception.SearchException;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SortContext {

	/**
	 * Order elements by their relevance score.
	 *
	 * <p>The default order is <strong>descending</strong>, i.e. higher scores come first.
	 */
	SortScoreContext byScore();

	/**
	 * Order elements by their internal index order.
	 */
	SortOrderTermination byIndexOrder();

	/**
	 * Order elements by the value of a specific field.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 * <p>The sort field type will be determined automatically if possible, and an exception will be thrown
	 * if it is not possible. Automatically determining the type is impossible in particular if
	 * a custom field bridge is defined on the given field and if this bridge doesn't implement
	 * {@link MetadataProvidingFieldBridge}.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @throws SearchException If the sort field type could not be automatically determined.
	 * @see #byField(String, org.apache.lucene.search.SortField.Type) The alternative for
	 * numeric fields with custom field bridges that don't implement {@link MetadataProvidingFieldBridge}.
	 */
	SortFieldContext byField(String fieldName);

	/**
	 * Order elements by value of a specific field, with the sort field type provided.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 * <p><strong>Note:</strong> using this method is only required when sorting on a
	 * field on which a custom field bridge that doesn't implement
	 * {@link MetadataProvidingFieldBridge} is defined.
	 * Otherwise, one may simply use {@link #byField(String)}.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @param sortFieldType The sort field type
	 * @deprecated Using this method shouldn't be needed if your custom field bridges
	 * implement {@link MetadataProvidingFieldBridge}. Use {@link #byField(String)} instead.
	 */
	@Deprecated
	SortFieldContext byField(String fieldName, SortField.Type sortFieldType);

	/**
	 * Order elements by distance.
	 *
	 * <p>The default order is <strong>ascending</strong>, i.e. shorter distances
	 * come first.
	 * <p>The distance is computed between the value of a field carrying coordinates
	 * (to be provided in the {@link SortDistanceNoFieldContext next context})
	 * and reference coordinates.
	 */
	SortDistanceNoFieldContext byDistance();

	/**
	 * Order element using the native backend API for Lucene.
	 *
	 * <p>The sort order (ascending/descending) is defined in <code>sortField</code>
	 *
	 * @param sortField The sort field to be added to the sort list.
	 */
	SortNativeContext byNative(SortField sortField);

	/**
	 * Order element using the native backend API for Elasticsearch.
	 *
	 * @param fieldName The name of the index field to sort by
	 * @param sortFieldNativeDescription The sort field description, as valid JSON. This may be a
	 * simple quoted string (e.g. "'asc'") or a JSON map (e.g. "{'key': 'value'}"). See
	 * Elasticsearch's documentation for information about the exact syntax.
	 */
	SortNativeContext byNative(String fieldName, String sortFieldNativeDescription);
}

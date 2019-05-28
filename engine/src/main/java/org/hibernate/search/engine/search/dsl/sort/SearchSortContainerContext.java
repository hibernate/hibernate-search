/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;

/**
 * A context allowing to add a sort element.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SearchSortContainerContext {

	/**
	 * Order elements by their relevance score.
	 * <p>
	 * The default order is <strong>descending</strong>, i.e. higher scores come first.
	 *
	 * @return A context allowing to define the sort more precisely, {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 */
	ScoreSortContext byScore();

	/**
	 * Order elements by their internal index order.
	 *
	 * @return A context allowing to {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 */
	NonEmptySortContext byIndexOrder();

	/**
	 * Order elements by the value of a specific field.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the index field to sort by
	 * @return A context allowing to define the sort more precisely, {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 * @throws SearchException If the sort field type could not be automatically determined.
	 */
	FieldSortContext byField(String absoluteFieldPath);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the indexed location field to sort by.
	 * @param location The location to which we want to compute the distance.
	 * @return A context allowing to define the sort more precisely, {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	DistanceSortContext byDistance(String absoluteFieldPath, GeoPoint location);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the indexed location field to sort by.
	 * @param latitude The latitude of the location to which we want to compute the distance.
	 * @param longitude The longitude of the location to which we want to compute the distance.
	 * @return A context allowing to define the sort more precisely, {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	default DistanceSortContext byDistance(String absoluteFieldPath, double latitude, double longitude) {
		return byDistance( absoluteFieldPath, GeoPoint.of( latitude, longitude ) );
	}

	/**
	 * Order by the given sort.
	 *
	 * @param sort A previously-built {@link SearchSort} object.
	 * @return A context allowing to {@link NonEmptySortContext#then() chain other sorts}
	 * or {@link SearchSortTerminalContext#toSort() get the resulting sort}.
	 */
	NonEmptySortContext by(SearchSort sort);

	/**
	 * Extend the current context with the given extension,
	 * resulting in an extended context offering different types of sorts.
	 *
	 * @param extension The extension to the sort DSL.
	 * @param <T> The type of context provided by the extension.
	 * @return The extended context.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchSortContainerContextExtension<T> extension);

	/**
	 * Create a context allowing to try to apply multiple extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchSortContainerContextExtension)} method instead.
	 *
	 * @return A context allowing to define the extensions to attempt, and the corresponding sorts.
	 */
	SearchSortContainerExtensionContext extension();

}

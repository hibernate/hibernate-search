/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchSort;
import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.engine.spatial.ImmutableGeoPoint;
import org.hibernate.search.util.SearchException;

/**
 * A context allowing to add a sort element.
 *
 * @param <N> The type of the next context (returned by terminal calls such as {@link FieldSortContext#end()}
 * or {@link ScoreSortContext#end()}).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface SearchSortContainerContext<N> {

	/**
	 * Order elements by their relevance score.
	 *
	 * <p>The default order is <strong>descending</strong>, i.e. higher scores come first.
	 */
	ScoreSortContext<N> byScore();

	/**
	 * Order elements by their internal index order.
	 */
	NonEmptySortContext<N> byIndexOrder();

	/**
	 * Order elements by the value of a specific field.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the index field to sort by
	 * @throws SearchException If the sort field type could not be automatically determined.
	 */
	FieldSortContext<N> byField(String absoluteFieldPath);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 *
	 * <p>The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the indexed location field to sort by.
	 * @param location The location to which we want to compute the distance.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	DistanceSortContext<N> byDistance(String absoluteFieldPath, GeoPoint location);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 *
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the indexed location field to sort by.
	 * @param latitude The latitude of the location to which we want to compute the distance.
	 * @param longitude The longitude of the location to which we want to compute the distance.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	default DistanceSortContext<N> byDistance(String absoluteFieldPath, double latitude, double longitude) {
		return byDistance( absoluteFieldPath, new ImmutableGeoPoint( latitude, longitude ) );
	}

	// TODO other sorts


	/**
	 * Order by the given sort.
	 */
	NonEmptySortContext<N> by(SearchSort sort);

	<T> T withExtension(SearchSortContainerContextExtension<N, T> extension);

	<T> NonEmptySortContext<N> withExtensionOptional(SearchSortContainerContextExtension<N, T> extension, Consumer<T> clauseContributor);

	<T> NonEmptySortContext<N> withExtensionOptional(SearchSortContainerContextExtension<N, T> extension, Consumer<T> clauseContributor,
			Consumer<SearchSortContainerContext<N>> fallbackClauseContributor);

}

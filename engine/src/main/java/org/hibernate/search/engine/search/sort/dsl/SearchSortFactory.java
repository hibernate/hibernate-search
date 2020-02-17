/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Consumer;

import org.hibernate.search.engine.spatial.GeoPoint;
import org.hibernate.search.util.common.SearchException;

/**
 * A factory for search sorts.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SearchSortFactory {

	/**
	 * Order elements by their relevance score.
	 * <p>
	 * The default order is <strong>descending</strong>, i.e. higher scores come first.
	 *
	 * @return A DSL step where the "score" sort can be defined in more details.
	 */
	ScoreSortOptionsStep<?> score();

	/**
	 * Order elements by their internal index order.
	 *
	 * @return A DSL step where the "index order" sort can be defined in more details.
	 */
	SortThenStep indexOrder();

	/**
	 * Order elements by the value of a specific field.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the index field to sort by
	 * @return A DSL step where the "field" sort can be defined in more details.
	 * @throws SearchException If the field doesn't exist or cannot be sorted on.
	 */
	FieldSortOptionsStep<?> field(String absoluteFieldPath);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the indexed location field to sort by.
	 * @param location The location to which we want to compute the distance.
	 * @return A DSL step where the "distance" sort can be defined in more details.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	DistanceSortOptionsStep<?> distance(String absoluteFieldPath, GeoPoint location);

	/**
	 * Order elements by the distance from the location stored in the specified field to the location specified.
	 * <p>
	 * The default order is <strong>ascending</strong>.
	 *
	 * @param absoluteFieldPath The absolute path of the indexed location field to sort by.
	 * @param latitude The latitude of the location to which we want to compute the distance.
	 * @param longitude The longitude of the location to which we want to compute the distance.
	 * @return A DSL step where the "distance" sort can be defined in more details.
	 * @throws SearchException If the field type does not constitute a valid location.
	 */
	default DistanceSortOptionsStep<?> distance(String absoluteFieldPath, double latitude, double longitude) {
		return distance( absoluteFieldPath, GeoPoint.of( latitude, longitude ) );
	}

	/**
	 * Order by a sort composed of several elements.
	 * <p>
	 * Note that, in general, calling this method is not necessary as you can chain sorts by calling
	 * {@link SortThenStep#then()}.
	 * This method is mainly useful to mix imperative and declarative style when building sorts.
	 * See {@link #composite(Consumer)}
	 *
	 * @return A DSL step where the "composite" sort can be defined in more details.
	 */
	CompositeSortComponentsStep<?> composite();

	/**
	 * Order by a sort composed of several elements,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 * <p>
	 * This is mainly useful to mix imperative and declarative style when building sorts, e.g.:
	 * <pre>{@code
	 * f.composite( c -> {
	 *    c.add( f.field( "category" ) );
	 *    if ( someInput != null ) {
	 *        c.add( f.distance( "location", someInput.getLatitude(), someInput.getLongitude() );
	 *    }
	 *    c.add( f.indexOrder() );
	 * } )
	 * }</pre>
	 *
	 * @param elementContributor A consumer that will add clauses to the step passed in parameter.
	 * Should generally be a lambda expression.
	 * @return A DSL step where the "composite" sort can be defined in more details.
	 */
	SortThenStep composite(Consumer<? super CompositeSortComponentsStep<?>> elementContributor);

	/**
	 * Extend the current factory with the given extension,
	 * resulting in an extended factory offering different types of sorts.
	 *
	 * @param extension The extension to the sort DSL.
	 * @param <T> The type of factory provided by the extension.
	 * @return The extended factory.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<T> T extension(SearchSortFactoryExtension<T> extension);

	/**
	 * Create a DSL step allowing multiple attempts to apply extensions one after the other,
	 * failing only if <em>none</em> of the extensions is supported.
	 * <p>
	 * If you only need to apply a single extension and fail if it is not supported,
	 * use the simpler {@link #extension(SearchSortFactoryExtension)} method instead.
	 *
	 * @return A DSL step.
	 */
	SearchSortFactoryExtensionIfSupportedStep extension();

}

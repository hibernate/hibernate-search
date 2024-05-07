/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.TypedFieldReference;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A base interface for subtypes of {@link SearchSortFactory} allowing to
 * easily override the self type and predicate factory type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <S> The self type, i.e. the exposed type of this factory.
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public interface ExtendedSearchSortFactory<
		E,
		S extends ExtendedSearchSortFactory<E, ?, PDF>,
		PDF extends SearchPredicateFactory<E>>
		extends SearchSortFactory<E> {

	@Override
	S withRoot(String objectFieldPath);

	@Override
	FieldSortOptionsStep<E, ?, PDF> field(String fieldPath);

	@Override
	default FieldSortOptionsStep<E, ?, PDF> field(TypedFieldReference<?> field) {
		return field( field.absolutePath() );
	}

	@Override
	DistanceSortOptionsStep<E, ?, PDF> distance(String fieldPath, GeoPoint location);

	@Override
	default DistanceSortOptionsStep<E, ?, PDF> distance(TypedFieldReference<? extends GeoPoint> field, GeoPoint location) {
		return distance( field.absolutePath(), location );
	}

	@Override
	default DistanceSortOptionsStep<E, ?, PDF> distance(String fieldPath, double latitude, double longitude) {
		return distance( fieldPath, GeoPoint.of( latitude, longitude ) );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
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
public interface ExtendedSearchSortFactory<S extends ExtendedSearchSortFactory<?, PDF>, PDF extends SearchPredicateFactory>
		extends SearchSortFactory {

	@Override
	S withRoot(String objectFieldPath);

	@Override
	FieldSortOptionsStep<?, PDF> field(String fieldPath);

	@Override
	DistanceSortOptionsStep<?, PDF> distance(String fieldPath, GeoPoint location);

	@Override
	default DistanceSortOptionsStep<?, PDF> distance(String fieldPath, double latitude, double longitude) {
		return distance( fieldPath, GeoPoint.of( latitude, longitude ) );
	}
}

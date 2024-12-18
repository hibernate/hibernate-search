/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.reference.sort.FieldSortFieldReference;
import org.hibernate.search.engine.spatial.GeoPoint;

/**
 * A base interface for subtypes of {@link SearchSortFactory} allowing to
 * easily override the self type and predicate factory type for all relevant methods.
 * <p>
 * <strong>Warning:</strong> Generic parameters of this type are subject to change,
 * so this type should not be referenced directly in user code.
 *
 * @param <SR> Scope root type.
 * @param <S> The self type, i.e. the exposed type of this factory.
 * @param <PDF> The type of factory used to create predicates in {@link FieldSortOptionsStep#filter(Function)}.
 */
public interface ExtendedSearchSortFactory<
		SR,
		S extends ExtendedSearchSortFactory<SR, ?, PDF>,
		PDF extends SearchPredicateFactory<SR>>
		extends SearchSortFactory<SR> {

	@Override
	S withRoot(String objectFieldPath);

	@Override
	FieldSortOptionsStep<SR, ?, PDF> field(String fieldPath);

	@Override
	DistanceSortOptionsStep<SR, ?, PDF> distance(String fieldPath, GeoPoint location);

	@Override
	default DistanceSortOptionsStep<SR, ?, PDF> distance(String fieldPath, double latitude, double longitude) {
		return distance( fieldPath, GeoPoint.of( latitude, longitude ) );
	}

	@Override
	default DistanceSortOptionsStep<SR, ?, PDF> distance(FieldSortFieldReference<? super SR, ?> fieldReference,
			GeoPoint location) {
		return distance( fieldReference.absolutePath(), location );
	}

	@Override
	default DistanceSortOptionsStep<SR, ?, PDF> distance(FieldSortFieldReference<? super SR, ?> fieldReference, double latitude,
			double longitude) {
		return distance( fieldReference, GeoPoint.of( latitude, longitude ) );
	}
}

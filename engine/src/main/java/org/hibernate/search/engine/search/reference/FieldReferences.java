/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.engine.search.reference.aggregation.AnyAggregationReference;
import org.hibernate.search.engine.search.reference.predicate.AnyPredicateReference;
import org.hibernate.search.engine.search.reference.projection.AnyProjectionReference;
import org.hibernate.search.engine.search.reference.sort.AnySortReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A set of util methods to create generic field references, mostly for testing purposes or trivial cases where just a few references are required.
 * <p>
 * While it is expected that the generated Hibernate Search static metamodel will contain
 * more precise field references that would match the actual field capabilities, these generic references can
 * be used by the users if they decide to opt-out of using the generated static metamodel
 * and would just want to create a few simple pre-defined references.
 */
@Incubating
public interface FieldReferences {

	static <SR, T> AnySortReference<SR, T> anySortReference(String absolutePath, Class<SR> scopeRootType, ValueModel valueModel,
			Class<T> sortType) {
		return new AnySortReference<>( absolutePath, scopeRootType, valueModel, sortType );
	}

	static <SR, T> AnyProjectionReference<SR, T> anyProjectionReference(String absolutePath, Class<SR> scopeRootType,
			ValueModel valueModel, Class<T> projectionType) {
		return new AnyProjectionReference<>( absolutePath, scopeRootType, valueModel, projectionType );
	}

	static <SR, T> AnyPredicateReference<SR, T> anyPredicateReference(String absolutePath, Class<SR> scopeRootType,
			ValueModel valueModel, Class<T> predicateType) {
		return new AnyPredicateReference<>( absolutePath, scopeRootType, valueModel, predicateType );
	}

	static <SR, T> AnyAggregationReference<SR, T> anyAggregationReference(String absolutePath, Class<SR> scopeRootType,
			ValueModel valueModel, Class<T> aggregationType) {
		return new AnyAggregationReference<>( absolutePath, scopeRootType, valueModel, aggregationType );
	}
}

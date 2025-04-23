/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.reference.predicate;

import org.hibernate.search.engine.search.common.ValueModel;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A generic predicate-field reference that can be manually created and passed to any predicate-DSL methods that require a field reference.
 * <p>
 * While it is expected that the generated Hibernate Search static metamodel will contain
 * more precise field references that would match the actual field capabilities, this generic reference can
 * be used by the users if they decide to opt-out of using the generated static metamodel
 * and would just want to create a few simple pre-defined references.
 *
 * @param absolutePath The absolut field path.
 * @param scopeRootType The class representing the scope root type.
 * @param valueModel The model of values used in the predicate. See {@link ValueModel}.
 * @param predicateType The class representing the type of the field used in a predicate, as per {@code valueModel}.
 * @param <SR> Scope root type
 * @param <T> The type of the field used in a predicate.
 */
@Incubating
public record AnyPredicateReference<SR, T>( String absolutePath, Class<SR> scopeRootType, ValueModel valueModel,
											Class<T> predicateType)
		implements ExistsPredicateFieldReference<SR>,
		KnnPredicateFieldReference<SR, T>,
		MatchPredicateFieldReference<SR, T>,
		NestedPredicateFieldReference<SR>,
		PhrasePredicateFieldReference<SR, T>,
		PrefixPredicateFieldReference<SR>,
		QueryStringPredicateFieldReference<SR, T>,
		RangePredicateFieldReference<SR, T>,
		RegexpPredicateFieldReference<SR>,
		SimpleQueryStringPredicateFieldReference<SR, T>,
		SpatialPredicateFieldReference<SR>,
		TermsPredicateFieldReference<SR>,
		WildcardPredicateFieldReference<SR> {
}

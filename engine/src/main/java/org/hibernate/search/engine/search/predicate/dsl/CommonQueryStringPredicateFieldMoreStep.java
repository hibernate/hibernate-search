/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.TypedPredicateFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a query string predicate definition, where the query string to match can be set
 * (see the superinterface {@link CommonQueryStringPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <SR> Scope root type.
 * @param <FR> Type of the field references.
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface CommonQueryStringPredicateFieldMoreStep<
		SR,
		S extends CommonQueryStringPredicateFieldMoreStep<SR, ?, N, FR>,
		N extends CommonQueryStringPredicateOptionsStep<?>,
		FR extends TypedPredicateFieldReference<SR, ?>>
		extends CommonQueryStringPredicateMatchingStep<N>, MultiFieldPredicateFieldBoostStep<S> {

	/**
	 * Target the given field in the query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link CommonQueryStringPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see CommonQueryStringPredicateFieldStep#field(String)
	 */
	default S field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link CommonQueryStringPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see CommonQueryStringPredicateFieldStep#fields(String...)
	 */
	S fields(String... fieldPaths);

	/**
	 * Target the given field in the query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link CommonQueryStringPredicateFieldStep#field(String)} for more information on targeted fields.
	 *
	 * @param field The field reference representing a <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see CommonQueryStringPredicateFieldStep#field(String)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default S field(FR field) {
		return fields( field );
	}

	/**
	 * Target the given fields in the query string predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link CommonQueryStringPredicateFieldStep#fields(String...)} for more information on targeted fields.
	 *
	 * @param fields The field reference representing <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see CommonQueryStringPredicateFieldStep#fields(String...)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default S fields(FR... fields) {
		String[] paths = new String[fields.length];
		for ( int i = 0; i < fields.length; i++ ) {
			paths[i] = fields[i].absolutePath();
		}
		return fields( paths );
	}

}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.TypedPredicateFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The initial step in a query string predicate definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <FR> Type of the field references.
 * @param <N> The type of the next step.
 */
public interface CommonQueryStringPredicateFieldStep<
		SR,
		N extends CommonQueryStringPredicateFieldMoreStep<SR, ?, ?, FR>,
		FR extends TypedPredicateFieldReference<SR, ?>> {

	/**
	 * Target the given field in the query string predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * Please refer to the reference documentation for more information.
	 *
	 * @param fieldPath The <a href="SearchPredicateFactory.html#field-paths">path</a> to the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	default N field(String fieldPath) {
		return fields( fieldPath );
	}

	/**
	 * Target the given fields in the query string predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link #field(String)},
	 * the only difference being that calls to {@link CommonQueryStringPredicateFieldMoreStep#boost(float)}
	 * and other field-specific settings on the returned step will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param fieldPaths The <a href="SearchPredicateFactory.html#field-paths">paths</a> to the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see #field(String)
	 */
	N fields(String... fieldPaths);


	/**
	 * Target the given field in the query string predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * Please refer to the reference documentation for more information.
	 *
	 * @param fieldReference The field reference representing a <a href="SearchPredicateFactory.html#field-references">definition</a> of the index field
	 * to apply the predicate on.
	 * @return The next step.
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default N field(FR fieldReference) {
		return fields( fieldReference );
	}

	/**
	 * Target the given fields in the query string predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #field(String)} followed by multiple calls to
	 * {@link #field(String)},
	 * the only difference being that calls to {@link CommonQueryStringPredicateFieldMoreStep#boost(float)}
	 * and other field-specific settings on the returned step will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param fieldReferences The field references representing <a href="SearchPredicateFactory.html#field-references">definitions</a> of the index fields
	 * to apply the predicate on.
	 * @return The next step.
	 *
	 * @see #field(String)
	 */
	@Incubating
	@SuppressWarnings("unchecked")
	default N fields(FR... fieldReferences) {
		String[] paths = new String[fieldReferences.length];
		for ( int i = 0; i < fieldReferences.length; i++ ) {
			paths[i] = fieldReferences[i].absolutePath();
		}
		return fields( paths );
	}
}

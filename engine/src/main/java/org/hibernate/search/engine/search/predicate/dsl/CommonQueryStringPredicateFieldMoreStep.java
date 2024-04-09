/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.TypedFieldReference;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a query string predicate definition, where the query string to match can be set
 * (see the superinterface {@link CommonQueryStringPredicateMatchingStep}),
 * or optional parameters for the last targeted field(s) can be set,
 * or more target fields can be added.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 * @param <N> The type of the next step.
 */
public interface CommonQueryStringPredicateFieldMoreStep<
		S extends CommonQueryStringPredicateFieldMoreStep<?, N>,
		N extends CommonQueryStringPredicateOptionsStep<?>>
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
	default S field(TypedFieldReference<?> field) {
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
	default S fields(TypedFieldReference<?>... fields) {
		String[] paths = new String[fields.length];
		for ( int i = 0; i < fields.length; i++ ) {
			paths[i] = fields[i].absolutePath();
		}
		return fields( paths );
	}

}

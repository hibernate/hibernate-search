/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import org.hibernate.search.engine.search.predicate.DslConverter;

/**
 * The context used when defining a match predicate, after at least one field was mentioned.
 */
public interface MatchPredicateFieldSetContext extends MultiFieldPredicateFieldSetContext<MatchPredicateFieldSetContext> {

	/**
	 * Target the given field in the match predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link MatchPredicateContext#onField(String)} for more information about targeting fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see MatchPredicateContext#onField(String)
	 */
	default MatchPredicateFieldSetContext orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the match predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * See {@link MatchPredicateContext#onFields(String...)} for more information about targeting fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see MatchPredicateContext#onFields(String...)
	 */
	MatchPredicateFieldSetContext orFields(String ... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to match the given value.
	 * <p>
	 * This method will apply DSL converters to {@code value} before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter#ENABLED}.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field.
	 * See {@link DslConverter#ENABLED} for more information.
	 * @return A context allowing to set further options or get the resulting predicate.
	 *
	 * @see #matching(Object, DslConverter)
	 */
	default MatchPredicateTerminalContext matching(Object value) {
		return matching( value, DslConverter.ENABLED );
	}

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@link Object},
	 * but a specific type is expected depending on the targeted field and on the {@code dslConverter} parameter.
	 * See {@link DslConverter} for more information.
	 * @param dslConverter Controls how the {@code value} should be converted before Hibernate Search attempts to interpret it as a field value.
	 * See {@link DslConverter} for more information.
	 * @return A context allowing to set further options or get the resulting predicate.
	 *
	 * @see DslConverter
	 */
	MatchPredicateTerminalContext matching(Object value, DslConverter dslConverter);
}

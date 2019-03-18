/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a phrase predicate, after at least one field was mentioned.
 */
public interface PhrasePredicateFieldSetContext extends MultiFieldPredicateFieldSetContext<PhrasePredicateFieldSetContext> {

	/**
	 * Target the given field in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateContext#onField(String)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return {@code this}, for method chaining.
	 *
	 * @see PhrasePredicateContext#onField(String)
	 */
	default PhrasePredicateFieldSetContext orField(String absoluteFieldPath) {
		return orFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the phrase predicate,
	 * as an alternative to the already-targeted fields.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * See {@link PhrasePredicateContext#onFields(String...)} for more information on targeted fields.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return {@code this}, for method chaining.
	 *
	 * @see PhrasePredicateContext#onFields(String...)
	 */
	PhrasePredicateFieldSetContext orFields(String... absoluteFieldPaths);

	/**
	 * Require at least one of the targeted fields to match the given phrase.
	 *
	 * @param phrase The phrase to match.
	 * @return A context allowing to set options or get the resulting predicate.
	 */
	PhrasePredicateTerminalContext matching(String phrase);

}

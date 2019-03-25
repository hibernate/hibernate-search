/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when starting to define a phrase predicate.
 */
public interface PhrasePredicateContext {

	/**
	 * Target the given field in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Multiple fields may be targeted by the same predicate:
	 * the predicate will match if <em>any</em> targeted field matches.
	 * <p>
	 * When targeting multiple fields, those fields must have compatible types.
	 * Please refer to the reference documentation for more information.
	 *
	 * @param absoluteFieldPath The absolute path (from the document root) of the targeted field.
	 * @return A {@link PhrasePredicateFieldSetContext} allowing to define field-specific settings
	 * (such as the {@link PhrasePredicateFieldSetContext#boostedTo(float) boost}),
	 * or simply to continue the definition of the phrase predicate
	 * ({@link PhrasePredicateFieldSetContext#matching(String) phrase to match}, ...).
	 */
	default PhrasePredicateFieldSetContext onField(String absoluteFieldPath) {
		return onFields( absoluteFieldPath );
	}

	/**
	 * Target the given fields in the phrase predicate.
	 * <p>
	 * Only text fields are supported.
	 * <p>
	 * Equivalent to {@link #onField(String)} followed by multiple calls to
	 * {@link PhrasePredicateFieldSetContext#orField(String)},
	 * the only difference being that calls to {@link PhrasePredicateFieldSetContext#boostedTo(float)}
	 * and other field-specific settings on the returned context will only need to be done once
	 * and will apply to all the fields passed to this method.
	 *
	 * @param absoluteFieldPaths The absolute paths (from the document root) of the targeted fields.
	 * @return A {@link PhrasePredicateFieldSetContext} (see {@link #onField(String)} for details).
	 *
	 * @see #onField(String)
	 */
	PhrasePredicateFieldSetContext onFields(String... absoluteFieldPaths);

}

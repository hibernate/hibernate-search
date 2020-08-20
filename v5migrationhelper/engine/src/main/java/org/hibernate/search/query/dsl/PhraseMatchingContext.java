/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
public interface PhraseMatchingContext extends FieldCustomization<PhraseMatchingContext> {
	/**
	 * field / property the term query is executed on
	 * @param field the name of the field
	 * @return {@code this} for method chaining
	 */
	PhraseMatchingContext andField(String field);

	/**
	 * Sentence to match. It will be processed by the analyzer
	 * @param sentence the sentence to match
	 * @return the {@link PhraseTermination} for method chaining
	 */
	PhraseTermination sentence(String sentence);
}

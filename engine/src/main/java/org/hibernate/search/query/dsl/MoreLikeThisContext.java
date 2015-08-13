/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * @hsearch.experimental More Like This queries are considered experimental
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface MoreLikeThisContext extends QueryCustomization<MoreLikeThisContext> {

	/**
	 * Exclude the entity used for comparison from the results
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisContext excludeEntityUsedForComparison();

	/**
	 * Boost significant terms relative to their scores.
	 * Amplified by the boost factor (1 is the recommended default to start with).
	 *
	 * Unless activated, terms are not boosted by their individual frequency.
	 * When activated, significant terms will have their influence increased more than by default.
	 * @param factor the factor value
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisContext favorSignificantTermsWithFactor(float factor);

	/**
	 * Match the content using "all" of the indexed fields of the entity.
	 *
	 * More precisely, only fields storing term vectors or physically stored will be compared.
	 * Fields storing the id and the class type are ignored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisTerminalMatchingContext comparingAllFields();

	/**
	 * Match the content using the selected fields of the entity.
	 *
	 * An exception is thrown if the fields are neither storing the term vectors nor physically stored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 * @param fieldNames the names of the fields to use for the comparison
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisOpenedMatchingContext comparingFields(String... fieldNames);

	/**
	 * Match the content using the selected field of the entity.
	 *
	 * An exception is thrown if the field are neither storing the term vectors nor physically stored.
	 *
	 * We highly recommend to store the term vectors if you plan on using More Like This queries.
	 * @param fieldName the name of the field to compare
	 * @return {@code this} for method chaining
	 */
	MoreLikeThisOpenedMatchingContext comparingField(String fieldName);

}

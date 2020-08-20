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
public interface TermMatchingContext extends FieldCustomization<TermMatchingContext> {
	/**
	 * Value searched in the field or fields.
	 * The value is passed to the field's:
	 *  - field bridge
	 *  - analyzer (unless ignoreAnalyzer is called).
	 *
	 * @param value the value to match
	 * @return {@code this} for method chaining
	 */
	TermTermination matching(Object value);

	/**
	 * field / property the term query is executed on
	 *
	 * @param field another field involved in the query
	 * @return {@code this} for method chaining
	 */
	TermMatchingContext andField(String field);
}

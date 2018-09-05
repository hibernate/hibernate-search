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
public interface WildcardContext extends QueryCustomization<WildcardContext> {
	/**
	 * @param field field/property the term query is executed on
	 * @return a {@link TermMatchingContext}
	 */
	TermMatchingContext onField(String field);

	/**
	 * @param fields fields/properties the term query is executed on
	 * @return a {@link TermMatchingContext}
	 */
	TermMatchingContext onFields(String... fields);

}

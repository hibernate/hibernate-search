/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * @author Guillaume Smet
 */
public interface SimpleQueryStringContext extends QueryCustomization<SimpleQueryStringContext> {
	/**
	 * @param field The field name the query is executed on
	 *
	 * @return {@code SimpleQueryStringMatchingContext} to continue the query
	 */
	SimpleQueryStringMatchingContext onField(String field);

	/**
	 * @param field The first field added to the list of fields (follows the same rules described below for fields)
	 * @param fields The field names the query is executed on
	 * @return {@code SimpleQueryStringMatchingContext} to continue the query
	 */
	SimpleQueryStringMatchingContext onFields(String field, String... fields);

}

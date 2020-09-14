/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.queryparser.simple.SimpleQueryParser;

/**
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface SimpleQueryStringDefinitionTermination {

	/**
	 * Simple query string passed to the {@link SimpleQueryParser}.
	 * @param simpleQueryString The query string
	 * @return {@code this} for method chaining
	 */
	SimpleQueryStringTermination matching(String simpleQueryString);

}

/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.queryparser.simple.SimpleQueryParser;

public interface SimpleQueryStringDefinitionTermination {

	/**
	 * Simple query string passed to the {@link SimpleQueryParser}.
	 */
	SimpleQueryStringTermination matching(String simpleQueryString);

}

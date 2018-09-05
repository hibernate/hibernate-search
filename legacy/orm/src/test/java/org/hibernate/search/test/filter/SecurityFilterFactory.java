/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;

/**
 * Apply a security filter to the results
 *
 * @author Emmanuel Bernard
 */
public class SecurityFilterFactory {
	private String login;

	/**
	 * injected parameter
	 */
	public void setLogin(String login) {
		this.login = login;
	}

	@Key
	public FilterKey getKey() {
		StandardFilterKey key = new StandardFilterKey();
		key.addParameter( login );
		return key;
	}

	@Factory
	public Query getFilter() {
		return new TermQuery( new Term( "teacher", login ) );
	}
}

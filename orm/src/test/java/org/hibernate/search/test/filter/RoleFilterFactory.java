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
 * Filter results by role
 *
 * @author Davide D'Alto
 */
public class RoleFilterFactory {
	private Employee.Role role;

	/**
	 * injected parameter
	 */
	public void setRole(Employee.Role login) {
		this.role = login;
	}

	@Key
	public FilterKey getKey() {
		StandardFilterKey key = new StandardFilterKey();
		key.addParameter( role );
		return key;
	}

	@Factory
	public Query getFilter() {
		return new TermQuery( new Term( "role", role.name() ) );
	}
}

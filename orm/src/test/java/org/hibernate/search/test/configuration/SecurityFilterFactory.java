/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.configuration;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.CachingWrapperFilter;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.hibernate.search.annotations.Factory;
import org.hibernate.search.annotations.Key;
import org.hibernate.search.filter.FilterKey;
import org.hibernate.search.filter.StandardFilterKey;

@SuppressWarnings("deprecation")
public class SecurityFilterFactory {

	private String ownerName;

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	@Factory
	public Filter buildSecurityFilter() {
		QueryWrapperFilter securityFilter = new QueryWrapperFilter( new TermQuery( new Term( "owner", ownerName ) ) );
		return new CachingWrapperFilter( securityFilter );
	}

	@Key
	public FilterKey getKey() {
		StandardFilterKey key = new StandardFilterKey();
		key.addParameter( ownerName );
		return key;
	}

}

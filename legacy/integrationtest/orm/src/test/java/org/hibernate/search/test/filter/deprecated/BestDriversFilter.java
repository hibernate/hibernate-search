/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.filter.deprecated;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;

/**
 * This filter is used verbatim in our documentation as an example.
 * Please keep the code simple and readable, formatted narrowly, and in sync with
 * the reference documentation.
 */
public class BestDriversFilter extends QueryWrapperFilter {

	public BestDriversFilter() {
		super( new TermQuery( new Term( "score", "5" ) ) );
	}

}

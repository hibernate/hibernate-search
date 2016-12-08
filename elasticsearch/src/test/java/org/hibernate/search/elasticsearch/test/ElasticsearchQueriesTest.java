/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.test;

import org.hibernate.search.elasticsearch.ElasticsearchQueries;
import org.hibernate.search.exception.SearchException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * @author Yoann Rodiere
 */
public class ElasticsearchQueriesTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/*
	 * Check that using the 'query' attribute with valid JSON works (does not throw an exception)
	 */
	@Test
	public void valid() {
		ElasticsearchQueries.fromJson(
				"{'query':{'match_all':{}}}"
		);
	}

	@Test
	public void invalidAttribute() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400051" );

		ElasticsearchQueries.fromJson(
				"{"
					+ "'aggs' : {"
						+ "'avg_grade' : { 'avg' : { 'field' : 'grade' } }"
					+ "}"
				+ "}"
		);
	}

	@Test
	public void malformatedJson() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400052" );

		ElasticsearchQueries.fromJson(
				"{ 'query': }"
		);
	}

	@Test
	public void nonObjectJson() {
		thrown.expect( SearchException.class );
		thrown.expectMessage( "HSEARCH400052" );

		ElasticsearchQueries.fromJson(
				"'foo'"
		);
	}

}

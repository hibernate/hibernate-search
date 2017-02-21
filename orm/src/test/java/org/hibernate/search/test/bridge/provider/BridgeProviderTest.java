/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge.provider;

import static org.fest.assertions.Assertions.assertThat;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.junit.Test;

/**
 * @author Emmanuel Bernard
 */
public class BridgeProviderTest extends SearchTestBase {
	@Test
	public void testCustomBridgeProvider() {
		Session s = openSession();
		s.getTransaction().begin();
		Movie laConfidential = new Movie( "LA Confidential" );
		Theater legendary = new Theater( "Legendary theater", laConfidential );
		s.persist( laConfidential );
		s.persist( legendary );
		s.getTransaction().commit();

		s.clear();

		s.getTransaction().begin();
		FullTextSession fts = Search.getFullTextSession( s );
		QueryBuilder qb = fts.getSearchFactory().buildQueryBuilder().forEntity( Theater.class ).get();
		Query query = qb.keyword().onField( "movie" ).matching( laConfidential ).createQuery();
		assertThat( fts.createFullTextQuery( query, Theater.class ).list() )
				.as( "The SearchFactory should build and find a bridge for Movie in Theater and  properly use it for indexing" )
				.hasSize( 1 );
		s.getTransaction().commit();

		s.clear();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Movie.class,
				Theater.class
		};
	}
}

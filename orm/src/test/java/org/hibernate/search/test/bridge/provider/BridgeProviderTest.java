/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.test.bridge.provider;

import org.apache.lucene.search.Query;

import org.hibernate.Session;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.spi.SearchConfiguration;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.spi.SearchFactoryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.test.util.HibernateManualConfiguration;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
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

		s.getTransaction().begin();
		for ( Object o : s.createQuery( "from java.lang.Object o" ).list() ) {
			s.delete( o );
		}
		s.getTransaction().commit();
	}

	@Test
	public void testMultipleMatchingFieldBridges() throws Exception {
		SearchConfiguration conf = new HibernateManualConfiguration()
				.addProperty( "hibernate.search.default.directory_provider", "ram" )
				.addProperty( "hibernate.search.lucene_version", TestConstants.getTargetLuceneVersion().name() )
				.addClass( Theater.class )
				.addClass( Chain.class );
		boolean throwException = false;
		try {
			SearchFactoryImplementor sf = new SearchFactoryBuilder().configuration( conf ).buildSearchFactory();
			sf.close();
		}
		catch (SearchException e) {
			assertThat( e.getMessage() ).contains( "TheaterBridgeProvider1" );
			throwException = true;
		}
		assertThat( throwException ).isTrue();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Movie.class,
				Theater.class
		};
	}
}

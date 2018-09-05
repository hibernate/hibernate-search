/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class MassIndexTest extends SearchTestBase {

	@Test
	public void testBatchSize() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		final int loop = 14;
		s.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				for ( int i = 0; i < loop; i++ ) {
					Statement statmt = connection.createStatement();
						statmt.executeUpdate( "insert into Domain(id, name) values( + "
								+ ( i + 1 ) + ", 'sponge" + i + "')" );
						statmt.executeUpdate( "insert into Email(id, title, body, header, domain_id) values( + "
								+ ( i + 1 ) + ", 'Bob Sponge', 'Meet the guys who create the software', 'nope', " + ( i + 1 ) + ")" );
						statmt.close();
					}
				}
		} );
		tx.commit();
		s.close();

		//check non created object does get found!!1
		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		ScrollableResults results = s.createCriteria( Email.class ).scroll( ScrollMode.FORWARD_ONLY );
		int index = 0;
		while ( results.next() ) {
			index++;
			s.index( results.get( 0 ) );
			if ( index % 5 == 0 ) {
				s.clear();
			}
		}
		tx.commit(); // if you get a LazyInitializationException, that's because we clear() the session in the loop.. it only works with a batch size of 5 (the point of the test)
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );
		List result = s.createFullTextQuery( parser.parse( "body:create" ) ).list();
		assertEquals( 14, result.size() );
		for ( Object object : result ) {
			s.delete( object );
		}
		tx.commit();
		s.close();
	}


	@Test
	public void testTransactional() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		final int loop = 4;
		for ( int i = 0; i < loop; i++ ) {
			Email email = new Email();
			email.setId( (long) i + 1 );
			email.setTitle( "JBoss World Berlin" );
			email.setBody( "Meet the guys who wrote the software" );
			s.persist( email );
		}
		tx.commit();
		s.close();

		//check non created object does get found!!1
		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );
		List result = s.createFullTextQuery( parser.parse( "body:create" ) ).list();
		assertEquals( 0, result.size() );
		tx.commit();
		s.close();

		s = Search.getFullTextSession( openSession() );
		s.getTransaction().begin();
		s.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				Statement stmt = connection.createStatement();
				stmt.executeUpdate( "update Email set body='Meet the guys who write the software'" );
				stmt.close();
			}
		} );
		s.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				//insert an object never indexed
				Statement stmt = connection.createStatement();
				stmt.executeUpdate( "insert into Email(id, title, body, header) values( + "
						+ ( loop + 1 ) + ", 'Bob Sponge', 'Meet the guys who create the software', 'nope')" );
				stmt.close();
			}
		} );

		s.getTransaction().commit();
		s.close();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		parser = new QueryParser( "id", TestConstants.stopAnalyzer );
		result = s.createFullTextQuery( parser.parse( "body:write" ) ).list();
		assertEquals( 0, result.size() );
		result = s.createCriteria( Email.class ).list();
		for ( int i = 0; i < loop / 2; i++ ) {
			s.index( result.get( i ) );
		}
		tx.commit(); //do the process
		s.index( result.get( loop / 2 ) ); //do the process out of tx
		tx = s.beginTransaction();
		for ( int i = loop / 2 + 1; i < loop; i++ ) {
			s.index( result.get( i ) );
		}
		tx.commit(); //do the process
		s.close();

		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		//object never indexed
		Email email = (Email) s.get( Email.class, Long.valueOf( loop + 1 ) );
		s.index( email );
		tx.commit();
		s.close();

		//check non indexed object get indexed by s.index
		s = Search.getFullTextSession( openSession() );
		tx = s.beginTransaction();
		result = s.createFullTextQuery( parser.parse( "body:create" ) ).list();
		assertEquals( 1, result.size() );
		tx.commit();
		s.close();
	}

	@Test
	public void testLazyLoading() throws Exception {
		Categorie cat = new Categorie( "Livre" );
		Entite ent = new Entite( "Le temple des songes", cat );
		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.persist( cat );
		s.persist( ent );
		tx.commit();
		s.close();

		s = getSessionWithAutoCommit();
		FullTextSession session = Search.getFullTextSession( s );
		Query luceneQuery = new TermQuery( new Term( "categorie.nom", "livre" ) );
		List result = session.createFullTextQuery( luceneQuery, Entite.class ).list();
		assertEquals( 1, result.size() );
		s.close();

		s = getSessionWithAutoCommit();
		ent = (Entite) s.get( Entite.class, ent.getId() );
		session = Search.getFullTextSession( s );
		session.index( ent );
		s.close();

		s = getSessionWithAutoCommit();
		session = Search.getFullTextSession( s );
		luceneQuery = new TermQuery( new Term( "categorie.nom", "livre" ) );
		result = session.createFullTextQuery( luceneQuery, Entite.class ).list();
		assertEquals( "test lazy loading and indexing", 1, result.size() );
		s.close();

		s = getSessionWithAutoCommit();
		Iterator it = s.createQuery( "from Entite where id = :id" ).setParameter( "id", ent.getId() ).iterate();
		session = Search.getFullTextSession( s );
		while ( it.hasNext() ) {
			ent = (Entite) it.next();
			session.index( ent );
		}
		s.close();

		s = getSessionWithAutoCommit();
		session = Search.getFullTextSession( s );
		luceneQuery = new TermQuery( new Term( "categorie.nom", "livre" ) );
		result = session.createFullTextQuery( luceneQuery, Entite.class ).list();
		assertEquals( "test lazy loading and indexing", 1, result.size() );
		s.close();
	}

	private Session getSessionWithAutoCommit() {
		Session s;
		s = openSession();
		s.doWork( new Work() {
			@Override
			public void execute(Connection connection) throws SQLException {
				connection.setAutoCommit( true );
			}
		} );
		return s;
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.QUEUEINGPROCESSOR_BATCHSIZE, "5" );
		cfg.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Email.class,
				Entite.class,
				Categorie.class,
				Domain.class
		};
	}
}

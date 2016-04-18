/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.query.boost;

import java.util.List;

import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author John Griffin
 */
public class FieldBoostTest extends SearchTestBase {

	private static final Log log = LoggerFactory.make();

	@Test
	public void testBoostedGetDesc() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		buildBoostedGetIndex( fullTextSession );

		fullTextSession.clear();
		Transaction tx = fullTextSession.beginTransaction();

		QueryParser authorParser = new QueryParser( "author", TestConstants.standardAnalyzer );
		QueryParser descParser = new QueryParser( "description", TestConstants.standardAnalyzer );
		Query author = authorParser.parse( "Wells" );
		Query desc = descParser.parse( "martians" );

		BooleanQuery query = new BooleanQuery.Builder()
				.add( author, BooleanClause.Occur.SHOULD )
				.add( desc, BooleanClause.Occur.SHOULD )
				.build();
		log.debug( query.toString() );

		org.hibernate.search.FullTextQuery hibQuery =
				fullTextSession.createFullTextQuery( query, BoostedGetDescriptionLibrary.class );
		List<?> results = hibQuery.list();

		log.debug( hibQuery.explain( 0 ).toString() );
		log.debug( hibQuery.explain( 1 ).toString() );

		assertTrue(
				"incorrect document returned",
				( (BoostedGetDescriptionLibrary) results.get( 0 ) ).getDescription().startsWith( "Martians" )
		);

		//cleanup
		for ( Object element : fullTextSession.createQuery( "from " + BoostedGetDescriptionLibrary.class.getName() )
				.list() ) {
			fullTextSession.delete( element );
		}
		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testBoostedFieldDesc() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		buildBoostedFieldIndex( fullTextSession );

		fullTextSession.clear();
		Transaction tx = fullTextSession.beginTransaction();

		QueryParser authorParser = new QueryParser( "author", TestConstants.standardAnalyzer );
		QueryParser descParser = new QueryParser( "description", TestConstants.standardAnalyzer );
		Query author = authorParser.parse( "Wells" );
		Query desc = descParser.parse( "martians" );

		BooleanQuery query = new BooleanQuery.Builder()
				.add( author, BooleanClause.Occur.SHOULD )
				.add( desc, BooleanClause.Occur.SHOULD )
				.build();
		log.debug( query.toString() );

		org.hibernate.search.FullTextQuery hibQuery =
				fullTextSession.createFullTextQuery( query, BoostedFieldDescriptionLibrary.class );
		List<?> results = hibQuery.list();

		assertTrue(
				"incorrect document boost",
				( (BoostedFieldDescriptionLibrary) results.get( 0 ) ).getDescription().startsWith( "Martians" )
		);

		log.debug( hibQuery.explain( 0 ).toString() );
		log.debug( hibQuery.explain( 1 ).toString() );

		//cleanup
		for ( Object element : fullTextSession.createQuery( "from " + BoostedFieldDescriptionLibrary.class.getName() )
				.list() ) {
			fullTextSession.delete( element );
		}
		tx.commit();
		fullTextSession.close();
	}

	@Test
	public void testBoostedDesc() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		buildBoostedDescIndex( fullTextSession );

		fullTextSession.clear();
		Transaction tx = fullTextSession.beginTransaction();

		QueryParser authorParser = new QueryParser( "author", TestConstants.standardAnalyzer );
		QueryParser descParser = new QueryParser( "description", TestConstants.standardAnalyzer );
		Query author = authorParser.parse( "Wells" );
		Query desc = descParser.parse( "martians" );

		BooleanQuery query = new BooleanQuery.Builder()
				.add( author, BooleanClause.Occur.SHOULD )
				.add( desc, BooleanClause.Occur.SHOULD )
				.build();
		log.debug( query.toString() );

		org.hibernate.search.FullTextQuery hibQuery =
				fullTextSession.createFullTextQuery( query, BoostedDescriptionLibrary.class );
		List<?> results = hibQuery.list();

		log.debug( hibQuery.explain( 0 ).toString() );
		log.debug( hibQuery.explain( 1 ).toString() );

		assertTrue(
				"incorrect document returned",
				( (BoostedDescriptionLibrary) results.get( 0 ) ).getDescription().startsWith( "Martians" )
		);

		//cleanup
		for ( Object element : fullTextSession.createQuery( "from " + BoostedDescriptionLibrary.class.getName() )
				.list() ) {
			fullTextSession.delete( element );
		}
		tx.commit();
		fullTextSession.close();
	}

	private void buildBoostedDescIndex(FullTextSession session) {
		Transaction tx = session.beginTransaction();
		BoostedDescriptionLibrary l = new BoostedDescriptionLibrary();
		l.setAuthor( "H.G. Wells" );
		l.setTitle( "The Invisible Man" );
		l.setDescription( "Scientist discovers invisibility and becomes insane." );
		session.save( l );

		l = new BoostedDescriptionLibrary();
		l.setAuthor( "H.G. Wells" );
		l.setTitle( "War of the Worlds" );
		l.setDescription( "Martians invade earth to eliminate mankind." );
		session.save( l );

		tx.commit();
	}

	private void buildBoostedFieldIndex(FullTextSession session) {
		Transaction tx = session.beginTransaction();
		BoostedFieldDescriptionLibrary l = new BoostedFieldDescriptionLibrary();
		l.setAuthor( "H.G. Wells" );
		l.setTitle( "The Invisible Man" );
		l.setDescription( "Scientist discovers invisibility and becomes insane." );
		session.save( l );

		l = new BoostedFieldDescriptionLibrary();
		l.setAuthor( "H.G. Wells" );
		l.setTitle( "War of the Worlds" );
		l.setDescription( "Martians invade earth to eliminate mankind." );
		session.save( l );

		tx.commit();
	}

	private void buildBoostedGetIndex(FullTextSession session) {
		Transaction tx = session.beginTransaction();
		BoostedGetDescriptionLibrary l = new BoostedGetDescriptionLibrary();
		l.setAuthor( "H.G. Wells" );
		l.setTitle( "The Invisible Man" );
		l.setDescription( "Scientist discovers invisibility and becomes insane." );
		session.save( l );

		l = new BoostedGetDescriptionLibrary();
		l.setAuthor( "H.G. Wells" );
		l.setTitle( "War of the Worlds" );
		l.setDescription( "Martians invade earth to eliminate mankind." );
		session.save( l );

		tx.commit();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				BoostedDescriptionLibrary.class,
				BoostedFieldDescriptionLibrary.class,
				BoostedGetDescriptionLibrary.class,
		};
	}
}

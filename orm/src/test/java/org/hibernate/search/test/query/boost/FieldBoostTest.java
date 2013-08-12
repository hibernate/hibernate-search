/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.test.query.boost;

import java.util.List;

import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * @author John Griffin
 */
public class FieldBoostTest extends SearchTestCase {

	private static final Log log = LoggerFactory.make();

	public void testBoostedGetDesc() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		buildBoostedGetIndex( fullTextSession );

		fullTextSession.clear();
		Transaction tx = fullTextSession.beginTransaction();

		QueryParser authorParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "author", TestConstants.standardAnalyzer );
		QueryParser descParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "description", TestConstants.standardAnalyzer );
		Query author = authorParser.parse( "Wells" );
		Query desc = descParser.parse( "martians" );

		BooleanQuery query = new BooleanQuery();
		query.add( author, BooleanClause.Occur.SHOULD );
		query.add( desc, BooleanClause.Occur.SHOULD );
		log.debug( query.toString() );

		org.hibernate.search.FullTextQuery hibQuery =
				fullTextSession.createFullTextQuery( query, BoostedGetDescriptionLibrary.class );
		List results = hibQuery.list();

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

	public void testBoostedFieldDesc() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		buildBoostedFieldIndex( fullTextSession );

		fullTextSession.clear();
		Transaction tx = fullTextSession.beginTransaction();

		QueryParser authorParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "author", TestConstants.standardAnalyzer );
		QueryParser descParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "description", TestConstants.standardAnalyzer );
		Query author = authorParser.parse( "Wells" );
		Query desc = descParser.parse( "martians" );

		BooleanQuery query = new BooleanQuery();
		query.add( author, BooleanClause.Occur.SHOULD );
		query.add( desc, BooleanClause.Occur.SHOULD );
		log.debug( query.toString() );

		org.hibernate.search.FullTextQuery hibQuery =
				fullTextSession.createFullTextQuery( query, BoostedFieldDescriptionLibrary.class );
		List results = hibQuery.list();

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

	public void testBoostedDesc() throws Exception {
		FullTextSession fullTextSession = Search.getFullTextSession( openSession() );
		buildBoostedDescIndex( fullTextSession );

		fullTextSession.clear();
		Transaction tx = fullTextSession.beginTransaction();

		QueryParser authorParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "author", TestConstants.standardAnalyzer );
		QueryParser descParser = new QueryParser( TestConstants.getTargetLuceneVersion(), "description", TestConstants.standardAnalyzer );
		Query author = authorParser.parse( "Wells" );
		Query desc = descParser.parse( "martians" );

		BooleanQuery query = new BooleanQuery();
		query.add( author, BooleanClause.Occur.SHOULD );
		query.add( desc, BooleanClause.Occur.SHOULD );
		log.debug( query.toString() );

		org.hibernate.search.FullTextQuery hibQuery =
				fullTextSession.createFullTextQuery( query, BoostedDescriptionLibrary.class );
		List results = hibQuery.list();

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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				BoostedDescriptionLibrary.class,
				BoostedFieldDescriptionLibrary.class,
				BoostedGetDescriptionLibrary.class,
		};
	}
}

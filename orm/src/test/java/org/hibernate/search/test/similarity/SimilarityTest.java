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
package org.hibernate.search.test.similarity;

import java.util.List;

import org.hibernate.search.SearchException;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.Environment;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/**
 * @author Emmanuel Bernard
 */
public class SimilarityTest extends SearchTestCase {

	public void testIndexLevelSimilarity() throws Exception {
		Configuration config = new Configuration();
		config.addAnnotatedClass( Can.class );
		config.addAnnotatedClass( Trash.class );
		config.setProperty( "hibernate.search.default.directory_provider", "ram" );
		config.setProperty( "hibernate.search.default.similarity", DummySimilarity.class.getName() );
		try {
			config.buildSessionFactory();
			fail( "Session creation should have failed due to duplicate similarity definition" );
		}
		catch ( SearchException e ) { // the SearchException will be wrapped in a HibernateException
			assertTrue(
					e.getMessage().startsWith(
							"Multiple similarities defined"
					)
			);
		}
	}

	public void testClassAndGlobalSimilarity() throws Exception {
		Session s = openSession(  );
		Transaction tx = s.beginTransaction();
		Trash trash = new Trash();
		trash.setName( "Green trash" );
		s.persist( trash );
		trash = new Trash();
		trash.setName( "Green Green Green trash" );
		s.persist( trash );
		Can can = new Can();
		can.setName( "Green can" );
		s.persist( can );
		can = new Can();
		can.setName( "Green Green Green can" );
		s.persist( can );
		tx.commit();

		s.clear();

		tx = s.beginTransaction();
		TermQuery tq = new TermQuery( new Term("name", "green") );
		FullTextSession fts = Search.getFullTextSession( s );
		List results = fts.createFullTextQuery( tq, Trash.class ).setProjection( FullTextQuery.SCORE, FullTextQuery.THIS ).list();
		assertEquals( 2, results.size() );
		assertEquals( "Similarity not overridden at the class level", ( (Object[]) results.get( 0 ) )[0], ( (Object[]) results.get( 1 ) )[0]);
		assertEquals( "Similarity not overridden", 1.0f, ( (Object[]) results.get( 0 ) )[0] );
		for (Object result : results) s.delete( ( (Object[]) result )[1] );

		results = fts.createFullTextQuery( tq, Can.class ).setProjection( FullTextQuery.SCORE, FullTextQuery.THIS ).list();
		assertEquals( 2, results.size() );
		assertEquals( "Similarity not overridden by the global setting", ( (Object[]) results.get( 0 ) )[0], ( (Object[]) results.get( 1 ) )[0]);
		assertFalse( "Similarity not overridden by the global setting", new Float(1.0f).equals( ( (Object[]) results.get( 0 ) )[0] ) );
		for (Object result : results) s.delete( ( (Object[]) result )[1] );

		tx.commit();
		s.close();

	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Trash.class,
				Can.class
		};
	}

	@Override
	protected void configure(Configuration cfg) {
		cfg.setProperty( Environment.SIMILARITY_CLASS, DummySimilarity2.class.getName() );
		super.configure( cfg );
	}
}

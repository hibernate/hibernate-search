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
package org.hibernate.search.test.reader.functionality;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.indexes.impl.SharingBufferReaderProvider;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.reader.Detective;
import org.hibernate.search.test.reader.Suspect;

public class FilterOnDirectoryTest extends SearchTestCase {

	public void testFilteredClasses() throws Exception {
		createDoeFamily();
		FullTextSession fts = Search.getFullTextSession( openSession() );
		Transaction tx = fts.beginTransaction();
		Query q = new TermQuery( new Term( "name", "doe" ) );

		assertEquals( 2, fts.createFullTextQuery( q ).getResultSize() );
		assertEquals( 2, fts.createFullTextQuery( q, Detective.class, Suspect.class ).getResultSize() );

		FullTextQuery detectiveQuery = fts.createFullTextQuery( q, Detective.class );
		assertEquals( 1, detectiveQuery.getResultSize() );
		assertTrue( detectiveQuery.list().get( 0 ) instanceof Detective );

		FullTextQuery suspectQuery = fts.createFullTextQuery( q, Suspect.class );
		assertEquals( 1, suspectQuery.getResultSize() );
		assertTrue( suspectQuery.list().get( 0 ) instanceof Suspect );

		assertEquals( 2, fts.createFullTextQuery( q ).getResultSize() );
		assertEquals( 2, fts.createFullTextQuery( q, Detective.class, Suspect.class ).getResultSize() );

		tx.commit();
		fts.close();
	}

	private void createDoeFamily() {
		Session s = openSession( );
		Transaction tx = s.beginTransaction();
		Detective detective = new Detective();
		detective.setName( "John Doe" );
		s.persist( detective );
		Suspect suspect = new Suspect();
		suspect.setName( "Jane Doe" );
		s.persist( suspect );
		tx.commit();
		s.close();
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, StandardAnalyzer.class.getName() );
		cfg.setProperty( "hibernate.search.default." + Environment.READER_STRATEGY, SharingBufferReaderProvider.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Detective.class,
				Suspect.class
		};
	}

}

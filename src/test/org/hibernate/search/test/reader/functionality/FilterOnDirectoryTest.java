// $Id$
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
import org.hibernate.search.reader.SharingBufferReaderProvider;
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
		assertTrue( detectiveQuery.list().get(0) instanceof Detective );
		
		FullTextQuery suspectQuery = fts.createFullTextQuery( q, Suspect.class );
		assertEquals( 1, suspectQuery.getResultSize() );
		assertTrue( suspectQuery.list().get(0) instanceof Suspect );
		
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

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, StandardAnalyzer.class.getName() );
		cfg.setProperty( Environment.READER_STRATEGY,  SharingBufferReaderProvider.class.getName() );
	}
	
	protected Class[] getMappings() {
		return new Class[] {
				Detective.class,
				Suspect.class
		};
	}

}

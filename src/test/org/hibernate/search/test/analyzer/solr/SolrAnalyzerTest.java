package org.hibernate.search.test.analyzer.solr;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.Transaction;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.index.Term;

/**
 * @author Emmanuel Bernard
 */
public class SolrAnalyzerTest extends SearchTestCase {
	public void testAnalyzerDef() throws Exception {
		Team team = new Team();
		System.out.println("ˆ = \u00E0");
		team.setDescription( "This is a D\u00E0scription" );
		team.setLocation( "Atlanta" );
		team.setName( "ATL team" );
		FullTextSession fts = Search.createFullTextSession( openSession() );
		Transaction tx = fts.beginTransaction();
		fts.persist( team );
		tx.commit();
		fts.clear();
		tx = fts.beginTransaction();
		TermQuery query = new TermQuery( new Term("description", "D\u00E0scription") );
		assertEquals( "iso latin filter should work", 0, fts.createFullTextQuery( query ).list().size() );
		query = new TermQuery( new Term("description", "is") );
		assertEquals( "stop word filter should work", 0, fts.createFullTextQuery( query ).list().size() );
		query = new TermQuery( new Term("description", "dascription") );
		assertEquals( 1, fts.createFullTextQuery( query ).list().size() );
		fts.delete( fts.createFullTextQuery( query ).list().get( 0 ) );
		tx.commit();
		fts.close();
	}

	protected Class[] getMappings() {
		return new Class[] {
				Team.class
		};
	}
}

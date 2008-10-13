// $Id$
package org.hibernate.search.test.analyzer.solr;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.test.SearchTestCase;

/**
 * Tests the Solr analyzer creation framework.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class SolrAnalyzerTest extends SearchTestCase {

	/**
	 * Tests that the token filters applied to <code>Team</code> are successfully created and used. Refer to
	 * <code>Team</code> to see the exat definitions.
	 *
	 * @throws Exception in case the test fails
	 */
	public void testAnalyzerDef() throws Exception {
		// create the test instance
		Team team = new Team();
		team.setDescription( "This is a D\u00E0scription" );  // \u00E0 == ˆ - ISOLatin1AccentFilterFactory should strip of diacritic 
		team.setLocation( "Atlanta" );
		team.setName( "ATL team" );

		// persist and index the test object
		FullTextSession fts = Search.getFullTextSession( openSession() );
		Transaction tx = fts.beginTransaction();
		fts.persist( team );
		tx.commit();
		fts.clear();

		// execute several search to show that the right tokenizers were applies
		tx = fts.beginTransaction();
		TermQuery query = new TermQuery( new Term( "description", "D\u00E0scription" ) );
		assertEquals(
				"iso latin filter should work.  ˆ should be a now", 0, fts.createFullTextQuery( query ).list().size()
		);

		query = new TermQuery( new Term( "description", "is" ) );
		assertEquals(
				"stop word filter should work. is should be removed", 0, fts.createFullTextQuery( query ).list().size()
		);

		query = new TermQuery( new Term( "description", "dascript" ) );
		assertEquals(
				"snowball stemmer should work. 'dascription' should be stemmed to 'dascript'",
				1,
				fts.createFullTextQuery( query ).list().size()
		);

		// cleanup
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

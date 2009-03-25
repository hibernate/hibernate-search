// $Id$
package org.hibernate.search.test.session;

import java.sql.Statement;
import java.util.List;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.Environment;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.impl.FullTextSessionImpl;
import org.hibernate.Transaction;
import org.hibernate.ScrollableResults;
import org.hibernate.ScrollMode;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.analysis.StopAnalyzer;

/**
 * @author Emmanuel Bernard
 */
public class MassIndexUsingManualFlushTest extends SearchTestCase {
	public void testManualIndexFlush() throws Exception {
		FullTextSession s = Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();
		int loop = 14;
		for (int i = 0; i < loop; i++) {
			Statement statmt = s.connection().createStatement();
			statmt.executeUpdate( "insert into Domain(id, name) values( + "
					+ ( i + 1 ) + ", 'sponge" + i + "')" );
			statmt.executeUpdate( "insert into Email(id, title, body, header, domain_id) values( + "
					+ ( i + 1 ) + ", 'Bob Sponge', 'Meet the guys who create the software', 'nope', " + ( i + 1 ) +")" );
			statmt.close();
		}
		tx.commit();
		s.close();

		//check non created object does get found!!1
		s = new FullTextSessionImpl( openSession() );
		tx = s.beginTransaction();
		ScrollableResults results = s.createCriteria( Email.class ).scroll( ScrollMode.FORWARD_ONLY );
		int index = 0;
		while ( results.next() ) {
			index++;
			final Email o = (Email) results.get( 0 );
			s.index( o );
			if ( index % 5 == 0 ) {
				s.flushToIndexes();
				s.clear();
			}
		}
		tx.commit();
		s.clear();
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( "id", new StopAnalyzer() );
		List result = s.createFullTextQuery( parser.parse( "body:create" ) ).list();
		assertEquals( 14, result.size() );
		for (Object object : result) {
			s.delete( object );
		}
		tx.commit();
		s.close();
	}

	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	protected Class[] getMappings() {
		return new Class[] {
				Email.class,
				Domain.class
		};
	}
}

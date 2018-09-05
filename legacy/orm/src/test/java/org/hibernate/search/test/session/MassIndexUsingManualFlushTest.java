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
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Transaction;
import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.cfg.Environment;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.TestConstants;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Emmanuel Bernard
 */
public class MassIndexUsingManualFlushTest extends SearchTestBase {
	@Test
	public void testManualIndexFlush() throws Exception {
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
		QueryParser parser = new QueryParser( "id", TestConstants.stopAnalyzer );
		List result = s.createFullTextQuery( parser.parse( "body:create" ) ).list();
		assertEquals( 14, result.size() );
		for ( Object object : result ) {
			s.delete( object );
		}
		tx.commit();
		s.close();
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Email.class,
				Domain.class
		};
	}
}

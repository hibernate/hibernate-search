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
package org.hibernate.search.test.session;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.hibernate.jdbc.Work;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.Environment;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;
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
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "id", TestConstants.stopAnalyzer );
		List result = s.createFullTextQuery( parser.parse( "body:create" ) ).list();
		assertEquals( 14, result.size() );
		for ( Object object : result ) {
			s.delete( object );
		}
		tx.commit();
		s.close();
	}

	@Override
	protected void configure(org.hibernate.cfg.Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ANALYZER_CLASS, StopAnalyzer.class.getName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {
				Email.class,
				Domain.class
		};
	}
}

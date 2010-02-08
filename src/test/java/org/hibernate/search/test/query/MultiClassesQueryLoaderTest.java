/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.test.query;

import java.sql.Statement;
import java.util.List;

import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.FullTextQuery;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;

/**
 * @author Emmanuel Bernard
 */
public class MultiClassesQueryLoaderTest extends SearchTestCase {

	public void testObjectNotFound() throws Exception {
		Session sess = openSession();
		Transaction tx = sess.beginTransaction();
		Author author = new Author();
		author.setName( "Moo Cow" );
		sess.persist( author );

		tx.commit();
		sess.clear();
		Statement statement = sess.connection().createStatement();
		statement.executeUpdate( "DELETE FROM Author" );
		statement.close();
		FullTextSession s = Search.getFullTextSession( sess );
		tx = s.beginTransaction();
		QueryParser parser = new QueryParser( getTargetLuceneVersion(), "title", SearchTestCase.keywordAnalyzer );
		Query query = parser.parse( "name:moo" );
		FullTextQuery hibQuery = s.createFullTextQuery( query, Author.class, Music.class );
		List result = hibQuery.list();
		assertEquals( "Should have returned no author", 0, result.size() );

		for (Object o : s.createCriteria( Object.class ).list()) {
			s.delete( o );
		}

		tx.commit();
		s.close();
	}

	protected Class<?>[] getMappings() {
		return new Class[] {
				Author.class,
				Music.class
		};
	}
}

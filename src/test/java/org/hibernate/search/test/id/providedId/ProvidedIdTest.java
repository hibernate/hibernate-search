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
package org.hibernate.search.test.id.providedId;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.test.SearchTestCase;

/**
 * @author Navin Surtani (<a href="mailto:nsurtani@redhat.com">nsurtani@redhat.com</a>)
 */
public class ProvidedIdTest extends SearchTestCase {

	protected Class[] getMappings() {
		return new Class[] {
				ProvidedIdPerson.class,
				ProvidedIdPersonSub.class
		};
	}

	public void testProvidedId() throws Exception {

		ProvidedIdPerson person1 = new ProvidedIdPerson();
		person1.setName( "Big Goat" );
		person1.setBlurb( "Eats grass" );

		ProvidedIdPerson person2 = new ProvidedIdPerson();
		person2.setName( "Mini Goat" );
		person2.setBlurb( "Eats cheese" );

		ProvidedIdPersonSub person3 = new ProvidedIdPersonSub();
		person3.setName( "Regular goat" );
		person3.setBlurb( "Is anorexic" );

		Session session = openSession();
		FullTextSession fullTextSession = Search.getFullTextSession( session );
		Transaction transaction = session.beginTransaction();
		session.persist( person1 );
		session.persist( person2 );
		session.persist( person3 );

		transaction.commit();
		session.clear();

		transaction = fullTextSession.beginTransaction();

		QueryParser parser = new QueryParser( "name", new StandardAnalyzer() );
		Query luceneQuery = parser.parse( "Goat" );

		//we cannot use FTQuery because @ProvidedId does not provide the getter id and Hibernate Hsearch Query extension
		//needs it. So we use plain Lucene 

		//we know there is only one DP
		DirectoryProvider provider = fullTextSession.getSearchFactory()
				.getDirectoryProviders( ProvidedIdPerson.class )[0];
		IndexSearcher searcher = new IndexSearcher( provider.getDirectory() );
		TopDocs hits = searcher.search( luceneQuery, 1000 );
		searcher.close();
		transaction.commit();
		session.close();

		assertEquals( 3, hits.totalHits );
	}


}

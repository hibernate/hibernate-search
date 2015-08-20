/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.id;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.search.Search;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestBase;
import org.hibernate.search.testsupport.backend.LeakingLocalBackend;
import org.junit.Test;

/**
 * Related to HSEARCH-1050: check we deal nicely with weird DocumentId
 * configurations.
 *
 * @author Sanne Grinovero (C) 2012 Red Hat Inc.
 */
public class EmbeddedIdWithDocumentIdTest extends SearchTestBase {

	@Test
	public void testFieldBridge() throws Exception {
		LeakingLocalBackend.reset();

		PersonPK johnDoePk = new PersonPK();
		johnDoePk.setFirstName( "John" );
		johnDoePk.setLastName( "Doe" );
		PersonCustomDocumentId johnDoe = new PersonCustomDocumentId();
		johnDoe.setFavoriteColor( "Blue" );
		johnDoe.setPersonNames( johnDoePk );
		johnDoe.setSecurityNumber( "AB123" );

		Session s = openSession();
		Transaction tx = s.beginTransaction();
		s.save( johnDoe );
		tx.commit();
		s.clear();

		List<LuceneWork> lastProcessedQueue = LeakingLocalBackend.getLastProcessedQueue();
		assertEquals( 1, lastProcessedQueue.size() );
		LuceneWork luceneWork = lastProcessedQueue.get( 0 );
		assertEquals( "AB123", luceneWork.getIdInString() );

		tx = s.beginTransaction();

		QueryBuilder queryBuilder = getSearchFactory().buildQueryBuilder().forEntity( PersonCustomDocumentId.class ).get();
		Query query = queryBuilder.keyword().onField( "id" ).ignoreAnalyzer().matching( "AB123" ).createQuery();

		List results = Search.getFullTextSession( s ).createFullTextQuery( query, PersonCustomDocumentId.class ).list();
		assertEquals( 1, results.size() );
		johnDoe = (PersonCustomDocumentId) results.get( 0 );
		johnDoe.setFavoriteColor( "Red" );
		tx.commit();
		s.clear();

		tx = s.beginTransaction();
		results = Search.getFullTextSession( s ).createFullTextQuery( query, PersonCustomDocumentId.class ).list();
		assertEquals( 1, results.size() );
		johnDoe = (PersonCustomDocumentId) results.get( 0 );
		assertEquals( "Red", johnDoe.getFavoriteColor() );
		s.delete( results.get( 0 ) );
		tx.commit();
		s.close();
	}

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ PersonCustomDocumentId.class };
	}

	@Override
	public void configure(Map<String,Object> cfg) {
		cfg.put( "hibernate.search.default.worker.backend", LeakingLocalBackend.class.getName() );
	}

}

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
package org.hibernate.search.test.engine.worker.duplication;

import java.util.List;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import org.hibernate.Transaction;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.backend.impl.WorkQueue;
import org.hibernate.search.backend.spi.WorkType;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.TestConstants;

/**
 * Testcase for HSEARCH-257.
 */
public class WorkDuplicationTest extends SearchTestCase {

	/**
	 * This test assures that HSEARCH-257. Before the fix Search would issue another <code>AddLuceneWork</code> after
	 * the <code>DeleteLuceneWork</code>. This lead to the fact that after the deletion there was still a Lucene document
	 * in the index.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testNoWorkDuplication() throws Exception {

		FullTextSession s = org.hibernate.search.Search.getFullTextSession( openSession() );
		Transaction tx = s.beginTransaction();

		// create new customer
		SpecialPerson person = new SpecialPerson();
		person.setName( "Joe Smith" );

		EmailAddress emailAddress = new EmailAddress();
		emailAddress.setAddress( "foo@foobar.com" );
		emailAddress.setDefaultAddress( true );

		person.addEmailAddress( emailAddress );

		// persist the customer
		s.persist( person );
		tx.commit();

		// search if the record made it into the index
		tx = s.beginTransaction();
		String searchQuery = "Joe";
		QueryParser parser = new QueryParser( TestConstants.getTargetLuceneVersion(), "Content", TestConstants.standardAnalyzer );
		Query luceneQuery = parser.parse( searchQuery );
		FullTextQuery query = s.createFullTextQuery( luceneQuery );
		List results = query.list();
		assertTrue( "We should have a hit", results.size() == 1 );
		tx.commit();

		// Now try to delete
		tx = s.beginTransaction();
		int id = person.getId();
		person = (SpecialPerson) s.get( SpecialPerson.class, id );
		s.delete( person );
		tx.commit();

		// Search and the record via Lucene directly
		tx = s.beginTransaction();

		SearchFactory searchFactory = s.getSearchFactory();
		IndexReader indexReader = searchFactory.getIndexReaderAccessor().open( SpecialPerson.class );
		try {
			IndexSearcher searcher = new IndexSearcher( indexReader );
			// we have to test using Lucene directly since query loaders will ignore hits for which there is no
			// database entry
			TopDocs topDocs = searcher.search( luceneQuery, null, 1 );
			assertTrue( "We should have no hit", topDocs.totalHits == 0 );
		}
		finally {
			searchFactory.getIndexReaderAccessor().close( indexReader );
		}
		tx.commit();
		s.close();
	}

	/**
	 * Tests that adding and deleting the same entity only results into a single delete in the work queue.
	 * See HSEARCH-293.
	 *
	 * @throws Exception in case the test fails.
	 */
	public void testAddWorkGetReplacedByDeleteWork() throws Exception {
		FullTextSession fullTextSession = org.hibernate.search.Search.getFullTextSession( openSession() );
		SearchFactoryImplementor searchFactory = (SearchFactoryImplementor) fullTextSession.getSearchFactory();

		// create test entity
		SpecialPerson person = new SpecialPerson();
		person.setName( "Joe Smith" );

		EmailAddress emailAddress = new EmailAddress();
		emailAddress.setAddress( "foo@foobar.com" );
		emailAddress.setDefaultAddress( true );

		person.addEmailAddress( emailAddress );

		WorkQueue plannerEngine = new WorkQueue( searchFactory );

		plannerEngine.add( new Work<SpecialPerson>( person, 1, WorkType.ADD ) );

		plannerEngine.prepareWorkPlan();
		List<LuceneWork> sealedQueue = plannerEngine.getSealedQueue();

		assertEquals( "There should only be one job in the queue", 1, sealedQueue.size() );
		assertTrue( "Wrong job type", sealedQueue.get( 0 ) instanceof AddLuceneWork );

		plannerEngine.add( new Work<SpecialPerson>( person, 1, WorkType.DELETE ) );
		plannerEngine.prepareWorkPlan();
		sealedQueue = plannerEngine.getSealedQueue();

		assertEquals( "Jobs should have countered each other", 0, sealedQueue.size() );

		fullTextSession.close();
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Person.class, EmailAddress.class, SpecialPerson.class };
	}
}

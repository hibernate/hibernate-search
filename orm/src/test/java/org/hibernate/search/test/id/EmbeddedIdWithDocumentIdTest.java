/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2012 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.test.id;

import java.util.List;

import org.apache.lucene.search.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.search.Search;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.LeakingLuceneBackend;

/**
 * Related to HSEARCH-1050: check we deal nicely with weird DocumentId
 * configurations.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2012 Red Hat Inc.
 */
public class EmbeddedIdWithDocumentIdTest extends SearchTestCase {

	public void testFieldBridge() throws Exception {
		LeakingLuceneBackend.reset();

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

		List<LuceneWork> lastProcessedQueue = LeakingLuceneBackend.getLastProcessedQueue();
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
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{ PersonCustomDocumentId.class };
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( "hibernate.search.default.worker.backend", LeakingLuceneBackend.class.getName() );
	}

}

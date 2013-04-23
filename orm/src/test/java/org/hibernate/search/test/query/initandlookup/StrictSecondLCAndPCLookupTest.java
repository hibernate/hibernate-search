/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
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
package org.hibernate.search.test.query.initandlookup;

import java.util.List;

import org.apache.lucene.search.MatchAllDocsQuery;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.search.FullTextQuery;
import org.hibernate.search.FullTextSession;
import org.hibernate.search.Search;
import org.hibernate.search.query.DatabaseRetrievalMethod;
import org.hibernate.search.query.ObjectLookupMethod;
import org.hibernate.search.test.SearchTestCase;
import org.hibernate.search.test.util.GatedLuceneBackend;
import org.hibernate.stat.Statistics;
import org.hibernate.testing.cache.CachingRegionFactory;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test second level cache and persistence context lookup methods
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class StrictSecondLCAndPCLookupTest extends SearchTestCase {

	public void testStaleCacheWithAsyncIndexer() {
		Session session = openSession();
		final Statistics statistics = session.getSessionFactory().getStatistics();
		statistics.clear();
		statistics.setStatisticsEnabled( true );
		setData( session, statistics );

		GatedLuceneBackend.open.set( false ); // disable processing of index updates
		Transaction tx = session.beginTransaction();
		List list = session.createCriteria( StrictKernel.class ).list();
		assertThat( list ).hasSize( 2 );
		session.delete( list.get( 0 ) );
		tx.commit();
		session.clear();
		GatedLuceneBackend.open.set( true );

		FullTextSession fullTextSession = Search.getFullTextSession( session );
		FullTextQuery allKernelsQuery = fullTextSession.createFullTextQuery( new MatchAllDocsQuery() )
				.initializeObjectsWith( ObjectLookupMethod.SECOND_LEVEL_CACHE, DatabaseRetrievalMethod.QUERY );

		//Identify the mismatch between index/database:
		assertThat( allKernelsQuery.getResultSize() ).isEqualTo( 2 );
		assertThat( allKernelsQuery.list() ).hasSize( 1 );
	}

	private void setData(Session session, Statistics statistics) {
		Transaction transaction = session.beginTransaction();
		StrictKernel k = new StrictKernel();
		k.setCodeName( "coconut" );
		k.setProduct( "Polgeiser" );
		session.persist( k );
		StrictKernel k2 = new StrictKernel();
		k2.setCodeName( "ballpark" );
		k2.setProduct( "Polgeiser" );
		session.persist( k2 );
		transaction.commit();

		session.clear();

		transaction = session.beginTransaction();
		session.get( StrictKernel.class, k.getId() );
		session.get( StrictKernel.class, k2.getId() );
		transaction.commit();

		assertThat( statistics.getSecondLevelCachePutCount() )
				.isEqualTo( 2 );
	}

	@Override
	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "true" );
		cfg.setProperty( "hibernate.search.default.worker.backend", org.hibernate.search.test.util.GatedLuceneBackend.class.getName() );
		cfg.setProperty( Environment.CACHE_REGION_FACTORY, CachingRegionFactory.class.getCanonicalName() );
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { StrictKernel.class };
	}
}
